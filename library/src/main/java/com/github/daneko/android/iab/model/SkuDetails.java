package com.github.daneko.android.iab.model;

import org.json.JSONObject;

import javax.annotation.Nonnull;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Value;

/**
 * Represents an in-app product's listing details.
 */
@Value
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(exclude = {"json"})
public class SkuDetails {
    String sku;
    String type;
    String price;
    String title;
    String description;
    JSONObject json;

    public static SkuDetails create(@Nonnull final JSONObject jsonSkuDetails) {

        return new SkuDetails(
                jsonSkuDetails.optString("productId"),
                jsonSkuDetails.optString("type"),
                jsonSkuDetails.optString("price"),
                jsonSkuDetails.optString("title"),
                jsonSkuDetails.optString("description"),
                jsonSkuDetails
        );
    }

    /**
     * alias {@link #getSku()}
     */
    public String getProductId() {
        return sku;
    }

}
