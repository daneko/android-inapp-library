package com.github.daneko.android.iab

import com.github.daneko.android.iab.exception.IabResponseException
import com.github.daneko.android.iab.model.GooglePlayResponse
import fj.data.Validation
import rx.Observable

internal fun <E : Throwable, T> fromValidation(valid: Validation<E, T>): Observable<T> =
        Observable.create<T> { subs ->
            valid.validation(
                    { e -> subs.onError(e) },
                    { s ->
                        subs.onNext(s)
                        subs.onCompleted()
                    }
            )
        }

internal fun checkGooglePlayResponse(
        response: GooglePlayResponse,
        cause: Exception = Exception()): Observable<GooglePlayResponse> =
        if (!response.isSuccess) {
            val message = """not success response
response: code:${response.code} / desc:${response.description}
response Bundle:  $response
"""
            Observable.error<GooglePlayResponse>(IabResponseException(response, message, cause))
        } else {
            Observable.just(response)
        }
