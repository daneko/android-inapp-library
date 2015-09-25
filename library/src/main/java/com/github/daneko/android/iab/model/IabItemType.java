package com.github.daneko.android.iab.model;

import android.support.annotation.NonNull;

public enum IabItemType {
    INAPP("inapp"),
    SUBSCRIPTION("subs");

    private final String typeName;

    public String getTypeName() {
        return this.typeName;
    }

    IabItemType(@NonNull final String typeName) {
        this.typeName = typeName;
    }
}
