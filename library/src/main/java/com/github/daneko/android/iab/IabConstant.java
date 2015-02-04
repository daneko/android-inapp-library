package com.github.daneko.android.iab;

import android.os.Bundle;

import javax.annotation.Nonnull;

import fj.data.List;

import lombok.extern.slf4j.Slf4j;

import com.github.daneko.android.iab.model.GooglePlayResponse;

/**
 */
@Slf4j
class IabConstant {
    public static final int TARGET_VERSION = 3;

    public static GooglePlayResponse extractResponse(@Nonnull final Bundle bundle) {
        Object o = bundle.get(IabConstant.BillingServiceConstants.RESPONSE_CODE.getValue());
        if (o == null) {
            log.debug("Bundle with null response code, assuming OK (known issue)");
            return GooglePlayResponse.OK;
        } else if (o instanceof Integer) {
            return GooglePlayResponse.create(((Integer) o));
        } else if (o instanceof Long) {
            return GooglePlayResponse.create((int) ((Long) o).longValue());
        } else {
            final String err = "Unexpected type for bundle response code: " + o.getClass().getName();
            log.error(err);
            throw new RuntimeException(err);
        }
    }

    public enum BillingServiceConstants {
        RESPONSE_CODE("RESPONSE_CODE"),
        GET_SKU_DETAILS_LIST("DETAILS_LIST"),
        BUY_INTENT("BUY_INTENT"),
        INAPP_PURCHASE_DATA("INAPP_PURCHASE_DATA"),
        INAPP_SIGNATURE("INAPP_DATA_SIGNATURE"),
        INAPP_ITEM_LIST("INAPP_PURCHASE_ITEM_LIST"),
        INAPP_PURCHASE_DATA_LIST("INAPP_PURCHASE_DATA_LIST"),
        INAPP_SIGNATURE_LIST("INAPP_DATA_SIGNATURE_LIST"),
        INAPP_CONTINUATION_TOKEN("INAPP_CONTINUATION_TOKEN");

        private final String value;

        public String getValue() {
            return this.value;
        }

        BillingServiceConstants(final String value) {
            this.value = value;
        }

        public static boolean hasPurchaseKey(@Nonnull Bundle bundle) {

            return List.list(INAPP_ITEM_LIST, INAPP_PURCHASE_DATA_LIST, INAPP_SIGNATURE_LIST).
                    forall(value ->
                                    bundle.containsKey(value.getValue())
                    );
        }

    }

    /**
     *
     */
    public enum GetSkuDetailKey {
        ITEM_LIST("ITEM_ID_LIST"),
        ITEM_TYPE_LIST("ITEM_TYPE_LIST");

        private final String value;

        public String getValue() {
            return this.value;
        }

        GetSkuDetailKey(final String value) {
            this.value = value;
        }
    }

}
