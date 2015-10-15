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
import fj.function.TryEffect1;

import rx.Observable;
import rx.Observer;
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
                validationToObservable(buyResponseSuccess(res, item));

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

        final Validation<Exception, Activity> extractActivity =
                getActivity().toValidation(new IabException("activity has gone ???"));

        return validationToObservable(generateBuyRequestIntent(service, item).map(PendingIntent::getIntentSender)
                .bind(intentSender -> extractActivity.map(startBuyIntent)
                        .bind(f -> TryEffect.f(f).f(intentSender))))
                .map(unit -> item);
    }

    Validation<Exception, Product> buyResponseSuccess(final ActivityResults res, final Product item) {

        final Validation<String, String> purchaseDatas = res.getData().bind(data -> Option.fromNull(
                data.getStringExtra(IabConstant.BillingServiceConstants.INAPP_PURCHASE_DATA.getValue())))
                .toValidation("Bug: purchaseData is null");

        final Validation<String, String> dataSignatures = res.getData().bind(data -> Option.fromNull(
                data.getStringExtra(IabConstant.BillingServiceConstants.INAPP_SIGNATURE.getValue())))
                .toValidation("Bug: dataSignature is null");

        final F2<String, String, Validation<Exception, Purchase>> purchaseFactory = (purchaseData, dataSignature) -> {
            try {
                return Validation.condition(verifyPurchaseLogic.f(purchaseData, dataSignature),
                        new IabException("item verify error, purchase:x" + purchaseData + " signature: " + dataSignature),
                        Purchase.create(new JSONObject(purchaseData), dataSignature)
                );
            } catch (JSONException e) {
                return Validation.fail(e);
            }
        };

        return purchaseDatas.accumulate(dataSignatures, purchaseFactory)
                .f().map(eList -> new IabException(eList.foldLeft((a, b) -> a + "\n" + b, "")))
                .validation(e -> Validation.fail((Exception) e), Function.identity())
                .map(purchase -> item.withPurchaseInfo(Option.some(purchase)));
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
     * call onError type: IabException | RemoteException | IllegalArgumentException
     */
    public Observable<Unit> consumeItem(final Product item) {
        if (item.getBillingType() != BillingType.CONSUMPTION) {
            return Observable
                    .error(new IllegalArgumentException(String.format("item type is not consumption [%s]", item)));
        }

        final F<IInAppBillingService, Observable<Unit>> f =
                service -> validationToObservable(consumeItem(service, item));

        return Observable.from(getActivity())
                .flatMap(this::getServiceObservable)
                .flatMap(f::f);
    }

    /**
     */
    Validation<Exception, Unit> consumeItem(final IInAppBillingService service, final Product item) {

        final F<GooglePlayResponse, Exception> failF = response -> {
            final String err = String.format("consume error: code:%s, desc: %s", response.getCode(), response.getDescription());
            log.error(err);
            return new IabResponseException(response, err);
        };

        try {
            return Validation.condition(item.getPurchaseInfo().isSome(),
                    (Exception) new IllegalArgumentException("item is not purchase info"),
                    GooglePlayResponse.create(service.consumePurchase(
                            IabConstant.TARGET_VERSION, packageName, item.getPurchaseInfo().some().getToken())))
                    .bind(response -> Validation.condition(response.isSuccess(),
                                    failF.f(response),
                                    Unit.unit())
                    );
        } catch (RemoteException e) {
            return Validation.fail(e);
        }
    }


    /**
     * 課金アイテム情報全件取得
     * <p>
     * call onError type: IabException | JSONException | RemoteException e
     */
    public Observable<Product> findBillingItem() {
        log.trace("#findBillingItem");
        return Observable.from(getActivity())
                .flatMap(this::getServiceObservable)
                .flatMap(this::findBillingItem);
    }

    /**
     * wrapper {@link com.android.vending.billing.IInAppBillingService#getSkuDetails(int, String, String, android.os.Bundle)}
     * <p>
     * call onError type: IabException | JSONException | RemoteException e
     * TODO: refactor
     */
    Observable<Product> findBillingItem(final IInAppBillingService service) {

        final F<IabItemType, Validation<Exception, List<Product>>> find = iabType -> {
            final List<String> productIds = productIdList
                    .filter(p -> p.getType().getIabItemType() == iabType)
                    .map(ProductBaseInfo::getId);
            if (productIds.isEmpty()) {
                return Validation.success(List.list());
            }

            final Bundle querySkus = new Bundle();
            querySkus.putStringArrayList(
                    IabConstant.GetSkuDetailKey.ITEM_LIST.getValue(),
                    Java.<String>List_ArrayList().f(productIds)
            );

            log.trace("query Bundle: {}", querySkus);

            try {
                final Bundle responseBundle = service.getSkuDetails(
                        IabConstant.TARGET_VERSION,
                        packageName,
                        iabType.getTypeName(),
                        querySkus);

                log.trace("response Bundle: {}", responseBundle);

                final GooglePlayResponse response = IabConstant.extractResponse(responseBundle);

                if (!response.isSuccess()) {
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
                    return Validation.fail(new IabResponseException(response, err));
                }

                if (!responseBundle.containsKey(IabConstant.BillingServiceConstants.GET_SKU_DETAILS_LIST.getValue())) {
                    final String err = "getSkuDetails, No detail list";
                    return Validation.fail(new IabException(err));
                }

                return Validation.success(List.list(responseBundle
                                .getStringArrayList(IabConstant.BillingServiceConstants.GET_SKU_DETAILS_LIST.getValue())
                ).map(this::skuDetailJsonParse));
            } catch (RemoteException e) {
                return Validation.fail(e);
            }
        };

        return Observable.create((Observable.OnSubscribe<Product>) s -> {
            for (IabItemType iabType : IabItemType.values()) {
                try {
                    final List<Purchase> purchases = findPurchase(service, iabType);
                    find.f(iabType).validation(Effect.f(s::onError), products -> {
                        for (Product product : products) {
                            final Option<Purchase> purchase =
                                    purchases.find(p -> p.getProductId().equals(product.getProductId()));
                            s.onNext(product.withPurchaseInfo(purchase));
                        }
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
        if (!googlePlayResponse.isSuccess()) {
            final String err = String.format("Unable to buy item, Error response: c:%d / desc:%s",
                    googlePlayResponse.getCode(), googlePlayResponse.getDescription());
            log.error(err);
            throw new IabResponseException(googlePlayResponse, err);
        }
        if (!IabConstant.BillingServiceConstants.hasPurchaseKey(purchases)) {
            final String err = "Bundle returned from getPurchases() doesn't contain required fields. " +
                    "purchases: " + purchases;
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
                final String err = String.format("BUG: empty/null token! Purchase data: %s", purchaseData);
                log.error(err);
                throw new IabException(err);
            }

            acc.add(purchase);
        }

        final String nextContinueToken = purchases
                .getString(IabConstant.BillingServiceConstants.INAPP_CONTINUATION_TOKEN.getValue());

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

    static <E extends Throwable, A> Observable<A> validationToObservable(Validation<E, A> validation) {
        final F2<Observer<? super A>, A, Unit> onSuccess = (subs, value) -> {
            subs.onNext(value);
            subs.onCompleted();
            return Unit.unit();
        };

        return Observable.defer(() -> Observable.create((Observable.OnSubscribe<A>) subs -> validation.validation(
                        Effect.f(subs::onError),
                        s -> onSuccess.f(subs, s)))
        );
    }
}
