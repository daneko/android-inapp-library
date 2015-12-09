package com.github.daneko.android.iab.model

import fj.data.Option

/**
 *
 */

data class Product(
        val productBaseInfo: ProductBaseInfo,
        val skuDetails: SkuDetails,
        val purchaseInfo: Option<Purchase>,
        val developerPayload: Option<String>
) {
    companion object {
        @JvmStatic
        fun create(productBaseInfo: ProductBaseInfo, skuDetails: SkuDetails): Product {
            return Product(productBaseInfo, skuDetails, Option.none(), Option.none())
        }

    }

    val billingType: BillingType by lazy { productBaseInfo.type }

    val productId: String by lazy { skuDetails.getProductId() }

    val IabItemType: IabItemType by lazy { billingType.iabItemType }

    fun canBuy(): Boolean {
        return purchaseInfo.isNone
    }

    fun canConsume(): Boolean {
        return billingType == BillingType.CONSUMPTION && purchaseInfo.isSome
    }
}
