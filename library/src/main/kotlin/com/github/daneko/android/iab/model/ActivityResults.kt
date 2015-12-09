package com.github.daneko.android.iab.model

import android.content.Intent
import fj.data.Option


/**
 * [android.app.Activity.onActivityResult] のパラメータをまとめたもの
 */
data class ActivityResults(
        val requestCode: Int,
        val resultCode: Int,
        val data: Option<Intent> = Option.none()) {

    companion object {
        @JvmStatic @JvmOverloads
        fun of(requestCode: Int, resultCode: Int, data: Option<Intent> = Option.none()):ActivityResults {
            return ActivityResults(requestCode, resultCode, data)
        }
    }
}
