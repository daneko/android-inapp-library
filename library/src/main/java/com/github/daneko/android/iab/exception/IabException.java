package com.github.daneko.android.iab.exception;

/**
 * 事実上Runtimeなのでは感
 */
public class IabException extends Exception {
    public IabException(String detailMessage) {
        super(detailMessage);
    }
}
