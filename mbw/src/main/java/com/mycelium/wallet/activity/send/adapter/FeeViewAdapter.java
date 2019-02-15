package com.mycelium.wallet.activity.send.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.mycelium.wallet.R;
import com.mycelium.wallet.activity.send.model.FeeItem;
import com.mycelium.wallet.activity.send.view.SelectableRecyclerView;
import com.mycelium.wallet.activity.util.ValueExtensionsKt;

import java.util.Collections;
import java.util.List;

import static com.mrd.bitlib.util.CoinUtil.Denomination.mBTC;

public class FeeViewAdapter extends SelectableRecyclerView.Adapter<FeeViewAdapter.ViewHolder> {
    private List<FeeItem> mDataset;
    private int paddingWidth = 0;

    public FeeViewAdapter(int paddingWidth) {
        this.paddingWidth = paddingWidth;
        mDataset = Collections.emptyList();
    }

    public void setDataset(List<FeeItem> mDataset) {
        List<FeeItem> oldDataset = this.mDataset;
        this.mDataset = mDataset;
        if (oldDataset.size() != this.mDataset.size()) {
            notifyDataSetChanged();
        } else {
            for (int i = 1; i < mDataset.size() - 2; i++) {
                notifyItemChanged(i);
            }
        }
    }

    public FeeItem getItem(int position) {
        return mDataset.get(position);
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).feePerKb;
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
            FeeItem item = mDataset.get(position);
            if (item.value != null) {
                holder.categoryTextView.setText(ValueExtensionsKt.toStringWithUnit(item.value, mBTC));
            }
            if (item.fiatValue != null) {
                holder.itemTextView.setText("~" + ValueExtensionsKt.toStringWithUnit(item.fiatValue));
            }

            holder.valueTextView.setText(String.valueOf(Math.round(item.feePerKb / 1000f)) + " sat/byte");

        } else {
            RecyclerView.LayoutParams layoutParams = (RecyclerView.LayoutParams) holder.itemView.getLayoutParams();
            layoutParams.width = paddingWidth;
            holder.itemView.setLayoutParams(layoutParams);
        }
    }

    @Override
    public int findIndex(Object object) {
        int selected = -1;
        for (int i = 0; i < mDataset.size(); i++) {
            FeeItem feeItem = mDataset.get(i);
            if (feeItem.equals(object)) {
                selected = i;
                break;
            }
        }
        return selected;
    }

    @Override
    public int getItemCount() {
        return mDataset.size();
    }

    @Override
    public int getItemViewType(int position) {
        return mDataset.get(position).type;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder{
        // each data item is just a string in this case
        public TextView categoryTextView;
        public TextView itemTextView;
        public TextView valueTextView;
        RecyclerView.Adapter adapter;

        public ViewHolder(View v, FeeViewAdapter adapter) {
            super(v);
            categoryTextView = v.findViewById(R.id.categorytextView);
            itemTextView = v.findViewById(R.id.itemTextView);
            valueTextView = v.findViewById(R.id.valueTextView);
            this.adapter = adapter;
        }
    }
}
