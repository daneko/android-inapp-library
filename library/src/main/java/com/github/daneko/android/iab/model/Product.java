package com.github.daneko.android.iab.model;

import fj.data.Option;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.Wither;

/**
 *
 */
@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@ToString(of = {"productBaseInfo", "skuDetails"})
public class Product {
    ProductBaseInfo productBaseInfo;
    SkuDetails skuDetails;

    @Wither
    Option<Purchase> purchaseInfo;

    @Wither
    Option<String> developerPayload;

    public static Product create(final ProductBaseInfo productInfo, final SkuDetails skuDetails) {
        return new Product(productInfo, skuDetails, Option.none(), Option.none());
    }

    @Getter(lazy = true)
    private final BillingType billingType = productBaseInfo.getType();

    @Getter(lazy = true)
    private final String productId = skuDetails.getProductId();

    @Getter(lazy = true)
    private final IabItemType iabItemType = getBillingType().getIabItemType();

    public boolean canBuy() {
        return purchaseInfo.isNone();
    }

    public boolean canConsume() {
        return getBillingType() == BillingType.CONSUMPTION && purchaseInfo.isSome();
    }
}

