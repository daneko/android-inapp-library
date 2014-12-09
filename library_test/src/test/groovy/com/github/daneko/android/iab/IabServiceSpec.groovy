package com.github.daneko.android.iab

import android.os.Bundle

import com.android.vending.billing.IInAppBillingService
import com.github.daneko.android.iab.model.*

import org.json.JSONObject

import pl.polidea.robospock.RoboSpecification

import fj.*
import fj.data.Java
import fj.data.List
import fj.data.Option
import fj.data.Set

class IabServiceSpec extends RoboSpecification {

    static Bundle emptyPurchaseResponse() {
        return createExpectPurchaseResponse(IabConstant.GooglePlayResponse.OK, new ArrayList<String>(), List.<JSONObject> nil(), new ArrayList<String>(), null)
    }

    static Bundle createExpectPurchaseResponse(
            final IabConstant.GooglePlayResponse resultCode,
            final ArrayList<String> itemList,
            final List<JSONObject> dataList,
            final ArrayList<String> signatureList,
            final String continuationToken) {

        final Bundle bundle = new Bundle()
        bundle.putInt("RESPONSE_CODE", resultCode.getCode())
        bundle.putStringArrayList("INAPP_PURCHASE_ITEM_LIST", itemList)
        final List<String> stringList = dataList.map(new F<JSONObject, String>() {
            @Override
            public String f(JSONObject jsonObject) {
                return jsonObject.toString()
            }
        })
        bundle.putStringArrayList("INAPP_PURCHASE_DATA_LIST", Java.<String> Array_ArrayList().f(stringList.toArray()))
        bundle.putStringArrayList("INAPP_DATA_SIGNATURE_LIST", signatureList)
        bundle.putString("INAPP_CONTINUATION_TOKEN", continuationToken)
        return bundle
    }

    static Bundle createExpectPurchaseResponse(
            final IabConstant.GooglePlayResponse resultCode,
            final List<Purchase> expectList,
            final String continuationToken) {

        final F<Purchase, String> extractId = new F<Purchase, String>() {
            @Override
            public String f(Purchase purchase) {
                return purchase.getProductId()
            }
        }

        final F<Purchase, JSONObject> extractJson = new F<Purchase, JSONObject>() {
            @Override
            public JSONObject f(Purchase purchase) {
                return purchase.getOriginalJson()
            }
        }

        final F<Purchase, String> extractSignature = new F<Purchase, String>() {
            @Override
            public String f(Purchase purchase) {
                return purchase.getSignature()
            }
        }

        return createExpectPurchaseResponse(
                resultCode,
                Java.<String> Array_ArrayList().f(expectList.map(extractId).toArray()),
                expectList.map(extractJson),
                Java.<String> Array_ArrayList().f(expectList.map(extractSignature).toArray()),
                continuationToken
        )

    }

    static JSONObject createSkuJson(
            final String productId,
            final IabItemType itemType,
            final String price,
            final String title,
            final String description) {
        final String json = $/|
                              | {
                              |   "productId" : "$productId",
                              |   "type" : "${itemType.getTypeName()}",
                              |   "price" : "$price",
                              |   "title" : "$title",
                              |   "description" :  "$description"
                              | } /$.stripMargin()

        return new JSONObject(json)
    }

    static P2<ProductBaseInfo, SkuDetails> createBaseData(
            final BillingType type,
            final String productId,
            final String price,
            final String title,
            final String description) {

        final JSONObject skuJson = createSkuJson(productId, type.getIabItemType(), price, title, description)
        return P.p(new ProductBaseInfo(productId, type), SkuDetails.create(skuJson))
    }


    static Bundle createGetSkuDetailsResult(
            final IabConstant.GooglePlayResponse resultCode,
            final List<JSONObject> expectedList) {
        final Bundle bundle = new Bundle()
        bundle.putInt("RESPONSE_CODE", resultCode.getCode())

        final List<String> stringList = expectedList.map(new F<JSONObject, String>() {
            @Override
            public String f(JSONObject jsonObject) {
                return jsonObject.toString()
            }
        })
        final ArrayList<String> strings = Java.<String> List_ArrayList().f(stringList)
        bundle.putStringArrayList("DETAILS_LIST", strings)
        return bundle

    }

    static boolean equalBundles(Bundle a, Bundle b) {
        if (a.size() != b.size()) return false
        final String key = "ITEM_ID_LIST"
        if (!a.containsKey(key) || !b.containsKey(key)) return false

        final List<String> aList = List.iterableList(a.getStringArrayList(key)).sort(Ord.stringOrd)
        final List<String> bList = List.iterableList(b.getStringArrayList(key)).sort(Ord.stringOrd)

        return aList.equals(bList)
    }

    /**
     * <pre>
     * {@code
     *
     *{
     *   "orderId":"12999763169054705758.1394237180765537",
     *   "packageName":"jp.daneko.example",
     *   "productId":"consumption_item_001",
     *   "purchaseTime":1413738315217,
     *   "purchaseState":0,
     *   "purchaseToken":"hogefugapiyo"
     *}*
     *}
     * </pre>
     *
     * @see <a href="https://developer.android.com/google/play/billing/billing_reference.html#getPurchases">developer site</a>
     */
    static Purchase createPurchase(
            final String orderId,
            final String packageName,
            final String productId,
            final long purchaseTime,
            final int purchaseState,
            final String purchaseToken,
            final String signature) {

        final String json = $/|
                              |{
                              |  "orderId" : "$orderId" ,
                              |  "packageName" : "$packageName" ,
                              |  "productId" : "$productId" ,
                              |  "purchaseTime" : $purchaseTime ,
                              |  "purchaseState" : $purchaseState ,
                              |  "purchaseToken" : "$purchaseToken"
                              |} /$.stripMargin()

        return Purchase.create(new JSONObject(json), signature)
    }

    def "findBillingItem#購入情報無し"() {
        setup:
        final String packageName = "com.example"

        final P2<ProductBaseInfo, SkuDetails> test1 = createBaseData(BillingType.CONSUMPTION, "test1", '$1.00', "Test 1 title", "test 1 description")
        final P2<ProductBaseInfo, SkuDetails> test2 = createBaseData(BillingType.MANAGE, "test2", '$2.00', "Test 2 title", "test 2 description")
        final P2<ProductBaseInfo, SkuDetails> test3 = createBaseData(BillingType.SUBSCRIPTION, "test3", '$3.00', "Test 3 title", "test 3 description")

        final Bundle inappBundle = new Bundle()
        inappBundle.putStringArrayList("ITEM_ID_LIST", new ArrayList<String>(Arrays.asList(test1._2().getProductId(), test2._2().getProductId())))
        final Bundle inappResultExpectBundle = createGetSkuDetailsResult(IabConstant.GooglePlayResponse.OK, List.list(test1._2().getJson(), test2._2().getJson()))

        final Bundle subsBundle = new Bundle()
        subsBundle.putStringArrayList("ITEM_ID_LIST", new ArrayList<String>(Arrays.asList(test3._2().getProductId())))
        final Bundle subsResultExpectBundle = createGetSkuDetailsResult(IabConstant.GooglePlayResponse.OK, List.list(test3._2().getJson()))

        final IInAppBillingService mockService = Mock(IInAppBillingService)

        1 * mockService.getSkuDetails(3, packageName, IabItemType.INAPP.getTypeName(), { Bundle b -> equalBundles(b, inappBundle) }) >> inappResultExpectBundle
        1 * mockService.getSkuDetails(3, packageName, IabItemType.SUBSCRIPTION.getTypeName(), { Bundle b -> equalBundles(b, subsBundle) }) >> subsResultExpectBundle

        final Bundle purchaseResultExpectBundle = emptyPurchaseResponse()

        1 * mockService.getPurchases(3, packageName, IabItemType.INAPP.getTypeName(), null) >> purchaseResultExpectBundle
        1 * mockService.getPurchases(3, packageName, IabItemType.SUBSCRIPTION.getTypeName(), null) >> purchaseResultExpectBundle

        @SuppressWarnings("unchecked")
        final List<P2<ProductBaseInfo, SkuDetails>> testBaseData = List.list(test1, test2, test3)

        final Set<ProductBaseInfo> idSet = Set.iterableSet(ProductBaseInfo.ord,
                testBaseData.map(new F<P2<ProductBaseInfo, SkuDetails>, ProductBaseInfo>() {
                    @Override
                    public ProductBaseInfo f(P2<ProductBaseInfo, SkuDetails> p2) {
                        return p2._1()
                    }
                })
        )
        final List<Product> target = List.iterableList(
                new IabService.Builder(packageName, null, null, "test", idSet).
                        build().
                        findBillingItem(mockService).toBlocking().toIterable())


        final List<Product> expected = testBaseData.map(new F<P2<ProductBaseInfo, SkuDetails>, Product>() {
            @Override
            public Product f(P2<ProductBaseInfo, SkuDetails> p2) {
                return Product.create(p2._1(), p2._2())
            }
        })

        expect:
        target.toList() == expected.toList()
    }

    def "findBillingItem#購入情報あり"() {
        setup:

        final String packageName = "com.example"

        final P2<ProductBaseInfo, SkuDetails> test1 = createBaseData(BillingType.CONSUMPTION, 'test1', '$1.00', 'Test 1 title', 'test 1 description')
        final P2<ProductBaseInfo, SkuDetails> test2 = createBaseData(BillingType.MANAGE, 'test2', '$2.00', 'Test 2 title', 'test 2 description')
        final P2<ProductBaseInfo, SkuDetails> test3 = createBaseData(BillingType.SUBSCRIPTION, 'test3', '$3.00', 'Test 3 title', 'test 3 description')

        final Bundle inappBundle = new Bundle()
        inappBundle.putStringArrayList("ITEM_ID_LIST", new ArrayList<String>(Arrays.asList(test1._2().getProductId(), test2._2().getProductId())))
        final Bundle inappResultExpectBundle = createGetSkuDetailsResult(IabConstant.GooglePlayResponse.OK, List.list(test1._2().getJson(), test2._2().getJson()))

        final Bundle subsBundle = new Bundle()
        subsBundle.putStringArrayList("ITEM_ID_LIST", new ArrayList<String>(Arrays.asList(test3._2().getProductId())))
        final Bundle subsResultExpectBundle = createGetSkuDetailsResult(IabConstant.GooglePlayResponse.OK, List.list(test3._2().getJson()))

        final IInAppBillingService mockService = Mock(IInAppBillingService)

        1 * mockService.getSkuDetails(3, packageName, IabItemType.INAPP.getTypeName(), { Bundle b -> equalBundles(b, inappBundle) }) >> inappResultExpectBundle
        1 * mockService.getSkuDetails(3, packageName, IabItemType.SUBSCRIPTION.getTypeName(), { Bundle b -> equalBundles(b, subsBundle) }) >> subsResultExpectBundle

        final Purchase purchaseData = createPurchase("12999763169054705758.1394237180765537",
                packageName,
                test1._2().getProductId(),
                1413738315217L,
                0,
                "hogefugapiyo",
                "signature1")
        final Bundle inappPurchaseResponse = createExpectPurchaseResponse(
                IabConstant.GooglePlayResponse.OK,
                List.list(purchaseData),
                null)
        final Bundle subsPurchaseResponse = emptyPurchaseResponse()

        1 * mockService.getPurchases(3, packageName, IabItemType.INAPP.getTypeName(), null) >> inappPurchaseResponse
        1 * mockService.getPurchases(3, packageName, IabItemType.SUBSCRIPTION.getTypeName(), null) >> subsPurchaseResponse

        @SuppressWarnings("unchecked")
        final List<P2<ProductBaseInfo, SkuDetails>> testBaseData = List.list(test1, test2, test3)

        final Set<ProductBaseInfo> idSet = Set.iterableSet(ProductBaseInfo.ord,
                testBaseData.map(new F<P2<ProductBaseInfo, SkuDetails>, ProductBaseInfo>() {
                    @Override
                    public ProductBaseInfo f(P2<ProductBaseInfo, SkuDetails> p2) {
                        return p2._1()
                    }
                })
        )

        final List<Product> target = List.iterableList(
                new IabService.Builder(packageName, null, null, "test", idSet).
                        verifyPurchaseLogic(new F3<String, String, String, Boolean>() {
                            @Override
                            public Boolean f(String s, String s2, String s3) {
                                // ignore verify
                                return true
                            }
                        }).
                        build().
                        findBillingItem(mockService).toBlocking().toIterable())


        final List<Product> expected = testBaseData.map(new F<P2<ProductBaseInfo, SkuDetails>, Product>() {
            @Override
            public Product f(P2<ProductBaseInfo, SkuDetails> p2) {
                final Product product = Product.create(p2._1(), p2._2())
                if (product.getProductId().equals(purchaseData.getProductId())) {
                    return product.withPurchaseInfo(Option.some(purchaseData))
                }
                return product
            }
        })

        expect:
        target.toList() == expected.toList()
    }

    def "findBillingItem#ContinuationTokenあり"() {
        setup:
        final String packageName = "com.example"

        final P2<ProductBaseInfo, SkuDetails> test1 = createBaseData(BillingType.CONSUMPTION, 'test1', '$1.00', 'Test 1 title', 'test 1 description')
        final P2<ProductBaseInfo, SkuDetails> test2 = createBaseData(BillingType.MANAGE, 'test2', '$2.00', 'Test 2 title', 'test 2 description')
        final P2<ProductBaseInfo, SkuDetails> test3 = createBaseData(BillingType.SUBSCRIPTION, 'test3', '$3.00', 'Test 3 title', 'test 3 description')

        final Bundle inappBundle = new Bundle()
        inappBundle.putStringArrayList("ITEM_ID_LIST", new ArrayList<String>(Arrays.asList(test1._2().getProductId(), test2._2().getProductId())))
        final Bundle inappResultExpectBundle = createGetSkuDetailsResult(IabConstant.GooglePlayResponse.OK, List.list(test1._2().getJson(), test2._2().getJson()))

        final Bundle subsBundle = new Bundle()
        subsBundle.putStringArrayList("ITEM_ID_LIST", new ArrayList<String>(Arrays.asList(test3._2().getProductId())))
        final Bundle subsResultExpectBundle = createGetSkuDetailsResult(IabConstant.GooglePlayResponse.OK, List.list(test3._2().getJson()))

        final IInAppBillingService mockService = Mock(IInAppBillingService)

        1 * mockService.getSkuDetails(3, packageName, IabItemType.INAPP.getTypeName(), { Bundle b -> equalBundles(b, inappBundle) }) >> inappResultExpectBundle
        1 * mockService.getSkuDetails(3, packageName, IabItemType.SUBSCRIPTION.getTypeName(), { Bundle b -> equalBundles(b, subsBundle) }) >> subsResultExpectBundle

        final String continuation = "continuation1"

        final Purchase purchaseData1 = createPurchase("12999763169054705758.1394237180765537",
                packageName,
                test1._2().getProductId(),
                1413738315217L,
                0,
                "hogefugapiyo",
                "signature1")

        final Bundle inappPurchaseResponse = createExpectPurchaseResponse(
                IabConstant.GooglePlayResponse.OK,
                List.list(purchaseData1),
                continuation)

        final Purchase purchaseData2 = createPurchase("13000000000000000000.1394237180765537",
                packageName,
                test2._2().getProductId(),
                1413738320000L,
                0,
                "aaaabbbbcccc",
                "signature2")
        final Bundle inappPurchaseResponse2 = createExpectPurchaseResponse(
                IabConstant.GooglePlayResponse.OK,
                List.list(purchaseData2),
                null)
        final Bundle subsPurchaseResponse = emptyPurchaseResponse()

        1 * mockService.getPurchases(3, packageName, IabItemType.INAPP.getTypeName(), null) >> inappPurchaseResponse
        1 * mockService.getPurchases(3, packageName, IabItemType.INAPP.getTypeName(), continuation) >> inappPurchaseResponse2
        1 * mockService.getPurchases(3, packageName, IabItemType.SUBSCRIPTION.getTypeName(), null) >> subsPurchaseResponse

        @SuppressWarnings("unchecked")
        final List<P2<ProductBaseInfo, SkuDetails>> testBaseData = List.list(test1, test2, test3)

        final Set<ProductBaseInfo> idSet = Set.iterableSet(ProductBaseInfo.ord,
                testBaseData.map(new F<P2<ProductBaseInfo, SkuDetails>, ProductBaseInfo>() {
                    @Override
                    public ProductBaseInfo f(P2<ProductBaseInfo, SkuDetails> p2) {
                        return p2._1()
                    }
                })
        )

        final List<Product> target = List.iterableList(
                new IabService.Builder(packageName, null, null, "test", idSet).
                        verifyPurchaseLogic(new F3<String, String, String, Boolean>() {
                            @Override
                            public Boolean f(String s, String s2, String s3) {
                                // ignore verify
                                return true
                            }
                        }).
                        build().
                        findBillingItem(mockService).toBlocking().toIterable())


        final List<Product> expected = testBaseData.map(new F<P2<ProductBaseInfo, SkuDetails>, Product>() {
            @Override
            public Product f(P2<ProductBaseInfo, SkuDetails> p2) {
                final Product product = Product.create(p2._1(), p2._2())
                if (product.getProductId().equals(purchaseData1.getProductId())) {
                    return product.withPurchaseInfo(Option.some(purchaseData1))
                }
                if (product.getProductId().equals(purchaseData2.getProductId())) {
                    return product.withPurchaseInfo(Option.some(purchaseData2))
                }
                return product
            }
        })
        expect:
        target.toList() == expected.toList()
    }
}
