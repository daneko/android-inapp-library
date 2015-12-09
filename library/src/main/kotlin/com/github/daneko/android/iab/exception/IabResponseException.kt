package com.github.daneko.android.iab.exception

import com.github.daneko.android.iab.model.GooglePlayResponse

/**
 * GooglePlayからの結果Responseを含むException
 * @see [billing response code](https://developer.android.com/google/play/billing/billing_reference.html.billing-codes)
 */
class IabResponseException : Exception {

    val response: GooglePlayResponse

    constructor(response: GooglePlayResponse, detailMessage: String) : super(detailMessage) {
        this.response = response
    }

    constructor(response: GooglePlayResponse, detailMessage: String, throwable: Throwable) : super(detailMessage, throwable) {
        this.response = response
    }

    constructor(response: GooglePlayResponse, throwable: Throwable) : super(throwable) {
        this.response = response
    }
}
