package jp.daneko.example;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import javax.annotation.Nonnull;

import butterknife.ButterKnife;
import butterknife.InjectView;

import fj.F;
import fj.Unit;

import jp.daneko.android.inappv3.model.Product;
import jp.daneko.android.inappv3.model.SkuDetails;

/**
 */
public class BillingItemsArrayAdapter extends ArrayAdapter<Product> {
    private final F<Unit, View> inflate;

    public BillingItemsArrayAdapter(Context context) {
        super(context, R.layout.sku_item);
        final LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflate = (Unit -> inflater.inflate(R.layout.sku_item, null));
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder holder;
        if (convertView == null) {
            convertView = inflate.f(Unit.unit());
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.setupView(getItem(position));

        return convertView;
    }

    static class ViewHolder {
        @InjectView(R.id.type_txt)
        TextView typeView;
        @InjectView(R.id.price_txt)
        TextView priceView;
        @InjectView(R.id.title_txt)
        TextView titleView;
        @InjectView(R.id.desc_txt)
        TextView descriptionView;
        @InjectView(R.id.buy_txt)
        TextView canPurchaseView;

        ViewHolder(View view) {
            ButterKnife.inject(this, view);
        }

        void setupView(@Nonnull final Product product) {
            final SkuDetails item = product.getSkuDetails();
            typeView.setText(product.getBillingType().getDescription());
            priceView.setText(item.getPrice());
            titleView.setText(item.getTitle());
            descriptionView.setText(item.getDescription());
            canPurchaseView.setText(String.format("%b", product.getPurchaseInfo().isSome()));
        }
    }
}
