package com.github.daneko.android.iab.model

import com.github.daneko.android.iab.exception.IabException
import fj.F
import fj.Try
import fj.TryEffect
import fj.data.Validation
import fj.function.Try1
import fj.function.TryEffect2
import org.json.JSONException
import org.json.JSONObject

/**
 * @see <a href="http://developer.android.com/intl/ja/google/play/billing/billing_reference.html#purchase-data-table">purchase json attribute</a>
 *
 * ```
managed product, purchase response json example
{
  "orderId":"GPA.1376-8964-0955-78427",
  "packageName":"jp.daneko.example",
  "productId":"sample_item_002",
  "purchaseTime":1449642721163,
  "purchaseState":0,
  "purchaseToken":"token string"
}

subscription product, purchase response json example
(where is orderId ?)
{
  "packageName":"jp.daneko.example",
  "productId":"subscription_sample_001",
  "purchaseTime":1449642809688,
  "purchaseState":0,
  "purchaseToken":"token string",
  "autoRenewing":true
}

 * ```
 */
data class Purchase protected constructor(
        val orderId: String,
        val packageName: String,
        val sku: String,
        val purchaseTime: Long = 0,
        val purchaseState: Int = 0,
        val developerPayload: String,
        val token: String,
        val autRenewing: Boolean,
        val originalJson: JSONObject,
        val signature: String
) {
    companion object {

        // how to write "create(logic):String -> String -> V<E,P>" ?
        fun <E : Exception> create(verificationLogic: TryEffect2<String, String, E>):
                (String, String) -> Validation<IabException, Purchase> =
                { purchaseData, signature ->
                    if (purchaseData.isNullOrEmpty() || signature.isNullOrEmpty()) {
                        val message = """purchaseData or dataSignature is null or empty.
  purchase $purchaseData
  dataSignature $signature"""
                        Validation.fail(IabException(message))
                    } else {
                        TryEffect.f(verificationLogic).f(purchaseData, signature).
                                f().
                                map(F<E, IabException> { e ->
                                    when (e) {
                                        is IabException -> e
                                        else -> IabException("verification error", e)
                                    }
                                }).
                                bind {
                                    Try.f(Try1<String, JSONObject, JSONException> { json -> JSONObject(json) }).
                                            f(purchaseData).f().
                                            map(F<JSONException, IabException> {
                                                IabException("json exception: $purchaseData", it)
                                            })
                                }.
                                map { json -> create(json, signature) }.
                                bind { purchase: Purchase ->
                                    Validation.condition(
                                            purchase.token.isNotEmpty(),
                                            IabException("""BUG: empty/null token! Purchase data: $purchaseData """),
                                            purchase
                                    )
                                }
                    }
                }

        private fun create(jsonPurchaseInfo: JSONObject, signature: String): Purchase {
            return Purchase(
                    jsonPurchaseInfo.optString("orderId"),
                    jsonPurchaseInfo.optString("packageName"),
                    jsonPurchaseInfo.optString("productId"),
                    jsonPurchaseInfo.optLong("purchaseTime"),
                    jsonPurchaseInfo.optInt("purchaseState"),
                    jsonPurchaseInfo.optString("developerPayload"),
                    jsonPurchaseInfo.optString("token", jsonPurchaseInfo.optString("purchaseToken")),
                    jsonPurchaseInfo.optBoolean("autoRenewing", false),
                    jsonPurchaseInfo,
                    signature)
        }
    }

    /**
     * alias [.getSku]
     */
    fun getProductId(): String = sku
}
