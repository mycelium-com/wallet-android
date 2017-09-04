package com.mycelium.wallet.activity.send.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mycelium.wallet.R;
import com.mycelium.wallet.activity.send.model.FeeLvlItem;
import com.mycelium.wallet.activity.send.view.SelectableRecyclerView;

/**
 * Created by elvis on 31.08.17.
 */

public class FeeLvlViewAdapter extends SelectableRecyclerView.Adapter<FeeLvlViewAdapter.ViewHolder> {

    private FeeLvlItem[] mDataset;

    private int paddingWidth = 0;

    public FeeLvlViewAdapter(FeeLvlItem[] values, int paddingWidth) {
        mDataset = values;
        this.paddingWidth = paddingWidth;
    }

    public FeeLvlItem getItem(int position) {
        return mDataset[position];
    }

    @Override
    public FeeLvlViewAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_ITEM) {
            // create a new view
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.recyclerview_item_fee_lvl, parent, false);
            // set the view's size, margins, paddings and layout parameters
            //...
            return new ViewHolder(v, this);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_padding_sender,
                    parent, false);

            RecyclerView.LayoutParams layoutParams = (RecyclerView.LayoutParams) view.getLayoutParams();
            layoutParams.width = paddingWidth;
            view.setLayoutParams(layoutParams);
            return new ViewHolder(view, this);
        }
    }

    @Override
    public void onBindViewHolder(FeeLvlViewAdapter.ViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        if (getItemViewType(position) == VIEW_TYPE_ITEM) {
            // - get element from your dataset at this position
            // - replace the contents of the view with that element
            FeeLvlItem item = mDataset[position];
//            holder.categoryTextView.setText(mDataset[position].getCategory());
            holder.itemTextView.setText(item.minerFee.getMinerFeeName(holder.itemView.getContext()));
            holder.valueTextView.setText(item.duration);
        }
    }

    @Override
    public int getItemViewType(int position) {
        return mDataset[position].type;
    }

    @Override
    public int getItemCount() {
        return mDataset.length;
    }


    public static class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public TextView categoryTextView;
        public TextView itemTextView;
        public TextView valueTextView;
        RecyclerView.Adapter adapter;

        public ViewHolder(View v, FeeLvlViewAdapter adapter) {
            super(v);
            categoryTextView = (TextView) v.findViewById(R.id.categorytextView);
            itemTextView = (TextView) v.findViewById(R.id.itemTextView);
            valueTextView = (TextView) v.findViewById(R.id.valueTextView);
            this.adapter = adapter;
        }
    }

}
