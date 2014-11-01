package com.github.daneko.android.iab;

import android.app.Activity;

import rx.subjects.PublishSubject;

import com.github.daneko.android.iab.model.ActivityResults;

/**
 * 名前はあとで考える
 * そもそもこのInterfaceを強要するかabstract class にするか 引数にするか…
 */
public interface IabContext {
    Activity getActivity();

    PublishSubject<ActivityResults> getPublishSubject();
}
