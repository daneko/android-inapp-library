package com.github.daneko.android.iab

import com.github.daneko.android.iab.exception.IabResponseException
import com.github.daneko.android.iab.model.GooglePlayResponse
import fj.data.Validation
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import rx.observers.TestSubscriber


/**
 *
 */
class UtilTest {

    @Test
    fun GooglePlayResponseがOKの場合はそのまま返ってくる() {
        val obs = checkGooglePlayResponse(GooglePlayResponse.OK)
        val subs = TestSubscriber.create<GooglePlayResponse>()
        obs.subscribe(subs)

        subs.assertNoErrors()
        subs.assertCompleted()

        assertThat(subs.onNextEvents[0]).isEqualTo(GooglePlayResponse.OK)
    }

    @Test
    fun GooglePlayResponseがOKではない場合IabResponseException() {
        val obs = checkGooglePlayResponse(GooglePlayResponse.DEVELOPER_ERROR)
        val subs = TestSubscriber.create<GooglePlayResponse>()
        obs.subscribe(subs)

        subs.assertError(IabResponseException::class.java)

        assertThat((subs.onErrorEvents[0] as IabResponseException).response).
                isEqualTo(GooglePlayResponse.DEVELOPER_ERROR)
    }

    @Test
    fun fromValidationOnSuccess() {
        val obs = fromValidation(Validation.success<Throwable, String>("test"))
        val subs = TestSubscriber.create<String>()
        obs.subscribe(subs)

        subs.assertNoErrors()
        subs.assertCompleted()

        assertThat(subs.onNextEvents[0]).isEqualTo("test")
    }

    @Test
    fun fromValidationOnError() {
        val obs = fromValidation(Validation.fail<Exception, String>(Exception("test")))
        val subs = TestSubscriber.create<String>()
        obs.subscribe(subs)

        subs.assertError(Exception::class.java)

        assertThat(subs.onErrorEvents[0]).
                isInstanceOf(Exception::class.java).
                hasMessage("test")
    }

}