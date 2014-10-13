package jp.daneko.android.inappv3;


import android.os.Bundle;

import com.android.vending.billing.IInAppBillingService;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import org.json.JSONException;
import org.json.JSONObject;
import org.mockito.ArgumentMatcher;

import java.util.ArrayList;
import java.util.Arrays;

import fj.F;
import fj.F3;
import fj.Ord;
import fj.P;
import fj.P2;
import fj.data.Java;
import fj.data.List;
import fj.data.Option;
import fj.data.Set;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import jp.daneko.android.inappv3.model.Product;
import jp.daneko.android.inappv3.model.BillingType;
import jp.daneko.android.inappv3.model.IabItemType;
import jp.daneko.android.inappv3.model.ProductBaseInfo;
import jp.daneko.android.inappv3.model.Purchase;
import jp.daneko.android.inappv3.model.SkuDetails;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


/**
 */
@Slf4j
@RunWith(RobolectricTestRunner.class)
@Config(emulateSdk = 18)
public class IabServiceTest {

    static Bundle emptyPurchaseResponse() {
        return createExpectPurchaseResponse(IabConstant.GooglePlayResponse.OK, new ArrayList<String>(), List.<JSONObject>nil(), new ArrayList<String>(), null);
    }

    static Bundle createExpectPurchaseResponse(
            @NonNull final IabConstant.GooglePlayResponse resultCode,
            @NonNull final ArrayList<String> itemList,
            @NonNull final List<JSONObject> dataList,
            @NonNull final ArrayList<String> signatureList,
            final String continuationToken) {

        final Bundle bundle = new Bundle();
        bundle.putInt("RESPONSE_CODE", resultCode.getCode());
        bundle.putStringArrayList("INAPP_PURCHASE_ITEM_LIST", itemList);
        final List<String> stringList = dataList.map(new F<JSONObject, String>() {
            @Override
            public String f(JSONObject jsonObject) {
                return jsonObject.toString();
            }
        });
        bundle.putStringArrayList("INAPP_PURCHASE_DATA_LIST", Java.<String>Array_ArrayList().f(stringList.toArray()));
        bundle.putStringArrayList("INAPP_DATA_SIGNATURE_LIST", signatureList);
        bundle.putString("INAPP_CONTINUATION_TOKEN", continuationToken);
        return bundle;
    }

    static Bundle createExpectPurchaseResponse(
            @NonNull final IabConstant.GooglePlayResponse resultCode,
            @NonNull final List<Purchase> expectList,
            final String continuationToken) {

        final F<Purchase, String> extractId = new F<Purchase, String>() {
            @Override
            public String f(Purchase purchase) {
                return purchase.getProductId();
            }
        };

        final F<Purchase, JSONObject> extractJson = new F<Purchase, JSONObject>() {
            @Override
            public JSONObject f(Purchase purchase) {
                return purchase.getOriginalJson();
            }
        };

        final F<Purchase, String> extractSignature = new F<Purchase, String>() {
            @Override
            public String f(Purchase purchase) {
                return purchase.getSignature();
            }
        };

        return createExpectPurchaseResponse(
                resultCode,
                Java.<String>Array_ArrayList().f(expectList.map(extractId).toArray()),
                expectList.map(extractJson),
                Java.<String>Array_ArrayList().f(expectList.map(extractSignature).toArray()),
                continuationToken
        );

    }

    @SneakyThrows(JSONException.class)
    static JSONObject createSkuJson(
            @NonNull final String productId,
            @NonNull final IabItemType itemType,
            @NonNull final String price,
            @NonNull final String title,
            @NonNull final String description) {

        return new JSONObject(String.format("{ " +
                "\"productId\" : \"%s\", " +
                "\"type\" : \"%s\", " +
                "\"price\" : \"%s\"," +
                "\"title\" : \"%s\"," +
                "\"description\" : \"%s\" " +
                "}", productId, itemType.getTypeName(), price, title, description)
        );
    }

    static P2<ProductBaseInfo, SkuDetails> createBaseData(
            @NonNull final BillingType type,
            @NonNull final String productId,
            @NonNull final String price,
            @NonNull final String title,
            @NonNull final String description) {

        final JSONObject skuJson = createSkuJson(productId, type.getIabItemType(), price, title, description);
        return P.p(new ProductBaseInfo(productId, type), SkuDetails.create(skuJson));
    }

    static Bundle createGetSkuDetailsResult(
            @NonNull final IabConstant.GooglePlayResponse resultCode,
            @NonNull final List<JSONObject> expectedList) {
        final Bundle bundle = new Bundle();
        bundle.putInt("RESPONSE_CODE", resultCode.getCode());

        final List<String> stringList = expectedList.map(new F<JSONObject, String>() {
            @Override
            public String f(JSONObject jsonObject) {
                return jsonObject.toString();
            }
        });
        final ArrayList<String> strings = Java.<String>List_ArrayList().f(stringList);
        bundle.putStringArrayList("DETAILS_LIST", strings);
        return bundle;

    }


    @AllArgsConstructor
    static class BundleItemIdMatcher extends ArgumentMatcher<Bundle> {

        private final Bundle expected;

        @Override
        public boolean matches(Object argument) {
            return !(argument == null || !(argument instanceof Bundle)) &&
                    equalBundles(expected, (Bundle) argument);
        }

        static boolean equalBundles(Bundle a, Bundle b) {
            if (a.size() != b.size()) return false;
            final String key = "ITEM_ID_LIST";
            if (!a.containsKey(key) || !b.containsKey(key)) return false;

            final List<String> aList = List.iterableList(a.getStringArrayList(key)).sort(Ord.stringOrd);
            final List<String> bList = List.iterableList(b.getStringArrayList(key)).sort(Ord.stringOrd);

            return aList.equals(bList);
        }
    }

    static Bundle bundleItemIdEq(@NonNull final Bundle expected) {
        return argThat(new BundleItemIdMatcher(expected));
    }

    /**
     */
    @Test
    public void findBillingItem_購入情報無し() {
        try {

            final String packageName = "com.example";

            final P2<ProductBaseInfo, SkuDetails> test1 = createBaseData(BillingType.CONSUMPTION, "test1", "$1.00", "Test 1 title", "test 1 description");
            final P2<ProductBaseInfo, SkuDetails> test2 = createBaseData(BillingType.MANAGE, "test2", "$2.00", "Test 2 title", "test 2 description");
            final P2<ProductBaseInfo, SkuDetails> test3 = createBaseData(BillingType.SUBSCRIPTION, "test3", "$3.00", "Test 3 title", "test 3 description");

            final Bundle inappBundle = new Bundle();
            inappBundle.putStringArrayList("ITEM_ID_LIST", new ArrayList<String>(Arrays.asList(test1._2().getProductId(), test2._2().getProductId())));
            final Bundle inappResultExpectBundle = createGetSkuDetailsResult(IabConstant.GooglePlayResponse.OK, List.list(test1._2().getJson(), test2._2().getJson()));

            final Bundle subsBundle = new Bundle();
            subsBundle.putStringArrayList("ITEM_ID_LIST", new ArrayList<String>(Arrays.asList(test3._2().getProductId())));
            final Bundle subsResultExpectBundle = createGetSkuDetailsResult(IabConstant.GooglePlayResponse.OK, List.list(test3._2().getJson()));

            final IInAppBillingService mockService = mock(IInAppBillingService.class);

            when(mockService.getSkuDetails(eq(3), eq(packageName), eq(IabItemType.INAPP.getTypeName()), bundleItemIdEq(inappBundle))).thenReturn(inappResultExpectBundle);
            when(mockService.getSkuDetails(eq(3), eq(packageName), eq(IabItemType.SUBSCRIPTION.getTypeName()), bundleItemIdEq(subsBundle))).thenReturn(subsResultExpectBundle);

            final Bundle purchaseResultExpectBundle = emptyPurchaseResponse();

            when(mockService.getPurchases(eq(3), eq(packageName), eq(IabItemType.INAPP.getTypeName()), isNull(String.class))).thenReturn(purchaseResultExpectBundle);
            when(mockService.getPurchases(eq(3), eq(packageName), eq(IabItemType.SUBSCRIPTION.getTypeName()), isNull(String.class))).thenReturn(purchaseResultExpectBundle);

            @SuppressWarnings("unchecked")
            final List<P2<ProductBaseInfo, SkuDetails>> testBaseData = List.list(test1, test2, test3);

            final Set<ProductBaseInfo> idSet = Set.iterableSet(ProductBaseInfo.ord,
                    testBaseData.map(new F<P2<ProductBaseInfo, SkuDetails>, ProductBaseInfo>() {
                        @Override
                        public ProductBaseInfo f(P2<ProductBaseInfo, SkuDetails> p2) {
                            return p2._1();
                        }
                    })
            );

            final List<Product> target = List.iterableList(
                    new IabService.Builder(packageName, null, null, "test", idSet).
                            build().
                            findBillingItem(mockService).toBlocking().toIterable());


            final List<Product> expected = testBaseData.map(new F<P2<ProductBaseInfo, SkuDetails>, Product>() {
                @Override
                public Product f(P2<ProductBaseInfo, SkuDetails> p2) {
                    return Product.create(p2._1(), p2._2());
                }
            });

            // TODO create custom matcher, like assertThat(fj.data.List).sameElements(fj.data.List)
            assertThat(target).containsOnly(expected.toArray().array(Product[].class));

        } catch (Exception e) {
            fail("error:", e);
        }
    }

    /**
     * <pre>
     * {@code
     *
     * {
     *   "orderId":"12999763169054705758.1394237180765537",
     *   "packageName":"jp.daneko.example",
     *   "productId":"consumption_item_001",
     *   "purchaseTime":1413738315217,
     *   "purchaseState":0,
     *   "purchaseToken":"hogefugapiyo"
     * }
     *
     * }
     * </pre>
     *
     * @see <a href="https://developer.android.com/google/play/billing/billing_reference.html#getPurchases">developer site</a>
     */
    @SneakyThrows(JSONException.class)
    static Purchase createPurchase(
            @NonNull final String orderId,
            @NonNull final String packageName,
            @NonNull final String productId,
            final long purchaseTime,
            final int purchaseState,
            @NonNull final String purchaseToken,
            @NonNull final String signature) {

        return Purchase.create(new JSONObject(String.format("{ " +
                "\"orderId\" : \"%s\"," +
                "\"packageName\" : \"%s\"," +
                "\"productId\" : \"%s\"," +
                "\"purchaseTime\" : %d," +
                "\"purchaseState\" : %d," +
                "\"purchaseToken\" : \"%s\"" +
                "}", orderId, packageName, productId, purchaseTime, purchaseState, purchaseToken)
        ), signature);
    }

    @Test
    public void findBillingItem_購入情報あり() {
        try {

            final String packageName = "com.example";

            final P2<ProductBaseInfo, SkuDetails> test1 = createBaseData(BillingType.CONSUMPTION, "test1", "$1.00", "Test 1 title", "test 1 description");
            final P2<ProductBaseInfo, SkuDetails> test2 = createBaseData(BillingType.MANAGE, "test2", "$2.00", "Test 2 title", "test 2 description");
            final P2<ProductBaseInfo, SkuDetails> test3 = createBaseData(BillingType.SUBSCRIPTION, "test3", "$3.00", "Test 3 title", "test 3 description");

            final Bundle inappBundle = new Bundle();
            inappBundle.putStringArrayList("ITEM_ID_LIST", new ArrayList<String>(Arrays.asList(test1._2().getProductId(), test2._2().getProductId())));
            final Bundle inappResultExpectBundle = createGetSkuDetailsResult(IabConstant.GooglePlayResponse.OK, List.list(test1._2().getJson(), test2._2().getJson()));

            final Bundle subsBundle = new Bundle();
            subsBundle.putStringArrayList("ITEM_ID_LIST", new ArrayList<String>(Arrays.asList(test3._2().getProductId())));
            final Bundle subsResultExpectBundle = createGetSkuDetailsResult(IabConstant.GooglePlayResponse.OK, List.list(test3._2().getJson()));

            final IInAppBillingService mockService = mock(IInAppBillingService.class);

            when(mockService.getSkuDetails(eq(3), eq(packageName), eq(IabItemType.INAPP.getTypeName()), bundleItemIdEq(inappBundle))).thenReturn(inappResultExpectBundle);
            when(mockService.getSkuDetails(eq(3), eq(packageName), eq(IabItemType.SUBSCRIPTION.getTypeName()), bundleItemIdEq(subsBundle))).thenReturn(subsResultExpectBundle);

            final Purchase purchaseData = createPurchase("12999763169054705758.1394237180765537",
                    packageName,
                    test1._2().getProductId(),
                    1413738315217L,
                    0,
                    "hogefugapiyo",
                    "signature1");
            final Bundle inappPurchaseResponse = createExpectPurchaseResponse(
                    IabConstant.GooglePlayResponse.OK,
                    List.list(purchaseData),
                    null);
            final Bundle subsPurchaseResponse = emptyPurchaseResponse();

            when(mockService.getPurchases(eq(3), eq(packageName), eq(IabItemType.INAPP.getTypeName()), isNull(String.class))).thenReturn(inappPurchaseResponse);
            when(mockService.getPurchases(eq(3), eq(packageName), eq(IabItemType.SUBSCRIPTION.getTypeName()), isNull(String.class))).thenReturn(subsPurchaseResponse);

            @SuppressWarnings("unchecked")
            final List<P2<ProductBaseInfo, SkuDetails>> testBaseData = List.list(test1, test2, test3);

            final Set<ProductBaseInfo> idSet = Set.iterableSet(ProductBaseInfo.ord,
                    testBaseData.map(new F<P2<ProductBaseInfo, SkuDetails>, ProductBaseInfo>() {
                        @Override
                        public ProductBaseInfo f(P2<ProductBaseInfo, SkuDetails> p2) {
                            return p2._1();
                        }
                    })
            );

            final List<Product> target = List.iterableList(
                    new IabService.Builder(packageName, null, null, "test", idSet).
                            verifyPurchaseLogic(new F3<String, String, String, Boolean>() {
                                @Override
                                public Boolean f(String s, String s2, String s3) {
                                    // ignore verify
                                    return true;
                                }
                            }).
                            build().
                            findBillingItem(mockService).toBlocking().toIterable());


            final List<Product> expected = testBaseData.map(new F<P2<ProductBaseInfo, SkuDetails>, Product>() {
                @Override
                public Product f(P2<ProductBaseInfo, SkuDetails> p2) {
                    final Product product = Product.create(p2._1(), p2._2());
                    if (product.getProductId().equals(purchaseData.getProductId())) {
                        return product.withPurchaseInfo(Option.some(purchaseData));
                    }
                    return product;
                }
            });

            // TODO create custom matcher, like assertThat(fj.data.List).sameElements(fj.data.List)
            assertThat(target).containsOnly(expected.toArray().array(Product[].class));

        } catch (Exception e) {
            fail(e.getMessage(), e);
        }
    }

    @Test
    public void findBillingItem_withContinuationToken() {
        try {

            final String packageName = "com.example";

            final P2<ProductBaseInfo, SkuDetails> test1 = createBaseData(BillingType.CONSUMPTION, "test1", "$1.00", "Test 1 title", "test 1 description");
            final P2<ProductBaseInfo, SkuDetails> test2 = createBaseData(BillingType.MANAGE, "test2", "$2.00", "Test 2 title", "test 2 description");
            final P2<ProductBaseInfo, SkuDetails> test3 = createBaseData(BillingType.SUBSCRIPTION, "test3", "$3.00", "Test 3 title", "test 3 description");

            final Bundle inappBundle = new Bundle();
            inappBundle.putStringArrayList("ITEM_ID_LIST", new ArrayList<String>(Arrays.asList(test1._2().getProductId(), test2._2().getProductId())));
            final Bundle inappResultExpectBundle = createGetSkuDetailsResult(IabConstant.GooglePlayResponse.OK, List.list(test1._2().getJson(), test2._2().getJson()));

            final Bundle subsBundle = new Bundle();
            subsBundle.putStringArrayList("ITEM_ID_LIST", new ArrayList<String>(Arrays.asList(test3._2().getProductId())));
            final Bundle subsResultExpectBundle = createGetSkuDetailsResult(IabConstant.GooglePlayResponse.OK, List.list(test3._2().getJson()));

            final IInAppBillingService mockService = mock(IInAppBillingService.class);

            when(mockService.getSkuDetails(eq(3), eq(packageName), eq(IabItemType.INAPP.getTypeName()), bundleItemIdEq(inappBundle))).thenReturn(inappResultExpectBundle);
            when(mockService.getSkuDetails(eq(3), eq(packageName), eq(IabItemType.SUBSCRIPTION.getTypeName()), bundleItemIdEq(subsBundle))).thenReturn(subsResultExpectBundle);

            final String continuation = "continuation1";

            final Purchase purchaseData1 = createPurchase("12999763169054705758.1394237180765537",
                    packageName,
                    test1._2().getProductId(),
                    1413738315217L,
                    0,
                    "hogefugapiyo",
                    "signature1");

            final Bundle inappPurchaseResponse = createExpectPurchaseResponse(
                    IabConstant.GooglePlayResponse.OK,
                    List.list(purchaseData1),
                    continuation);

            final Purchase purchaseData2 = createPurchase("13000000000000000000.1394237180765537",
                    packageName,
                    test2._2().getProductId(),
                    1413738320000L,
                    0,
                    "aaaabbbbcccc",
                    "signature2");
            final Bundle inappPurchaseResponse2 = createExpectPurchaseResponse(
                    IabConstant.GooglePlayResponse.OK,
                    List.list(purchaseData2),
                    null);
            final Bundle subsPurchaseResponse = emptyPurchaseResponse();

            when(mockService.getPurchases(eq(3), eq(packageName), eq(IabItemType.INAPP.getTypeName()), isNull(String.class))).thenReturn(inappPurchaseResponse);
            when(mockService.getPurchases(eq(3), eq(packageName), eq(IabItemType.INAPP.getTypeName()), eq(continuation))).thenReturn(inappPurchaseResponse2);
            when(mockService.getPurchases(eq(3), eq(packageName), eq(IabItemType.SUBSCRIPTION.getTypeName()), isNull(String.class))).thenReturn(subsPurchaseResponse);

            @SuppressWarnings("unchecked")
            final List<P2<ProductBaseInfo, SkuDetails>> testBaseData = List.list(test1, test2, test3);

            final Set<ProductBaseInfo> idSet = Set.iterableSet(ProductBaseInfo.ord,
                    testBaseData.map(new F<P2<ProductBaseInfo, SkuDetails>, ProductBaseInfo>() {
                        @Override
                        public ProductBaseInfo f(P2<ProductBaseInfo, SkuDetails> p2) {
                            return p2._1();
                        }
                    })
            );

            final List<Product> target = List.iterableList(
                    new IabService.Builder(packageName, null, null, "test", idSet).
                            verifyPurchaseLogic(new F3<String, String, String, Boolean>() {
                                @Override
                                public Boolean f(String s, String s2, String s3) {
                                    // ignore verify
                                    return true;
                                }
                            }).
                            build().
                            findBillingItem(mockService).toBlocking().toIterable());


            final List<Product> expected = testBaseData.map(new F<P2<ProductBaseInfo, SkuDetails>, Product>() {
                @Override
                public Product f(P2<ProductBaseInfo, SkuDetails> p2) {
                    final Product product = Product.create(p2._1(), p2._2());
                    if (product.getProductId().equals(purchaseData1.getProductId())) {
                        return product.withPurchaseInfo(Option.some(purchaseData1));
                    }
                    if (product.getProductId().equals(purchaseData2.getProductId())) {
                        return product.withPurchaseInfo(Option.some(purchaseData2));
                    }
                    return product;
                }
            });

            // TODO create custom matcher, like assertThat(fj.data.List).sameElements(fj.data.List)
            assertThat(target).containsOnly(expected.toArray().array(Product[].class));

        } catch (Exception e) {
            fail("error:", e);
        }
    }
}
