
package com.github.daneko.android.iab.model;

import android.content.Intent;

import fj.data.Option;

import lombok.Value;

/**
 * {@link android.app.Activity#onActivityResult(int, int, android.content.Intent)} のパラメータをまとめたもの
 */
@Value(staticConstructor = "of")
public class ActivityResults {
    int requestCode;
    int resultCode;
    Option<Intent> data;
}
