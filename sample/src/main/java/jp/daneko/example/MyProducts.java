package jp.daneko.example;

import java.util.List;

import com.github.daneko.android.iab.model.BillingType;
import com.github.daneko.android.iab.model.ProductBaseInfo;

/**
 */
public class MyProducts {
    public static List<ProductBaseInfo> getAllItemData() {
        return fj.data.List.list(
//                    new ProductBaseInfo("android.test.purchased", BillingType.CONSUMPTION),
                new ProductBaseInfo("consumption_item_001", BillingType.CONSUMPTION),
                new ProductBaseInfo("consumption_item_002", BillingType.CONSUMPTION),
                new ProductBaseInfo("sample_item_001", BillingType.MANAGE),
                new ProductBaseInfo("sample_item_002", BillingType.MANAGE),
                new ProductBaseInfo("subscription_sample_001", BillingType.SUBSCRIPTION)
        ).toJavaList();
    }
}
