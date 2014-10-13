package jp.daneko.android.inappv3;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.LruCache;

import com.android.vending.billing.IInAppBillingService;

import javax.annotation.Nonnull;

import fj.F;
import fj.F1Functions;
import fj.P1;
import fj.Unit;
import fj.data.Option;

import rx.Observable;
import rx.subjects.ReplaySubject;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import jp.daneko.android.inappv3.exception.IabException;
import jp.daneko.android.inappv3.model.IabItemType;

import static fj.data.Option.none;

/**
 */
@Slf4j
class IabServiceConnection implements ServiceConnection {

    private static LruCache<Activity, IabServiceConnection> connectionCache =
            new LruCache<Activity, IabServiceConnection>(1024);

    private static final String IAB_BIND = "com.android.vending.billing.InAppBillingService.BIND";
    private static final String IAB_PACKAGE = "com.android.vending";

    @Setter(value = AccessLevel.PRIVATE)
    private String packageName;

    public static IabServiceConnection getConnection(@Nonnull final Activity activity) {
        final F<Unit, P1<IabServiceConnection>> connectionFactory = F1Functions.lazy(u -> {
            IabServiceConnection connection = new IabServiceConnection();
            Intent serviceIntent = new Intent(IAB_BIND);
            serviceIntent.setPackage(IAB_PACKAGE);
            if (activity.getPackageManager().queryIntentServices(serviceIntent, 0).isEmpty()) {
                throw new RuntimeException("InAppBillingService has gone ???");
            }

            log.trace("bind from {}", activity);
            activity.bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE);
            connection.setPackageName(activity.getPackageName());
            connectionCache.put(activity, connection);
            return connection;
        });

        log.trace("call get Connection");

        return Option.fromNull(connectionCache.get(activity))
                .orSome(connectionFactory.f(Unit.unit()));
    }

    private final ReplaySubject<IInAppBillingService> serviceSubject;
    @Getter
    private final Observable<IInAppBillingService> serviceObservable;
    private Option<IInAppBillingService> billingService;

    private IabServiceConnection() {
        billingService = none();
        serviceSubject = ReplaySubject.<IInAppBillingService>create();
        serviceObservable = serviceSubject.asObservable();
    }

    /**
     * RemoteExceptionとか言われたら、{@link com.android.vending.billing.IInAppBillingService} に書いてあることが信用できなくなるので無視
     * v3 inapp & 月額課金対応かはチェックする
     */
    @SneakyThrows(RemoteException.class)
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        log.trace("service connected.");

        final IInAppBillingService s = IInAppBillingService.Stub.asInterface(service);

        final int inappSupported = s.isBillingSupported(IabConstant.TARGET_VERSION, packageName, IabItemType.INAPP.getTypeName());
        final int subsSupported = s.isBillingSupported(IabConstant.TARGET_VERSION, packageName, IabItemType.SUBSCRIPTION.getTypeName());
        if (inappSupported != IabConstant.GooglePlayResponse.OK.getCode()
                || subsSupported != IabConstant.GooglePlayResponse.OK.getCode()
                ) {
            final String err = String.format("unsupport v3 api. inapp response code:%d / subscription response code:%d", inappSupported, subsSupported);
            log.error(err);
            serviceSubject.onError(new IabException(err));
            return;
        }
        billingService = Option.some(s);
        serviceSubject.onNext(s);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        log.trace("service disconnected.");
        serviceSubject.onCompleted();
        billingService = none();
    }

    public boolean isConnected() {
        log.trace("check connection");
        return billingService.isSome();
    }

    public static Unit dispose(@Nonnull final Activity activity) {
        Option.fromNull(connectionCache.remove(activity)).map(c -> {
            log.trace("unbind from {}", activity);
            activity.unbindService(c);
            return Unit.unit();
        });
        return Unit.unit();
    }
}
