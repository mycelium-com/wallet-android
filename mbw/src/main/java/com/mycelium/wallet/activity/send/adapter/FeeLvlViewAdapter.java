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
import com.mycelium.wallet.activity.send.model.FeeLvlItem;
import com.mycelium.wallet.activity.send.view.SelectableRecyclerView;

import java.util.List;

import javax.annotation.Nonnull;

public class FeeLvlViewAdapter extends SelectableRecyclerView.SRVAdapter<FeeLvlViewAdapter.FeeLvlViewHolder> {
    private List<FeeLvlItem> mDataset;

    private int paddingWidth;

    public FeeLvlViewAdapter(List<FeeLvlItem> values, int paddingWidth) {
        mDataset = values;
        this.paddingWidth = paddingWidth;
    }

    public FeeLvlItem getItem(int position) {
        return mDataset.get(position);
    }

    @Nonnull
    @Override
    public FeeLvlViewHolder onCreateViewHolder(@Nonnull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_ITEM) {
            // create a new view
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.recyclerview_item_fee_lvl, parent, false);
            view.findViewById(R.id.categorytextView).setVisibility(View.GONE);
            ImageView imageView = view.findViewById(R.id.rectangle);
            imageView.setImageResource(R.drawable.recyclerview_item_bottom_rectangle_selector);
            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) imageView.getLayoutParams();
            layoutParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
            layoutParams.height = parent.getResources().getDimensionPixelSize(R.dimen.recycler_item_rectangle_height);
            imageView.setLayoutParams(layoutParams);
            // set the view's size, margins, paddings and layout parameters
            //...
            return new FeeLvlViewHolder(view, this);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_padding_sender,
                    parent, false);

            RecyclerView.LayoutParams layoutParams = (RecyclerView.LayoutParams) view.getLayoutParams();
            layoutParams.width = paddingWidth;
            view.setLayoutParams(layoutParams);
            return new FeeLvlViewHolder(view, this);
        }
    }

    @Override
    public void onBindViewHolder(@Nonnull FeeLvlViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        if (getItemViewType(position) == VIEW_TYPE_ITEM) {
            // - get element from your dataset at this position
            // - replace the contents of the view with that element
            FeeLvlItem item = mDataset.get(position);
//            holder.categoryTextView.setText(mDataset[position].getCategory());
            holder.itemTextView.setText(item.minerFee.getMinerFeeName(holder.itemView.getContext()));
            holder.valueTextView.setText(item.duration);
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
            if (mDataset.get(i).equals(object) || mDataset.get(i).minerFee.equals(object)) {
                selected = i;
                break;
            }
        }
        return selected;
    }

    @Override
    public int getItemViewType(int position) {
        return mDataset.get(position).type;
    }

    @Override
    public int getItemCount() {
        return mDataset.size();
    }

    static class FeeLvlViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        TextView categoryTextView;
        TextView itemTextView;
        TextView valueTextView;
        RecyclerView.Adapter adapter;

        FeeLvlViewHolder(View v, FeeLvlViewAdapter adapter) {
            super(v);
            categoryTextView = v.findViewById(R.id.categorytextView);
            itemTextView = v.findViewById(R.id.itemTextView);
            valueTextView = v.findViewById(R.id.valueTextView);
            this.adapter = adapter;
        }
    }
}
