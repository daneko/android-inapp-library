package com.github.daneko.android.iab.exception;

import lombok.Getter;

import com.github.daneko.android.iab.model.GooglePlayResponse;

/**
 * GooglePlayからの結果Responseを含むException
 * @see <a href="https://developer.android.com/google/play/billing/billing_reference.html#billing-codes">billing response code</a>
 */
public class IabResponseException extends Exception {

    @Getter
    private final GooglePlayResponse response;

    public IabResponseException(final GooglePlayResponse response, final String detailMessage) {
        super(detailMessage);
        this.response = response;
    }
}
