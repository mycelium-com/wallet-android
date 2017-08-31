package com.mycelium.wallet.activity.send.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mycelium.wallet.R;
import com.mycelium.wallet.activity.send.event.ViewHolderClickListener;
import com.mycelium.wallet.activity.send.event.ViewHolderSelectListener;
import com.mycelium.wallet.activity.send.model.FeeItem;

/**
 * Created by elvis on 31.08.17.
 */

public class FeeViewAdapter extends RecyclerView.Adapter<FeeViewAdapter.ViewHolder> {

    private FeeItem[] mDataset;

    public static final int VIEW_TYPE_PADDING = 1;
    public static final int VIEW_TYPE_ITEM = 2;
    private int paddingWidth = 0;

    private int selectedItem = 1;

    ViewHolderClickListener viewHolderClickListener;
    ViewHolderSelectListener viewHolderSelectListener;

    public FeeViewAdapter(FeeItem[] values, int paddingWidth) {
        mDataset = values;
        this.paddingWidth = paddingWidth;
    }

    public void setViewHolderClickListener(ViewHolderClickListener viewHolderClickListener) {
        this.viewHolderClickListener = viewHolderClickListener;
    }

    public void setViewHolderSelectListener(ViewHolderSelectListener viewHolderSelectListener) {
        this.viewHolderSelectListener = viewHolderSelectListener;
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
            // set the view's size, margins, paddings and layout parameters
            //...
            return new FeeViewAdapter.ViewHolder(v, this, viewHolderClickListener);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_padding_sender,
                    parent, false);

            RecyclerView.LayoutParams layoutParams = (RecyclerView.LayoutParams) view.getLayoutParams();
            layoutParams.width = paddingWidth;
            view.setLayoutParams(layoutParams);
            return new FeeViewAdapter.ViewHolder(view, this, viewHolderClickListener);
        }
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        if (getItemViewType(position) == VIEW_TYPE_ITEM) {
            // - get element from your dataset at this position
            // - replace the contents of the view with that element
//            holder.categoryTextView.setText(mDataset[position].getCategory());
            holder.itemTextView.setText(String.valueOf(mDataset[position].feePerKb) + "sat/kb");
//            holder.valueTextView.setText(mDataset[position].getValue());
            if (position == selectedItem) {
                holder.itemView.setActivated(true);
            } else {
                holder.itemView.setActivated(false);
            }
            holder.itemView.setOnClickListener(holder);
        }
    }

    @Override
    public int getItemCount() {
        return 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener, View.OnLongClickListener {
        // each data item is just a string in this case
        public TextView categoryTextView;
        public TextView itemTextView;
        public TextView valueTextView;
        ViewHolderClickListener viewHolderClickListener;
        RecyclerView.Adapter adapter;

        public ViewHolder(View v, FeeViewAdapter adapter,
                          ViewHolderClickListener viewHolderClickListener) {
            super(v);
            categoryTextView = (TextView) v.findViewById(R.id.categorytextView);
            itemTextView = (TextView) v.findViewById(R.id.itemTextView);
            valueTextView = (TextView) v.findViewById(R.id.valueTextView);
            this.adapter = adapter;
            this.viewHolderClickListener = viewHolderClickListener;
        }

        /**
         * Called when a view has been clicked.
         *
         * @param v The view that was clicked.
         */
        @Override
        public void onClick(View v) {
            viewHolderClickListener.onClick(adapter, getAdapterPosition());
        }

        /**
         * Called when a view has been clicked and held.
         *
         * @param v The view that was clicked and held.
         * @return true if the callback consumed the long click, false otherwise.
         */
        @Override
        public boolean onLongClick(View v) {
            //Do nothing.
            return true;
        }
    }
}
