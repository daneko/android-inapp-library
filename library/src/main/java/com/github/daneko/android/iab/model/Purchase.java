package com.github.daneko.android.iab.model;


import org.json.JSONObject;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Value;

/**
 * base on com.example.android.trivialdrivesample.util.Purchase
 */
@Value
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(exclude = {"originalJson"})
public class Purchase {
    String orderId;
    String packageName;
    String sku;
    long purchaseTime;
    int purchaseState;
    String developerPayload;
    String token;
    JSONObject originalJson;
    String signature;

    public static Purchase create(final JSONObject jsonPurchaseInfo, final String signature) {
        return new Purchase(
                jsonPurchaseInfo.optString("orderId"),
                jsonPurchaseInfo.optString("packageName"),
                jsonPurchaseInfo.optString("productId"),
                jsonPurchaseInfo.optLong("purchaseTime"),
                jsonPurchaseInfo.optInt("purchaseState"),
                jsonPurchaseInfo.optString("developerPayload"),
                jsonPurchaseInfo.optString("token", jsonPurchaseInfo.optString("purchaseToken")),
                jsonPurchaseInfo,
                signature
        );
    }

    /**
     * alias {@link #getSku()}
     */
    public String getProductId() {
        return sku;
    }
}
