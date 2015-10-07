package com.github.daneko.android.iab;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.LruCache;

import android.support.annotation.NonNull;

import com.android.vending.billing.IInAppBillingService;

import fj.F0;
import fj.Unit;
import fj.data.Option;

import rx.Observable;
import rx.subjects.BehaviorSubject;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import com.github.daneko.android.iab.exception.IabException;
import com.github.daneko.android.iab.model.GooglePlayResponse;
import com.github.daneko.android.iab.model.IabItemType;

/**
 */
@Slf4j
class IabServiceConnection implements ServiceConnection {

    private static LruCache<Activity, IabServiceConnection> connectionCache = new LruCache<>(16);

    private static final String IAB_BIND = "com.android.vending.billing.InAppBillingService.BIND";
    private static final String IAB_PACKAGE = "com.android.vending";

    @Setter(value = AccessLevel.PRIVATE)
    private String packageName;

    public static IabServiceConnection getConnection(@NonNull final Activity activity) {
        final F0<IabServiceConnection> connectionFactory = (() -> {
            IabServiceConnection connection = new IabServiceConnection();
            Intent serviceIntent = new Intent(IAB_BIND);
            serviceIntent.setPackage(IAB_PACKAGE);
            if (activity.getPackageManager().queryIntentServices(serviceIntent, 0).isEmpty()) {
                throw new RuntimeException("InAppBillingService has gone ???");
            }

            log.trace("bind from {}", activity);
            activity.bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE);
            connection.setPackageName(activity.getPackageName());
            connectionCache.put(activity, connection);
            return connection;
        });

        log.trace("call get Connection");

        return Option.fromNull(connectionCache.get(activity)).orSome(connectionFactory);
    }

    private final BehaviorSubject<IInAppBillingService> serviceSubject;
    @Getter
    private final Observable<IInAppBillingService> serviceObservable;

    private IabServiceConnection() {
        serviceSubject = BehaviorSubject.create();
        serviceObservable = serviceSubject.asObservable();
    }

    /**
     * RemoteExceptionとか言われたら、{@link com.android.vending.billing.IInAppBillingService} に書いてあることが信用できなくなるので無視
     * v3 inapp & 月額課金対応かはチェックする
     */
    @SneakyThrows(RemoteException.class)
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        log.trace("service connected.");

        final IInAppBillingService s = IInAppBillingService.Stub.asInterface(service);

        final GooglePlayResponse inappSupported = GooglePlayResponse.create(
                s.isBillingSupported(IabConstant.TARGET_VERSION, packageName, IabItemType.INAPP.getTypeName()));
        final GooglePlayResponse subsSupported = GooglePlayResponse.create(
                s.isBillingSupported(IabConstant.TARGET_VERSION, packageName, IabItemType.SUBSCRIPTION.getTypeName()));

        if (inappSupported != GooglePlayResponse.OK || subsSupported != GooglePlayResponse.OK) {
            final String err = String.format("unsupported v3 api. inapp response %s / subscription response %s",
                    inappSupported.getDescription(), subsSupported.getDescription());
            log.error(err);
            serviceSubject.onError(new IabException(err));
            return;
        }

        serviceSubject.onNext(s);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        log.trace("service disconnected.");
        serviceSubject.onCompleted();
    }

    public static Unit dispose(final Activity activity) {
        Option.fromNull(connectionCache.remove(activity)).map(c -> {
            log.trace("unbind from {}", activity);
            activity.unbindService(c);
            return Unit.unit();
        });
        return Unit.unit();
    }
}
