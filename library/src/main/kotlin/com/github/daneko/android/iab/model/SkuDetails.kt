package com.github.daneko.android.iab.model

import org.json.JSONObject

/**
 * Represents an in-app product's listing details.
 */
data class SkuDetails private constructor(
        internal val sku: String,
        val type: String,
        val price: String,
        val title: String,
        val description: String,
        val json: JSONObject
) {
    /**
     * alias [.getSku]
     */
    fun getProductId() = sku

    companion object {
        @JvmStatic
        fun create(jsonSkuDetails: JSONObject): SkuDetails {
            return SkuDetails(
                    jsonSkuDetails.optString("productId"),
                    jsonSkuDetails.optString("type"),
                    jsonSkuDetails.optString("price"),
                    jsonSkuDetails.optString("title"),
                    jsonSkuDetails.optString("description"),
                    jsonSkuDetails)
        }
    }
}
