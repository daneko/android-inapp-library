package jp.daneko.example;

import fj.data.Set;

import com.github.daneko.android.iab.model.BillingType;
import com.github.daneko.android.iab.model.ProductBaseInfo;

/**
 */
public class MyProducts {
    public static Set<ProductBaseInfo> getAllItemData() {
        return allItemData;
    }

    private final static Set<ProductBaseInfo> allItemData =
            Set.set(ProductBaseInfo.ord,
//                    new ProductBaseInfo("android.test.purchased", BillingType.CONSUMPTION),
                    new ProductBaseInfo("consumption_item_001", BillingType.CONSUMPTION),
                    new ProductBaseInfo("consumption_item_002", BillingType.CONSUMPTION),
                    new ProductBaseInfo("sample_item_001", BillingType.MANAGE),
                    new ProductBaseInfo("sample_item_002", BillingType.MANAGE),
                    new ProductBaseInfo("subscription_sample_001", BillingType.SUBSCRIPTION)
            );
}
