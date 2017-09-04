package com.mycelium.wallet.activity.send.view;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;

import com.mycelium.wallet.R;
import com.mycelium.wallet.activity.send.event.SelectListener;

/**
 * Created by elvis on 02.09.17.
 */

public class SelectableRecyclerView extends RecyclerView {
    private SelectListener selectListener;
    private int itemWidth;
    private int padding;

    public SelectableRecyclerView(Context context) {
        super(context);
    }

    public SelectableRecyclerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public SelectableRecyclerView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setSelectListener(SelectListener selectListener) {
        this.selectListener = selectListener;
    }

    public int getSelectedItem() {
        return ((Adapter) getAdapter()).getSelectedItem();
    }

    public void setSelectedItem(int selectedItem) {
        ((Adapter) getAdapter()).setSelectedItem(selectedItem);
        scrollListToPosition(selectedItem);
        if (selectListener != null) {
            selectListener.onSelect(getAdapter(), selectedItem);
        }
    }

    @Override
    public void onScrollStateChanged(int state) {
        super.onScrollStateChanged(state);
        if (state == RecyclerView.SCROLL_STATE_IDLE) {
            calculatePositionAndScroll();
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        itemWidth = getResources().getDimensionPixelSize(R.dimen.item_dob_width);
        padding = (getWidth() - itemWidth) / 2;
        scrollListToPosition(getSelectedItem());
    }

    private void calculatePositionAndScroll() {
        int scroll = computeHorizontalScrollOffset();
        int expectedPosition = Math.round((scroll + padding - itemWidth) / itemWidth) + 1;

        if (expectedPosition == -1) {
            expectedPosition = 1;
        } else if (expectedPosition >= getAdapter().getItemCount() - 1) {
            expectedPosition = getAdapter().getItemCount() - 2;
        }
        scrollListToPosition(expectedPosition);
        setSelectedItem(expectedPosition);
    }

    private void scrollListToPosition(int expectedPosition) {
        int scroll = computeHorizontalScrollOffset();
        int targetScrollPos = expectedPosition * itemWidth - padding;
        int missingPx = targetScrollPos - scroll;
        if (missingPx != 0f) {
            smoothScrollBy(missingPx, 0);
        } else if (expectedPosition != getSelectedItem()) {
            ((Adapter) getAdapter()).setSelectedItem(expectedPosition);
        }
    }


    public static abstract class Adapter<VH extends ViewHolder> extends RecyclerView.Adapter<VH> implements Selectable {
        public static final int VIEW_TYPE_PADDING = 1;
        public static final int VIEW_TYPE_ITEM = 2;
        private int selectedItem;
        private SelectableRecyclerView recyclerView;

        @Override
        public void onAttachedToRecyclerView(RecyclerView recyclerView) {
            super.onAttachedToRecyclerView(recyclerView);
            this.recyclerView = (SelectableRecyclerView) recyclerView;
        }

        @Override
        public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
            super.onDetachedFromRecyclerView(recyclerView);
            this.recyclerView = null;
        }

        @Override
        public void onBindViewHolder(VH holder, final int position) {
            if (getItemViewType(position) != VIEW_TYPE_PADDING) {
                if (position == selectedItem) {
                    holder.itemView.setActivated(true);
                } else {
                    holder.itemView.setActivated(false);
                }
                holder.itemView.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        recyclerView.scrollListToPosition(position);
                    }
                });
            }
        }

        @Override
        public void setSelectedItem(int selectedItem) {
            int oldSelectedItem = this.selectedItem;
            this.selectedItem = selectedItem;
            notifyItemChanged(oldSelectedItem);
            notifyItemChanged(selectedItem);
        }

        @Override
        public int getSelectedItem() {
            return selectedItem;
        }

    }

}
