package com.mycelium.wallet.activity.send.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.mrd.bitlib.util.CoinUtil;
import com.mycelium.wallet.R;
import com.mycelium.wallet.activity.send.model.FeeItem;
import com.mycelium.wallet.activity.send.view.SelectableRecyclerView;

import java.math.BigDecimal;

import static com.mrd.bitlib.util.CoinUtil.Denomination.mBTC;

/**
 * Created by elvis on 31.08.17.
 */

public class FeeViewAdapter extends SelectableRecyclerView.Adapter<FeeViewAdapter.ViewHolder> {

    private FeeItem[] mDataset;
    public static final int VIEW_TYPE_ITEM = 2;
    private int paddingWidth = 0;

    public FeeViewAdapter(int paddingWidth) {
        this.paddingWidth = paddingWidth;
        mDataset = new FeeItem[0];
    }

    public void setDataset(FeeItem[] mDataset) {
        this.mDataset = mDataset;
        notifyDataSetChanged();
    }

    public FeeItem getItem(int position) {
        return mDataset[position];
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_ITEM) {
            // create a new view
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.recyclerview_item_fee_lvl, parent, false);
            ImageView imageView = (ImageView) v.findViewById(R.id.rectangle);
            imageView.setImageResource(R.drawable.recyclerview_item_top_rectangle_selector);
            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) imageView.getLayoutParams();
            layoutParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
            layoutParams.height = parent.getResources().getDimensionPixelSize(R.dimen.recycler_item_triangle_height);
            imageView.setLayoutParams(layoutParams);
            // set the view's size, margins, paddings and layout parameters
            //...
            return new FeeViewAdapter.ViewHolder(v, this);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_padding_sender,
                    parent, false);

            RecyclerView.LayoutParams layoutParams = (RecyclerView.LayoutParams) view.getLayoutParams();
            layoutParams.width = paddingWidth;
            view.setLayoutParams(layoutParams);
            return new FeeViewAdapter.ViewHolder(view, this);
        }
    }


    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        if (getItemViewType(position) == VIEW_TYPE_ITEM) {
            // - get element from your dataset at this position
            // - replace the contents of the view with that element
            FeeItem item = mDataset[position];
            if (item.btc != null) {
                holder.categoryTextView.setText(CoinUtil.valueString(item.btc.getLongValue(), mBTC, true) + " " + mBTC.getUnicodeName());
            }
            if (item.currencyValue != null && item.currencyValue.getValue() != null) {
                holder.itemTextView.setText("~" + item.currencyValue.getValue().setScale(2, BigDecimal.ROUND_HALF_DOWN)
                        + " " + item.currencyValue.getCurrency());
            }
            holder.valueTextView.setText(String.valueOf(item.feePerKb / 1000) + " sat/byte");

        }
    }

    @Override
    public int getItemCount() {
        return mDataset.length;
    }

    @Override
    public int getItemViewType(int position) {
        return mDataset[position].type;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder{
        // each data item is just a string in this case
        public TextView categoryTextView;
        public TextView itemTextView;
        public TextView valueTextView;
        RecyclerView.Adapter adapter;

        public ViewHolder(View v, FeeViewAdapter adapter) {
            super(v);
            categoryTextView = (TextView) v.findViewById(R.id.categorytextView);
            itemTextView = (TextView) v.findViewById(R.id.itemTextView);
            valueTextView = (TextView) v.findViewById(R.id.valueTextView);
            this.adapter = adapter;
        }
    }
}
