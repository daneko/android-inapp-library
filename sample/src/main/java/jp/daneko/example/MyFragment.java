package jp.daneko.example;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

import fj.function.TryEffect2;

import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

import lombok.extern.slf4j.Slf4j;

import com.github.daneko.android.iab.IabContext;
import com.github.daneko.android.iab.IabService;
import com.github.daneko.android.iab.model.Product;

/**
 * A placeholder fragment containing a simple view.
 */
@Slf4j
public class MyFragment extends Fragment {

    @Bind(R.id.item_list)
    ListView listView;

    public MyFragment() {
    }

    IabContext context;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (!(context instanceof IabContext)) {
            throw new RuntimeException();
        }
        this.context = (IabContext) context;
    }

    // verifyPurchaseLogic((s1, s2, e) -> true). // verify off, if try buy android.test.purchase.
    private static final TryEffect2<String, String, Exception> verificationLogic
            = IabService.defaultVerificationLogic(BuildConfig.PUBLIC_KEY);

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_my, container, false);
        ButterKnife.bind(this, rootView);

        listView.setAdapter(new BillingItemsArrayAdapter(getActivity()));
        listView.setOnItemClickListener((adapter, view, pos, id) -> {
            final Product item = (Product) adapter.getItemAtPosition(pos);
            if (item.canBuy()) {
                final int requestCode = 1234;
                IabService.buyProduct(context, item, requestCode, verificationLogic)
                        .subscribeOn(Schedulers.newThread())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                next -> {
                                    log.debug("onNext purchase success {}", next);
                                    toast("purchase success");
                                },
                                e -> {
                                    log.error("onError\n", e);
                                    toast(e.getMessage());
                                },
                                () -> log.debug("onCompleted purchase")
                        );
            } else if (item.canConsume()) {
                IabService.consumeProduct(getActivity(), item)
                        .subscribeOn(Schedulers.newThread())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                next -> {
                                    log.debug("onNext consume success");
                                    toast("consume success");
                                },
                                e -> {
                                    log.error("onError\n", e);
                                    toast(e.getMessage());
                                },
                                () -> log.debug("onCompleted consume")
                        );
            }

        });

        return rootView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.unbind(this);
    }

    @OnClick(R.id.button1)
    public void b1Click(Button button) {
        ((BillingItemsArrayAdapter) listView.getAdapter()).clear();
        IabService.fetchProduct(getActivity(),
                verificationLogic,
                MyProducts.getAllItemData())
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        next -> {
                            log.debug("onNext");
                            ((BillingItemsArrayAdapter) listView.getAdapter()).add(next);
                        },
                        e -> {
                            log.error("onError\n", e);
                            toast(e.getMessage());
                        },
                        () -> log.debug("onCompleted")
                );

    }

    @OnClick(R.id.button2)
    public void b2Click(Button button) {
    }

    private void toast(@NonNull final String msg) {
        log.debug("toast {}", msg);
        Toast.makeText(getActivity(), msg, Toast.LENGTH_LONG).show();
    }
}
