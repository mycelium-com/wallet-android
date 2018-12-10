package com.mycelium.wallet.activity.send.view;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import com.mycelium.wallet.R;
import com.mycelium.wallet.activity.send.event.SelectListener;


public class SelectableRecyclerView extends RecyclerView {
    private SelectListener selectListener;
    private int itemWidth = getResources().getDimensionPixelSize(R.dimen.item_dob_width);
    private int padding;
    private int scrollX;

    public SelectableRecyclerView(Context context) {
        super(context);
    }

    public SelectableRecyclerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public SelectableRecyclerView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public int getPadding() {
        return padding;
    }

    public void setSelectListener(SelectListener selectListener) {
        this.selectListener = selectListener;
    }

    public int getSelectedItem() {
        return ((Adapter) getAdapter()).getSelectedItem();
    }

    public void setSelectedItem(int selectedItem) {
        if(getAdapter() == null) {
            return;
        }
        ((Adapter) getAdapter()).setSelectedItem(selectedItem);
        scrollListToPosition(selectedItem);
        if (selectListener != null) {
            selectListener.onSelect(getAdapter(), selectedItem);
        }
    }

    public void setSelectedItem(Object selected) {
        int selectIndex = ((Adapter) getAdapter()).findIndex(selected);
        selectIndex = selectIndex == -1 ? getAdapter().getItemCount() / 2 : selectIndex;
        setSelectedItem(selectIndex);
    }

    @Override
    public void onScrollStateChanged(int state) {
        super.onScrollStateChanged(state);
        if (state == RecyclerView.SCROLL_STATE_IDLE) {
            calculatePositionAndScroll();
        }
    }

    @Override
    public void setAdapter(RecyclerView.Adapter adapter) {
        super.setAdapter(adapter);
        adapter.registerAdapterDataObserver(new AdapterDataObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                scrollX = 0;
                scrollToPosition(0);
            }
        });
    }

    @Override
    public void onScrolled(int dx, int dy) {
        super.onScrolled(dx, dy);
        scrollX += dx;
    }

    private int oldWidth = 0;

    public void setItemWidth(int itemWidthPx) {
        itemWidth = itemWidthPx;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (changed && oldWidth != getWidth()) {
            padding = (getWidth() - itemWidth) / 2;
            if(getAdapter() != null) {
                scrollListToPosition(getSelectedItem());
            }
            oldWidth = getWidth();
        }
    }

    private void calculatePositionAndScroll() {
        int expectedPosition = Math.round((scrollX + itemWidth / 2 - 1) / itemWidth) + 1;

        if (expectedPosition < 1) {
            expectedPosition = 1;
        } else if (getAdapter() != null && expectedPosition > getAdapter().getItemCount() - 2) {
            expectedPosition = getAdapter().getItemCount() - 2;
        }
        setSelectedItem(expectedPosition);
    }

    private void scrollListToPosition(int expectedPosition) {
        int targetScrollPos = (expectedPosition - 1) * itemWidth;
        final int missingPx = targetScrollPos - scrollX;
        if (missingPx != 0f) {
            if (missingPx == 1) {
                scrollBy(missingPx, 0);
            } else {
                smoothScrollBy(missingPx, 0);
            }
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
                holder.itemView.setActivated(position == selectedItem);
                holder.itemView.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        recyclerView.scrollListToPosition(position);
                    }
                });
            } else {
                ViewGroup.LayoutParams layoutParams = holder.itemView.getLayoutParams();
                layoutParams.width = recyclerView.getPadding();
                holder.itemView.setLayoutParams(layoutParams);
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

        public abstract int findIndex(Object selected);
    }

}
