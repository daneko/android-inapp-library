package com.github.daneko.android.iab.model;

/**
 * <a href="https://developer.android.com/google/play/billing/billing_reference.html#billing-codes">参照</a>
 */
public enum GooglePlayResponse {
    OK(0, "OK"),
    USER_CANCELED(1, "User Canceled"),
    BILLING_UNAVAILABLE(3, "Billing Unavailable"),
    ITEM_UNAVAILABLE(4, "Item Unavailable"),
    DEVELOPER_ERROR(5, "Developer Error"),
    ERROR(6, "Error"),
    ITEM_ALREADY_OWNED(7, "Item Already Owned"),
    ITEM_NOT_OWNED(8, "Item not owned");

    private final int code;
    private final String description;

    public int getCode() {
        return this.code;
    }

    public String getDescription() {
        return this.description;
    }

    GooglePlayResponse(final int code, final String description) {
        this.code = code;
        this.description = description;
    }

    public static GooglePlayResponse create(int value) {
        for (GooglePlayResponse response : GooglePlayResponse.values()) {
            if (value == response.getCode()) {
                return response;
            }
        }
        throw new IllegalArgumentException(String.format("unknown response code: %d", value));
    }

}
