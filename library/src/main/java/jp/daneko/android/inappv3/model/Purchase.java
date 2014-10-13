package jp.daneko.android.inappv3.model;

import org.json.JSONObject;

import javax.annotation.Nonnull;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Value;

/**
 * <a href="https://developer.android.com/google/play/billing/billing_overview.html#console">ここに</a>skuってこんなのって言っているけど
 * productIdでいいんじゃないか感
 * <p>
 * 購入情報
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

    public static Purchase create(@Nonnull JSONObject jsonPurchaseInfo, @Nonnull String signature) {
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
