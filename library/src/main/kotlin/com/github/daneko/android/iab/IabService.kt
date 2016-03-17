package com.github.daneko.android.iab

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.annotation.VisibleForTesting
import com.example.android.trivialdrivesample.util.Security
import com.github.daneko.android.iab.exception.IabException
import com.github.daneko.android.iab.exception.IabResponseException
import com.github.daneko.android.iab.model.*
import fj.data.Option
import fj.function.Effect1
import fj.function.TryEffect2
import org.json.JSONObject
import org.slf4j.LoggerFactory
import rx.Observable
import java.util.*

/**
 *
 */
object IabService {
    private val log = LoggerFactory.getLogger(IabService::class.java)

    @JvmStatic
    fun defaultVerificationLogic(base64PublicKey: String): TryEffect2<String, String, Exception> =
            TryEffect2 { purchaseData, signature ->
                if (!Security.verifyPurchase(base64PublicKey, purchaseData, signature)) {
                    throw IabException("purchase verification error: $purchaseData / $signature")
                }
            }

    @JvmStatic
    @JvmOverloads
    fun buyProduct(
            iabContext: IabContext,
            buyItem: Product,
            requestCode: Int,
            packageName: String = iabContext.activity.packageName,
            verificationLogic: TryEffect2<String, String, Exception>): Observable<Product> {

        return if (!buyItem.canBuy()) {
            Observable.error<Product>(IllegalArgumentException("item was purchased."))
        } else {
            PlayMarketConnection.connect(iabContext.activity, packageName).flatMap {
                _buyProduct(it, iabContext, buyItem, requestCode, packageName, verificationLogic)
            }
        }
    }

    @VisibleForTesting
    internal fun _buyProduct(
            connection: PlayMarketConnection,
            iabContext: IabContext,
            buyItem: Product,
            requestCode: Int,
            packageName: String,
            verificationLogic: TryEffect2<String, String, Exception>): Observable<Product> {

        return buyResponseFlow(iabContext.activityResultObservable, buyItem, requestCode, verificationLogic).
                mergeWith(buyRequest(connection, buyItem, requestCode, packageName).
                        map { it.f(iabContext.activity) }.
                        map { unit -> buyItem }).
                filter { it.purchaseInfo.isSome }
    }

    @VisibleForTesting
    internal fun buyRequest(marketConnection: PlayMarketConnection,
                            buyItem: Product,
                            requestCode: Int,
                            packageName: String): Observable<Effect1<Activity>> {

        return marketConnection.action {
            it.getBuyIntent(IabConstant.TARGET_VERSION,
                    packageName,
                    buyItem.productId,
                    buyItem.IabItemType.typeName,
                    buyItem.developerPayload.toNull()
            )
        }.
                flatMap { checkGooglePlayResponse(it) }.
                map { it.getParcelable<PendingIntent>(IabConstant.BillingServiceConstants.BUY_INTENT.value) }.
                map { pendingIntent ->
                    Effect1<Activity> {
                        it.startIntentSenderForResult(
                                pendingIntent.intentSender,
                                requestCode,
                                Intent(),
                                0, 0, 0)
                    }
                }
    }

    /**
     * 購入処理結果Responseをどうにかする部分
     */
    private fun buyResponseFlow(
            activityResultObservable: Observable<ActivityResults>,
            buyItem: Product,
            requestCode: Int,
            verificationLogic: TryEffect2<String, String, Exception>): Observable<Product> {

        return activityResultObservable.filter { it.requestCode == requestCode && it.data.isSome }.
                first().
                doOnNext { log.trace("buy request response intent: $it") }.
                groupBy { it.resultCode == Activity.RESULT_OK }.
                doOnNext { log.trace("groupKey: ${it.key}") }.
                flatMap { g ->
                    if (g.key) {
                        g.flatMapIterable { it.data }.map {
                            // Pair<PurchaseData, Signature>
                            Pair(it.getStringExtra(IabConstant.BillingServiceConstants.INAPP_PURCHASE_DATA.value),
                                    it.getStringExtra(IabConstant.BillingServiceConstants.INAPP_SIGNATURE.value))
                        }.flatMap {
                            fromValidation(Purchase.create(verificationLogic)(it.first)(it.second)).
                                    map { buyItem.copy(purchaseInfo = Option.some(it)) }
                        }
                    } else {
                        g.flatMapIterable { it.data }.map { IabConstant.extractResponse(it.extras) }.flatMap {
                            val message = """purchase response (onActivityResult) is error. code : ${it.code}, desc : ${it.description}"""
                            Observable.error<Product>(IabResponseException(it, message))
                        }
                    }
                }
    }

    @JvmOverloads
    @JvmStatic
    fun consumeProduct(
            context: Context,
            packageName: String = context.packageName,
            product: Product): Observable<fj.Unit> =
            if (!product.canConsume()) {
                Observable.error<fj.Unit>(IllegalArgumentException("item can not consume: $product"))
            } else {
                PlayMarketConnection.connect(context, packageName).flatMap { connection ->
                    connection.action {
                        it.consumePurchase(IabConstant.TARGET_VERSION, packageName, product.purchaseInfo.some().token)
                    }.flatMap { checkGooglePlayResponse(GooglePlayResponse.create(it)) }.map { unit -> fj.Unit.unit() }
                }
            }

    /**
     * all product info
     */
    @JvmOverloads
    @JvmStatic
    fun fetchProduct(
            context: Context,
            verificationLogic: TryEffect2<String, String, Exception>,
            packageName: String = context.packageName,
            productIdList: List<ProductBaseInfo>): Observable<Product> {

        return PlayMarketConnection.connect(context, packageName).flatMap { connection ->
            Observable.from(IabItemType.values()).
                    flatMap { iabType ->
                        Observable.combineLatest(
                                fetchPurchase(connection,
                                        packageName,
                                        iabType,
                                        verificationLogic
                                ).toList(),
                                fetchProduct(connection, packageName, productIdList, iabType),
                                { purchases, product ->
                                    log.trace("purchases: $purchases")
                                    log.trace("product: $product")
                                    purchases.find { it.getProductId().equals(product.productId) }.
                                            let { product.copy(purchaseInfo = Option.fromNull(it)) }.
                                            apply { log.trace("returned product: $this") }
                                })
                    }
        }
    }

    private fun fetchProduct(marketConnection: PlayMarketConnection,
                             packageName: String,
                             productIdList: List<ProductBaseInfo>,
                             iabType: IabItemType): Observable<Product> {

        val productIdRawList: ArrayList<String> = ArrayList(productIdList.
                filter { it.type.iabItemType == iabType }.
                map { it.id })

        if (productIdRawList.isEmpty()) {
            return Observable.empty()
        }

        val querySkus = Bundle()
        querySkus.putStringArrayList(IabConstant.GetSkuDetailKey.ITEM_LIST.value, productIdRawList)
        log.trace("querySkus Bundle: $querySkus")

        return marketConnection.action {
            it.getSkuDetails(
                    IabConstant.TARGET_VERSION,
                    packageName,
                    iabType.typeName,
                    querySkus)
        }.
                flatMap { checkGooglePlayResponse(it) }.
                flatMap { responseBundle ->
                    if (!responseBundle.containsKey(
                            IabConstant.BillingServiceConstants.GET_SKU_DETAILS_LIST.value)) {
                        val message = "getSkuDetails, No detail list";
                        Observable.error<Product>(IabException(message))
                    } else {
                        Observable.from(responseBundle.getStringArrayList(
                                IabConstant.BillingServiceConstants.GET_SKU_DETAILS_LIST.value).
                                map { parseSkuDetailJson(it, productIdList) })
                    }
                }
    }

    private fun fetchPurchase(
            marketConnection: PlayMarketConnection,
            packageName: String,
            itemType: IabItemType,
            verificationLogic: TryEffect2<String, String, Exception>,
            continuationToken: Option<String> = Option.none(),
            acc: Observable<Purchase> = Observable.empty()): Observable<Purchase> {

        val purchaseBundleObservable: Observable<Bundle> = marketConnection.action {
            it.getPurchases(
                    IabConstant.TARGET_VERSION,
                    packageName,
                    itemType.typeName,
                    continuationToken.toNull()
            )
        }.flatMap { checkGooglePlayResponse(it) }.cache()

        val purchaseObservable: Observable<Purchase> = purchaseBundleObservable.
                flatMapIterable {
                    // make Triple<OwnedSku, PurchaseData, Signature>
                    it.getStringArrayList(IabConstant.BillingServiceConstants.INAPP_ITEM_LIST.value).
                            zip(it.getStringArrayList(
                                    IabConstant.BillingServiceConstants.INAPP_PURCHASE_DATA_LIST.value)).
                            zip(it.getStringArrayList(
                                    IabConstant.BillingServiceConstants.INAPP_SIGNATURE_LIST.value),
                                    { pair, sign -> Triple(pair.first, pair.second, sign) })
                }.
                doOnNext { log.trace("purchase data: $it") }.
                flatMap { triple ->
                    fromValidation(Purchase.create(verificationLogic)(triple.second)(triple.third))
                }

        return purchaseBundleObservable.
                map { it.getString(IabConstant.BillingServiceConstants.INAPP_CONTINUATION_TOKEN.value) }.
                flatMap {
                    if (it.isNullOrEmpty()) {
                        Observable.concat(acc, purchaseObservable)
                    } else {
                        fetchPurchase(marketConnection,
                                packageName,
                                itemType,
                                verificationLogic,
                                Option.fromNull(it),
                                Observable.concat(acc, purchaseObservable))
                    }
                }
    }

    private fun checkGooglePlayResponse(responseBundle: Bundle): Observable<Bundle> =
            Observable.just(responseBundle).
                    doOnNext { log.trace("response Bundle : $it") }.
                    flatMap { response ->
                        checkGooglePlayResponse(IabConstant.extractResponse(response)).map { response }
                    }

    private fun parseSkuDetailJson(skuJson: String,
                                   productIdList: List<ProductBaseInfo>): Product {
        log.trace("sku detail json: $skuJson")
        val skuDetails = SkuDetails.create(JSONObject(skuJson))
        return productIdList.find { it.id.equals(skuDetails.getProductId()) }.
                let { Product.create(it!!, skuDetails) }
    }
}

