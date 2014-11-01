package com.github.daneko.android.iab.model;

import javax.annotation.Nonnull;

public enum IabItemType {
    INAPP("inapp"),
    SUBSCRIPTION("subs");

    private final String typeName;

    public String getTypeName() {
        return this.typeName;
    }

    IabItemType(@Nonnull final String typeName) {
        this.typeName = typeName;
    }
}
