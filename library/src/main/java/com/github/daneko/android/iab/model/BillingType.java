package com.github.daneko.android.iab.model;


import android.support.annotation.NonNull;

import com.github.daneko.android.iab.IabService;

/**
 */
public enum BillingType {
    MANAGE("manage"),
    CONSUMPTION("consumption"),
    SUBSCRIPTION("subscription");

    private final String description;

    public String getDescription() {
        return description;
    }

    BillingType(final String description) {
        this.description = description;
    }

    public IabItemType getIabItemType() {
        return getIabItemType(this);
    }

    static IabItemType getIabItemType(@NonNull final BillingType type) {
        switch (type) {
            case MANAGE:
            case CONSUMPTION:
                return IabItemType.INAPP;
            case SUBSCRIPTION:
                return IabItemType.SUBSCRIPTION;
        }
        throw new RuntimeException();
    }

}
