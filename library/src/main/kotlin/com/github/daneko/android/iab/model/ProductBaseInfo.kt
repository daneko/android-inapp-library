package com.github.daneko.android.iab.model

import fj.F2Functions
import fj.Ord
import fj.Ordering


/**
 */
class ProductBaseInfo(internal val id: String, internal val type: BillingType) : Comparable<ProductBaseInfo> {

    override fun compareTo(other: ProductBaseInfo): Int {
        val x = type.compareTo(other.type)
        return if (x == 0) id.compareTo(other.id) else x
    }

    companion object {
        @JvmStatic
        val ord: Ord<ProductBaseInfo> = Ord.ord(F2Functions.curry { a: ProductBaseInfo, b: ProductBaseInfo ->
            val x = a.compareTo(b)
            if (x < 0) Ordering.LT else if (x == 0) Ordering.EQ else Ordering.GT
        })
    }
}
