package com.github.daneko.android.iab;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.os.RemoteException;

import com.android.vending.billing.IInAppBillingService;
import com.example.android.trivialdrivesample.util.Security;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import fj.Effect;
import fj.F;
import fj.F2;
import fj.F3;
import fj.Function;
import fj.P2;
import fj.Try;
import fj.TryEffect;
import fj.Unit;
import fj.data.Java;
import fj.data.List;
import fj.data.Option;
import fj.data.Set;
import fj.data.Validation;
import fj.function.Strings;
import fj.function.Try0;
import fj.function.Try1;
import fj.function.TryEffect1;

import rx.Observable;
import rx.schedulers.Schedulers;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import com.github.daneko.android.iab.exception.IabException;
import com.github.daneko.android.iab.exception.IabResponseException;
import com.github.daneko.android.iab.model.ActivityResults;
import com.github.daneko.android.iab.model.BillingType;
import com.github.daneko.android.iab.model.GooglePlayResponse;
import com.github.daneko.android.iab.model.IabItemType;
import com.github.daneko.android.iab.model.Product;
import com.github.daneko.android.iab.model.ProductBaseInfo;
import com.github.daneko.android.iab.model.Purchase;
import com.github.daneko.android.iab.model.SkuDetails;

//@formatter:off
/**
 * sample
 * <pre>
 * {@code
 * service = IabService.builder(this, key).
 *   verifyPurchaseLogic(f).
 *   build();
 *
 * service.findBillingItem().
 *   observeOn(AndroidSchedule.mainThread()).subscriber(...)
 * }
 *
 * service.dispose();
 * </pre>
 */
//@formatter:on
@Slf4j
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class IabService {

    private final String packageName;
    private final WeakReference<Activity> weakContext;
    private final Observable<ActivityResults> activityResultsObservable;
    // TODO not use String
    private final F2<String, String, Boolean> verifyPurchaseLogic;
    private final List<ProductBaseInfo> productIdList;


    /**
     * @param context
     * @param base64PublicKey the base64-encoded public key to use for verifying.
     * @param productIds      sku list
     */
    public static Builder builder(
            final IabContext context,
            final String base64PublicKey,
            final Set<ProductBaseInfo> productIds
    ) {

        return new Builder(
                context.getActivity().getApplicationContext().getPackageName(),
                new WeakReference<>(context.getActivity()),
                context.getActivityResultObservable(),
                base64PublicKey,
                productIds
        );
    }

    public static class Builder {
        private String packageName;
        private final WeakReference<Activity> weakContext;
        private final Observable<ActivityResults> activityResultsObservable;
        private final String base64PublicKey;
        private F3<String, String, String, Boolean> verifyPurchaseLogic;
        private final Set<ProductBaseInfo> productIds;

        Builder(
                final String packageName,
                final WeakReference<Activity> weakContext,
                final Observable<ActivityResults> activityResultsObservable,
                final String base64PublicKey,
                final Set<ProductBaseInfo> productIds
        ) {
            this.packageName = packageName;
            this.weakContext = weakContext;
            this.activityResultsObservable = activityResultsObservable;
            this.base64PublicKey = base64PublicKey;
            this.productIds = productIds;
        }

        public IabService build() {
            return new IabService(
                    packageName,
                    weakContext,
                    activityResultsObservable,
                    Function.uncurryF2(
                            Function.curry(
                                    Option.fromNull(verifyPurchaseLogic).orSome(Security::verifyPurchase)
                            ).f(base64PublicKey)),
                    productIds.toList());
        }

        /**
         * if convert logic which verifies purchase data <br>
         * default verify logic is {@link com.example.android.trivialdrivesample.util.Security#verifyPurchase(String, String, String)}
         *
         * @param f verify logic
         * @see com.example.android.trivialdrivesample.util.Security#verifyPurchase(String, String, String)
         */
        public Builder verifyPurchaseLogic(final F3<String, String, String, Boolean> f) {
            verifyPurchaseLogic = f;
            return this;
        }

        public Builder packageName(final String customPackageName) {
            packageName = customPackageName;
            return this;
        }
    }

    private Option<Context> getContext() {
        return getActivity().map(ContextWrapper::getApplicationContext);
    }

    private Option<Activity> getActivity() {
        return Option.fromNull(weakContext.get());
    }

    /**
     * @return onNext で 購入成功 / onError で 購入失敗、キャンセル 成功時は購入情報が付与された値が来る
     * onError IllegalStateException / RemoteException / {@link com.github.daneko.android.iab.exception.IabException } / {@link com.github.daneko.android.iab.exception.IabResponseException }
     */
    public Observable<Product> buyItem(final Product item, final int requestCode) {
        if (item.getPurchaseInfo().isSome()) {
            log.trace("has purchase info {}", item.getPurchaseInfo());
            return Observable.error(new IllegalStateException("item was purchased."));
        }

        final F<IInAppBillingService, Observable<Product>> purchaseRequestF =
                service -> buyRequest(service, item, requestCode);

        final F<ActivityResults, Observable<Product>> successResponseF = res ->
                Observable.create((Observable.OnSubscribe<Product>) subscriber -> {
                    try {
                        subscriber.onNext(buyResponseSuccess(res, item));
                        subscriber.onCompleted();
                    } catch (IabException e) {
                        subscriber.onError(e);
                    }
                });

        final F<ActivityResults, Observable<Product>> errorResponseF = res -> Observable.error(buyResponseFailure(res));

        final Observable<Product> responseObservable = activityResultsObservable
                .filter(res -> res.getRequestCode() == requestCode && res.getData().isSome())
                .first()
                .groupBy(res -> res.getResultCode() == Activity.RESULT_OK
                        && res.getData().map(Intent::getExtras).map(IabConstant::extractResponse)
                        .exists(r -> r == GooglePlayResponse.OK))
                .flatMap(g -> g.flatMap(Option.iif(g.getKey(), successResponseF).orSome(errorResponseF)::f));

        /**
         * 成功時は購入情報のみが付与されているはず…
         */
        return Observable.from(getActivity())
                .flatMap(this::getServiceObservable)
                .flatMap(purchaseRequestF::f)
                .mergeWith(responseObservable)
                .filter(i -> i.getPurchaseInfo().isSome());
    }

    Validation<Exception, PendingIntent> generateBuyRequestIntent(
            final IInAppBillingService service,
            final Product item) {

        final Try0<Bundle, Exception> extractBundle = () -> service.getBuyIntent(
                IabConstant.TARGET_VERSION,
                packageName,
                item.getProductId(),
                item.getIabItemType().getTypeName(),
                item.getDeveloperPayload().toNull());

        final F<Bundle, Validation<Exception, PendingIntent>> extractPendingIntent = buyIntent -> {
            final GooglePlayResponse response = IabConstant.extractResponse(buyIntent);
            return Option.<PendingIntent>iif(response.isSuccess(),
                    buyIntent.getParcelable(IabConstant.BillingServiceConstants.BUY_INTENT.getValue()))
                    .toValidation(new IabResponseException(response,
                            String.format("Unable to buy item, Error code: %s / desc: %s",
                                    response.getCode(), response.getDescription())));
        };

        return Try.f(extractBundle).f().bind(extractPendingIntent::f);
    }

    /**
     * onError type : IntentSender.SendIntentException, RemoteException, IabException, IabResponseException
     */
    Observable<Product> buyRequest(final IInAppBillingService service,
                                   final Product item,
                                   final int requestCode) {

        final F<Activity, TryEffect1<IntentSender, Exception>> startBuyIntent =
                a -> sender -> a.startIntentSenderForResult(sender,
                        requestCode,
                        new Intent(),
                        0,
                        0,
                        0);

        return Observable.create((Observable.OnSubscribe<Product>) subs -> {
                    final Validation<Exception, Activity> extractActivity =
                            getActivity().toValidation(new IabException("activity has gone ???"));

                    generateBuyRequestIntent(service, item).map(PendingIntent::getIntentSender)
                            .bind(intentSender -> extractActivity.map(startBuyIntent)
                                    .bind(f -> TryEffect.f(f).f(intentSender)))
                            .validation(
                                    e -> Effect.f(subs::onError).f(e),
                                    unit -> Effect.f(subs::onNext).f(item)
                            );
                    subs.onCompleted();
                }

        );
    }

    /**
     * 戻り値に購入したItemを入れるかどうか…
     *
     * @param item resにItemTypeがないので… とりあえず購入しようとしたものを渡す
     */
    @SneakyThrows(JSONException.class)
    Product buyResponseSuccess(final ActivityResults res,
                               final Product item) throws IabException {
        final Intent data = res.getData().some();
        final String purchaseData = data.getStringExtra(IabConstant.BillingServiceConstants.INAPP_PURCHASE_DATA.getValue());
        final String dataSignature = data.getStringExtra(IabConstant.BillingServiceConstants.INAPP_SIGNATURE.getValue());

        if (purchaseData == null || dataSignature == null) {
            final String err = String.format("BUG: either purchaseData or dataSignature is null.\n" +
                    "  purchase %s" +
                    "  dataSignature %s" +
                    "  extras %s", purchaseData, dataSignature, data.getExtras().toString());
            log.error(err);
            throw new IabException(err);
        }

        final Purchase purchase = Purchase.create(new JSONObject(purchaseData), dataSignature);

        if (!verifyPurchaseLogic.f(purchaseData, dataSignature)) {
            final String err = String.format("item verify error, purchase:[%s]", purchase.toString());
            log.error(err);
            throw new IabException(err);
        }

        return item.withPurchaseInfo(Option.some(purchase));
    }

    IabResponseException buyResponseFailure(final ActivityResults res) {
        final GooglePlayResponse response =
                IabConstant.extractResponse(res.getData().some().getExtras());

        final String err = String.format(
                "purchase response (onActivityResult) is error. code : %s, desc : %s",
                response.getCode(), response.getDescription());
        return new IabResponseException(response, err);
    }

    /**
     * 購入したアイテムを消費する
     * onNext で成功 onErrorで失敗
     * call onError type: IabException | RemoteException e
     *
     * @throws java.lang.IllegalArgumentException 消費アイテムじゃない or 買ってない
     */
    public Observable<Unit> consumeItem(final Product item) {
        if (item.getBillingType() != BillingType.CONSUMPTION) {
            throw new IllegalArgumentException(String.format("item type is not consumption [%s]", item));
        }

        if (item.getPurchaseInfo().isNone()) {
            throw new IllegalArgumentException("item is not purchase info");
        }

        final F<IInAppBillingService, Observable<Unit>> f = (service ->
                Observable.create((Observable.OnSubscribe<Unit>) subscriber -> {
                            try {
                                subscriber.onNext(consumeItem(service, item));
                                subscriber.onCompleted();
                            } catch (IabResponseException | RemoteException e) {
                                subscriber.onError(e);
                            }
                        }
                )
        );

        return getActivity().map(context ->
                        getServiceObservable(context).
                                flatMap(f::f)
        ).some();
    }

    /**
     * throwしなくてもGooglePlayResponse返せば良い気もするがRemoteExceptionがなぁ…
     */
    Unit consumeItem(final IInAppBillingService service, final Product item)
            throws RemoteException, IabResponseException {

        final GooglePlayResponse response = GooglePlayResponse.create(
                service.consumePurchase(IabConstant.TARGET_VERSION, packageName, item.getPurchaseInfo().some().getToken()));
        if (response != GooglePlayResponse.OK) {
            final String err = String.format("consume error: code:%s, desc: %s", response.getCode(), response.getDescription());
            log.error(err);
            throw new IabResponseException(response, err);
        }
        return Unit.unit();
    }


    /**
     * 課金アイテム情報全件取得
     * <p>
     * call onError type: IabException | JSONException | RemoteException e
     */
    public Observable<Product> findBillingItem() {
        log.trace("#findBillingItem");
        if (getActivity().isNone()) {
            // throw Runtime かどうか…
            return Observable.create((Observable.OnSubscribe<Product>) s -> s.onError(new IabException("context reference is clear")));
        }

        return getActivity().map(context ->
                        getServiceObservable(context).
                                flatMap(this::findBillingItem)
        ).some();
    }

    /**
     * {@link com.android.vending.billing.IInAppBillingService#getSkuDetails(int, String, String, android.os.Bundle)} のラッパー
     * <p>
     * call onError type: IabException | JSONException | RemoteException e
     */
    Observable<Product> findBillingItem(final IInAppBillingService service) {

        Try1<IabItemType, List<Product>, Exception> find = iabType -> {
            final ArrayList<String> productIdRawList = Java.<String>List_ArrayList()
                    .f(productIdList.filter(p -> p.getType().getIabItemType() == iabType)
                            .map(ProductBaseInfo::getId));
            if (productIdRawList.isEmpty()) {
                return List.list();
            }

            final Bundle querySkus = new Bundle();
            querySkus.putStringArrayList(
                    IabConstant.GetSkuDetailKey.ITEM_LIST.getValue(),
                    productIdRawList
            );

            final Bundle responseBundle = service.getSkuDetails(
                    IabConstant.TARGET_VERSION,
                    packageName,
                    iabType.getTypeName(),
                    querySkus);

            log.trace("query Bundle: {}", querySkus);
            log.trace("response Bundle: {}", responseBundle);

            final GooglePlayResponse response = IabConstant.extractResponse(responseBundle);

            if (response != GooglePlayResponse.OK) {
                final String err = String.format("getSkuDetails\n" +
                                "Error response: c:%d / desc:%s\n" +
                                "iab type %s\n" +
                                "query Bundle %s\n" +
                                "response Bundle %s\n",
                        response.getCode(),
                        response.getDescription(),
                        iabType,
                        querySkus,
                        responseBundle);
                throw new IabResponseException(response, err);
            }

            if (!responseBundle.containsKey(IabConstant.BillingServiceConstants.GET_SKU_DETAILS_LIST.getValue())) {
                final String err = "getSkuDetails, No detail list";
                throw new IabException(err);
            }

            return List.iterableList(
                    responseBundle.getStringArrayList(IabConstant.BillingServiceConstants.GET_SKU_DETAILS_LIST.getValue())
            ).map(this::skuDetailJsonParse);
        };

        // TODO 多分もうちょっとどうにかすればObservableでなくListで返せる気がする…
        // List.list(IabItemType.values()).map(Try.f(find)::f);

        return Observable.create((Observable.OnSubscribe<Product>) s -> {
            for (IabItemType iabType : IabItemType.values()) {
                try {
                    final List<Purchase> purchases = findPurchase(service, iabType);
                    find.f(iabType).foreach(billingItem -> {
                        final Option<Purchase> purchase = purchases.find(p -> p.getProductId().equals(billingItem.getProductId()));
                        s.onNext(billingItem.withPurchaseInfo(purchase));
                        return Unit.unit();
                    });
                } catch (Exception e) {
                    s.onError(e);
                }
            }
            s.onCompleted();
        });
    }

    @SneakyThrows(JSONException.class)
    Product skuDetailJsonParse(final String json) {
        final SkuDetails skuDetails = SkuDetails.create(new JSONObject(json));
        final ProductBaseInfo productBaseInfo = productIdList.find(p -> p.getId().equals(skuDetails.getProductId())).some();
        log.trace("sku detail json {}", json);
        return Product.create(productBaseInfo, skuDetails);
    }

    /**
     * 購入済み情報を返すよ
     * {@link com.android.vending.billing.IInAppBillingService#getPurchases(int, String, String, String)}のラッパー
     */
    List<Purchase> findPurchase(final IInAppBillingService service,
                                final IabItemType itemType) throws RemoteException, IabException, IabResponseException {
        return _findPurchase(service, itemType, null, new ArrayList<>());
    }

    @SneakyThrows(JSONException.class)
    List<Purchase> _findPurchase(final IInAppBillingService service,
                                 final IabItemType itemType,
                                 final String continuationToken,
                                 java.util.List<Purchase> acc) throws RemoteException, IabException, IabResponseException {

        log.trace("#findPurchase");

        final Bundle purchases = service.getPurchases(
                IabConstant.TARGET_VERSION,
                packageName,
                itemType.getTypeName(),
                continuationToken);

        log.trace("purchases is {}", purchases);

        final GooglePlayResponse googlePlayResponse = IabConstant.extractResponse(purchases);
        if (googlePlayResponse != GooglePlayResponse.OK) {
            final String err = String.format("Unable to buy item, Error response: c:%d / desc:%s", googlePlayResponse.getCode(), googlePlayResponse.getDescription());
            log.error(err);
            throw new IabResponseException(googlePlayResponse, err);
        }
        if (!IabConstant.BillingServiceConstants.hasPurchaseKey(purchases)) {
            final String err = "Bundle returned from getPurchases() doesn't contain required fields. purchases: " + purchases;
            log.error(err);
            throw new IabException(err);
        }

        List<String> ownedSkus = List.iterableList(purchases.getStringArrayList(
                IabConstant.BillingServiceConstants.INAPP_ITEM_LIST.getValue()));
        List<String> purchaseDataList = List.iterableList(purchases.getStringArrayList(
                IabConstant.BillingServiceConstants.INAPP_PURCHASE_DATA_LIST.getValue()));
        List<String> signatureList = List.iterableList(purchases.getStringArrayList(
                IabConstant.BillingServiceConstants.INAPP_SIGNATURE_LIST.getValue()));

        log.trace("item type {}", itemType.getTypeName());
        log.trace("purchases {}", purchases.toString());
        log.trace("ownedSkus {}", ownedSkus.toString());
        log.trace("purchaseDataList {}", purchaseDataList.toString());
        log.trace("signatureList {}", signatureList.toString());

        // P3<A,B,C>にする方法ないかなぁ…
        // List#foreachを使用してlambda使うとtry/catchを中で書く必要性が発生するのでforで…
        for (P2<String, Integer> purchaseDataWithIndex : purchaseDataList.zipIndex()) {
            final String purchaseData = purchaseDataWithIndex._1();
            final Integer index = purchaseDataWithIndex._2();
            final String signature = signatureList.index(index);
            final String sku = ownedSkus.index(index);

            if (!verifyPurchaseLogic.f(purchaseData, signature)) {
                final String err = String.format(
                        "Purchase signature verification **FAILED**. Not adding item.\n" +
                                "   Purchase data: %s" +
                                "   Signature: %s"
                        , purchaseData, signature);
                throw new IabException(err);
            }

            log.trace("Sku is owned: " + sku);
            Purchase purchase = Purchase.create(new JSONObject(purchaseData), signature);

            if (!Strings.isNotNullOrEmpty.f(purchase.getToken())) {
                final String err = String.format(
                        "BUG: empty/null token!" +
                                "Purchase data: %s",
                        purchaseData);
                log.error(err);
                throw new IabException(err);
            }

            acc.add(purchase);
        }

        final String nextContinueToken = purchases.getString(IabConstant.BillingServiceConstants.INAPP_CONTINUATION_TOKEN.getValue());
        if (Strings.isNotNullOrEmpty.f(nextContinueToken)) {
            log.trace("has continuation token : {}", nextContinueToken);
            return _findPurchase(service, itemType, nextContinueToken, acc);
        } else {
            return List.iterableList(acc);
        }
    }

    /**
     */
    Observable<IInAppBillingService> getServiceObservable(final Activity activity) {
        return IabServiceConnection
                .getConnection(activity)
                .getServiceObservable()
                .subscribeOn(Schedulers.newThread());
    }

    /**
     */
    public final Unit dispose() {
        return getActivity().map(IabServiceConnection::dispose).orSome(Unit.unit());
    }
}

