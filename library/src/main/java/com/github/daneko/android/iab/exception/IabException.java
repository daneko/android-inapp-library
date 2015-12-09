package com.github.daneko.android.iab.exception;

/**
 */
public class IabException extends Exception {
    public IabException(String detailMessage) {
        super(detailMessage);
    }

    public IabException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public IabException(Throwable throwable) {
        super(throwable);
    }
}
