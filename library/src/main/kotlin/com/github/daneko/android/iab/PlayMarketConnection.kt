package com.github.daneko.android.iab

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.support.annotation.VisibleForTesting
import com.android.vending.billing.IInAppBillingService
import com.github.daneko.android.iab.exception.IabException
import com.github.daneko.android.iab.model.GooglePlayResponse
import com.github.daneko.android.iab.model.IabItemType
import fj.F
import fj.data.Option
import org.slf4j.LoggerFactory
import rx.Observable
import rx.subjects.BehaviorSubject

/**
 *
 */
@VisibleForTesting
internal open class PlayMarketConnection(val context: Context) : ServiceConnection {

    companion object {
        private val log = LoggerFactory.getLogger(PlayMarketConnection::class.java)
        private val intentAction = "com.android.vending.billing.InAppBillingService.BIND"
        private val marketPackage = "com.android.vending"

        private fun create(context: Context): Option<PlayMarketConnection> {
            val connection = PlayMarketConnection(context)
            return Option.iif(connection.connectionOpen(Context.BIND_AUTO_CREATE), connection)
        }

        fun connect(context: Context, packageName: String): Observable<PlayMarketConnection> =
                fromValidation(PlayMarketConnection.create(context).toValidation(IabException("connect fail"))).
                        flatMap { connection ->
                            connection.checkBillingSupportWithError(packageName).
                                    all { it.isSuccess }.map { connection }.
                                    doOnUnsubscribe { log.trace("unsubscribe connection observable") }.
                                    doOnUnsubscribe { connection.connectionClose() }
                        }
    }

    private val subject: BehaviorSubject<IInAppBillingService> = BehaviorSubject.create()

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        log.trace("onServiceConnected name: $name, service: $service")
        subject.onNext(IInAppBillingService.Stub.asInterface(service))
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        log.trace("onServiceDisconnected name: $name")
        subject.onCompleted()
    }

    private fun connectionOpen(/* @BindServiceFlags */ flags: Int): Boolean {
        log.trace("do Bind")
        val intent = Intent(intentAction)
        intent.setPackage(marketPackage)

        return Option.iif(context.packageManager.queryIntentServices(intent, 0).isNotEmpty(), flags).
                map(F<Int, Boolean> { context.bindService(intent, this@PlayMarketConnection, it) }).orSome(false)
    }

    private fun connectionClose() {
        log.trace("do close")
        context.unbindService(this)
    }

    open fun <T> action(f: (IInAppBillingService) -> T): Observable<T> {
        return subject.asObservable().take(1).map(f)
    }

    /**
     * wrap {@link IInAppBillingService#isBillingSupported }
     */
    private fun checkBillingSupport(packageName: String): Observable<GooglePlayResponse> {
        log.trace("#checkBillingSupport: target package name : $packageName")
        return Observable.from(IabItemType.values().map { it.typeName }).
                flatMap { typeName ->
                    action {
                        GooglePlayResponse.create(
                                it.isBillingSupported(IabConstant.TARGET_VERSION, packageName, typeName))
                    }
                }
    }

    /**
     * if response is not {@link GooglePlayResponse#OK}, return Observable#onError
     */
    fun checkBillingSupportWithError(packageName: String): Observable<GooglePlayResponse> =
            checkBillingSupport(packageName).flatMap { checkGooglePlayResponse(it) }
}
