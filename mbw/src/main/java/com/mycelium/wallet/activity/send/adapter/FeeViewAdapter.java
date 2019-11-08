package com.mycelium.wallet.activity.send.adapter;

import androidx.recyclerview.widget.RecyclerView;
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
import com.mycelium.wapi.wallet.coins.Value;

import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;


public class FeeViewAdapter extends SelectableRecyclerView.SRVAdapter<FeeViewAdapter.FeeViewHolder> {
    private List<FeeItem> mDataset;
    private int paddingWidth;
    private FeeItemFormatter formatter;

    public FeeViewAdapter(int paddingWidth) {
        this.paddingWidth = paddingWidth;
        mDataset = Collections.emptyList();
    }

    public void setDataset(List<FeeItem> mDataset) {
        this.mDataset = mDataset;
        notifyDataSetChanged();
    }

    public void setFormatter(FeeItemFormatter formatter){
        this.formatter = formatter;
    }

    public FeeItem getItem(int position) {
        return mDataset.get(position);
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).feePerKb;
    }

    @Nonnull
    @Override
    public FeeViewHolder onCreateViewHolder(@Nonnull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_ITEM) {
            // create a new view
            View view = LayoutInflater.from(parent.getContext())
                     .inflate(R.layout.recyclerview_item_fee_lvl, parent, false);
            ImageView imageView = view.findViewById(R.id.rectangle);
            imageView.setImageResource(R.drawable.recyclerview_item_top_rectangle_selector);
            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) imageView.getLayoutParams();
            layoutParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
            layoutParams.height = parent.getResources().getDimensionPixelSize(R.dimen.recycler_item_triangle_height);
            imageView.setLayoutParams(layoutParams);
            // set the view's size, margins, paddings and layout parameters
            //...
            return new FeeViewHolder(view, this);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_padding_sender,
                        parent, false);

            RecyclerView.LayoutParams layoutParams = (RecyclerView.LayoutParams) view.getLayoutParams();
            layoutParams.width = paddingWidth;
            view.setLayoutParams(layoutParams);
            return new FeeViewHolder(view, this);
        }
    }

    @Override
    public void onBindViewHolder(@Nonnull FeeViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        if (getItemViewType(position) == VIEW_TYPE_ITEM) {
            // - get element from your dataset at this position
            // - replace the contents of the view with that element
            FeeItem item = mDataset.get(position);
            if (item.value != null) {
                holder.categoryTextView.setText(formatter.getFeeAbsValue(item.value));
            }
            if (item.fiatValue != null) {
                holder.itemTextView.setText(formatter.getAltValue(item.fiatValue));
            }

            holder.valueTextView.setText(formatter.getFeePerUnit(item.feePerKb));
        } else {
            RecyclerView.LayoutParams layoutParams = (RecyclerView.LayoutParams) holder.itemView.getLayoutParams();
            layoutParams.width = paddingWidth;
            holder.itemView.setLayoutParams(layoutParams);
        }
    }

    @Override
    public int findIndex(Object object) {
        int selected = -1;
        int bestNear = -1;
        long bestNearPerKb = Long.MAX_VALUE;
        for (int i = 0; i < mDataset.size(); i++) {
            FeeItem feeItem = mDataset.get(i);
            if (feeItem.equals(object)) {
                selected = i;
                break;
            } else if (object instanceof Value
                    && Math.abs(((Value) object).value - feeItem.feePerKb) < Math.abs(((Value) object).value - bestNearPerKb)) {
                bestNear = i;
                bestNearPerKb = feeItem.feePerKb;
            }
        }
        return selected == -1 ? bestNear : selected;
    }

    @Override
    public int getItemCount() {
        return mDataset.size();
    }

    @Override
    public int getItemViewType(int position) {
        return mDataset.get(position).type;
    }

    static class FeeViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        TextView categoryTextView;
        TextView itemTextView;
        TextView valueTextView;
        RecyclerView.Adapter adapter;

        FeeViewHolder(View v, FeeViewAdapter adapter) {
            super(v);
            categoryTextView = v.findViewById(R.id.categorytextView);
            itemTextView = v.findViewById(R.id.itemTextView);
            valueTextView = v.findViewById(R.id.valueTextView);
            this.adapter = adapter;
        }
    }

    public interface FeeItemFormatter {
        String getFeeAbsValue(Value value);
        String getAltValue(Value value);
        String getFeePerUnit(long value);
    }
}
