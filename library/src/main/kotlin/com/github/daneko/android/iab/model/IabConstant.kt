package com.github.daneko.android.iab

import android.os.Bundle
import com.github.daneko.android.iab.model.GooglePlayResponse
import fj.data.List
import org.slf4j.LoggerFactory

/**
 */
internal object IabConstant {
    val log = LoggerFactory.getLogger(IabConstant.javaClass)
    val TARGET_VERSION = 5

    /**
    TODO: rewrite when
     */
    fun extractResponse(bundle: Bundle): GooglePlayResponse {
        val o = bundle.get(IabConstant.BillingServiceConstants.RESPONSE_CODE.value)
        if (o == null) {
            log.debug("Bundle with null response code, assuming OK (known issue)")
            return GooglePlayResponse.OK
        } else if (o is Int) {
            return GooglePlayResponse.create(o)
        } else if (o is Long) {
            return GooglePlayResponse.create((o).toLong().toInt())
        } else {
            val err = "Unexpected type for bundle response code: " + o.javaClass.name
            log.error(err)
            throw RuntimeException(err)
        }
    }

    enum class BillingServiceConstants internal constructor(val value: String) {
        RESPONSE_CODE("RESPONSE_CODE"),
        GET_SKU_DETAILS_LIST("DETAILS_LIST"),
        BUY_INTENT("BUY_INTENT"),
        INAPP_PURCHASE_DATA("INAPP_PURCHASE_DATA"),
        INAPP_SIGNATURE("INAPP_DATA_SIGNATURE"),
        INAPP_ITEM_LIST("INAPP_PURCHASE_ITEM_LIST"),
        INAPP_PURCHASE_DATA_LIST("INAPP_PURCHASE_DATA_LIST"),
        INAPP_SIGNATURE_LIST("INAPP_DATA_SIGNATURE_LIST"),
        INAPP_CONTINUATION_TOKEN("INAPP_CONTINUATION_TOKEN");

        companion object {
            fun hasPurchaseKey(bundle: Bundle): Boolean {
                return List.list(INAPP_ITEM_LIST, INAPP_PURCHASE_DATA_LIST, INAPP_SIGNATURE_LIST).
                        forall { value -> bundle.containsKey(value.value) }
            }
        }
    }

    /**

     */
    enum class GetSkuDetailKey internal constructor(val value: String) {
        ITEM_LIST("ITEM_ID_LIST"),
        ITEM_TYPE_LIST("ITEM_TYPE_LIST")
    }
}
