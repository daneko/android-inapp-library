package jp.daneko.example;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import javax.annotation.Nonnull;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

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

    @InjectView(R.id.item_list)
    ListView listView;

    IabService iabService;

    public MyFragment() {
    }

    IabContext context;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (!(activity instanceof IabContext)) {
            throw new RuntimeException();
        }
        context = (IabContext) activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_my, container, false);

        iabService = IabService.builder(context, BuildConfig.PUBLIC_KEY, MyProducts.getAllItemData()).
//                verifyPurchaseLogic((s1, s2, s3) -> true). // verify off, if try buy android.test.purchase.
        build();
        ButterKnife.inject(this, rootView);
        listView.setAdapter(new BillingItemsArrayAdapter(getActivity()));
        listView.setOnItemClickListener((adapter, view, pos, id) -> {
            final Product item = (Product) adapter.getItemAtPosition(pos);
            if (item.canBuy()) {
                iabService.
                        buyItem(item, 12345 + pos).
                        subscribeOn(Schedulers.newThread()).
                        observeOn(AndroidSchedulers.mainThread()).
                        subscribe(
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
                iabService.
                        consumeItem(item).
                        subscribeOn(Schedulers.newThread()).
                        observeOn(AndroidSchedulers.mainThread()).
                        subscribe(
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
        iabService.dispose();
        ButterKnife.reset(this);
    }

    @OnClick(R.id.button1)
    public void b1Click(Button button) {
        ((BillingItemsArrayAdapter) listView.getAdapter()).clear();
        iabService.
                findBillingItem().
                subscribeOn(Schedulers.newThread()).
                observeOn(AndroidSchedulers.mainThread()).
                subscribe(
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

    private void toast(@Nonnull final String msg) {
        log.debug("toast {}", msg);
        Toast.makeText(getActivity(), msg, Toast.LENGTH_LONG).show();
    }
}
