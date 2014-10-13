package jp.daneko.android.inappv3;

import android.app.Activity;

import rx.subjects.PublishSubject;

import jp.daneko.android.inappv3.model.ActivityResults;

/**
 * 名前はあとで考える
 * そもそもこのInterfaceを強要するかabstract class にするか 引数にするか…
 */
public interface IabContext {
    Activity getActivity();

    PublishSubject<ActivityResults> getPublishSubject();
}
