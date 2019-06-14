package com.mycelium.wallet.activity.send.view;

import android.content.Context;
import android.graphics.Canvas;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;

import com.mycelium.wallet.R;
import com.mycelium.wallet.activity.send.event.SelectListener;


public class SelectableRecyclerView extends RecyclerView {
    private SelectListener selectListener;
    private int itemWidth = getResources().getDimensionPixelSize(R.dimen.item_dob_width);
    private int padding;

    private View header;
    private View footer;

    public SelectableRecyclerView(Context context) {
        super(context);
        setClipToPadding(false);
    }

    public SelectableRecyclerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setClipToPadding(false);
    }

    public SelectableRecyclerView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setClipToPadding(false);
    }

    public void setHeader(View header) {
        this.header = header;
    }

    public void setFooter(View footer) {
        this.footer = footer;
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
        if (getAdapter() == null) {
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
            setPadding(padding, getPaddingTop(), padding, getPaddingBottom());
            if (getAdapter() != null) {
                scrollListToPosition(getSelectedItem());
            }
            oldWidth = getWidth();
        }
        if (header != null) {
            int widthSpec = View.MeasureSpec.makeMeasureSpec(padding, View.MeasureSpec.EXACTLY);
            int heightSpec = View.MeasureSpec.makeMeasureSpec(getHeight(), View.MeasureSpec.EXACTLY);
            header.measure(widthSpec, heightSpec);
            header.layout(0, 0, padding, getHeight());
        }
        if (footer != null) {
            int widthSpec = View.MeasureSpec.makeMeasureSpec(padding, View.MeasureSpec.EXACTLY);
            int heightSpec = View.MeasureSpec.makeMeasureSpec(getHeight(), View.MeasureSpec.EXACTLY);
            footer.measure(widthSpec, heightSpec);
            footer.layout(0, 0, padding, getHeight());
        }
    }

    private void calculatePositionAndScroll() {
        int expectedPosition = Math.round((computeHorizontalScrollOffset() + itemWidth / 2 - 1) / itemWidth);
        if (expectedPosition < 0) {
            expectedPosition = 0;
        } else if (getAdapter() != null && expectedPosition > getAdapter().getItemCount() - 1) {
            expectedPosition = getAdapter().getItemCount() - 1;
        }
        setSelectedItem(expectedPosition);
    }

    private void scrollListToPosition(int expectedPosition) {
        int targetScrollPos = expectedPosition * itemWidth;
        final int missingPx = targetScrollPos - computeHorizontalScrollOffset();
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

    @Override
    public void onDraw(Canvas c) {
        super.onDraw(c);
        if (header != null) {
            // drawing header view to recycler view on start padding place сonsidering rc scroll offset
            c.save();
            c.translate(-computeHorizontalScrollOffset(), 0);
            header.draw(c);
            c.restore();
        }
        if (footer != null) {
            // drawing footer view to recycler view on end padding place сonsidering rc scroll offset
            c.save();
            c.translate(computeHorizontalScrollRange() + padding - computeHorizontalScrollOffset(), 0);
            footer.draw(c);
            c.restore();
        }
    }

    public static abstract class Adapter<VH extends ViewHolder> extends RecyclerView.Adapter<VH> implements Selectable {
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
            holder.itemView.setActivated(position == selectedItem);
            holder.itemView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    recyclerView.setSelectedItem(position);
                }
            });
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
