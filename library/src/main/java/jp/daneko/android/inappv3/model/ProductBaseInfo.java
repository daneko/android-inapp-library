package jp.daneko.android.inappv3.model;

import fj.Ord;
import fj.Ordering;

import lombok.NonNull;
import lombok.Value;

/**
 */
@Value
public class ProductBaseInfo implements Comparable<ProductBaseInfo> {
    @NonNull
    String id;
    @NonNull
    BillingType type;

    @Override
    public int compareTo(ProductBaseInfo another) {
        final int x = type.compareTo(another.getType());
        return x == 0 ? id.compareTo(another.getId()) : x;
    }

    public static Ord<ProductBaseInfo> ord = Ord.<ProductBaseInfo>ord(
            a1 -> a2 -> {
                final int x = a1.compareTo(a2);
                return x < 0 ? Ordering.LT : x == 0 ? Ordering.EQ : Ordering.GT;
            });
}
