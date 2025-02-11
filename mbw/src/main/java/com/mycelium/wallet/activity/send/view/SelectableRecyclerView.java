package com.mycelium.wallet.activity.send.view;

import android.content.Context;
import android.graphics.Canvas;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;

import com.mycelium.wallet.R;
import com.mycelium.wallet.activity.send.event.SelectListener;

import javax.annotation.Nonnull;

import static com.google.common.base.Preconditions.checkNotNull;


public class SelectableRecyclerView extends RecyclerView {
    private SelectListener selectListener;
    private int itemWidth = getResources().getDimensionPixelSize(R.dimen.item_dob_width);
    private int padding;
    private View header;
    private View footer;
    private int oldWidth = 0;

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
        return checkNotNull(((SRVAdapter) getAdapter())).getSelectedItem();
    }

    public void setSelectedItem(int selectedItem) {
        if (getAdapter() == null || selectedItem < 0 || selectedItem >= getAdapter().getItemCount()) {
            return;
        }
        if (((SRVAdapter) getAdapter()).getSelectedItem() != selectedItem) {
            ((SRVAdapter) getAdapter()).setSelectedItem(selectedItem);
            if (selectListener != null) {
                selectListener.onSelect(getAdapter(), selectedItem);
            }
        }
        scrollListToItem(selectedItem);
    }

    public void setSelectedItem(Object selected) {
        SRVAdapter adapter = checkNotNull((SRVAdapter) getAdapter());
        int selectIndex = adapter.findIndex(selected);
        selectIndex = selectIndex == -1 ? adapter.getItemCount() / 2 : selectIndex;
        setSelectedItem(selectIndex);
    }

    @Override
    public void onScrollStateChanged(int state) {
        super.onScrollStateChanged(state);
        if (state == RecyclerView.SCROLL_STATE_IDLE) {
            calculatePositionAndScroll();
        }
    }

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
                scrollListToItem(getSelectedItem());
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
        int expectedPosition = (computeHorizontalScrollOffset() + itemWidth / 2 - 1) / itemWidth;
        if (expectedPosition < 0) {
            expectedPosition = 0;
        } else if (getAdapter() != null && expectedPosition > getAdapter().getItemCount() - 1) {
            expectedPosition = getAdapter().getItemCount() - 1;
        }
        setSelectedItem(expectedPosition);
    }

    private void scrollListToItem(int item) {
        int targetScrollPos = item * itemWidth;
        final int missingPx = targetScrollPos - computeHorizontalScrollOffset();
        if (missingPx != 0f) {
            smoothScrollToPosition(item);
        } else if (item != getSelectedItem()) {
            checkNotNull(((SRVAdapter) getAdapter())).setSelectedItem(item);
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

    public abstract static class SRVAdapter<H extends ViewHolder> extends RecyclerView.Adapter<H> implements Selectable {
        public static final int VIEW_TYPE_ITEM = 2;
        private int selectedItem;
        private SelectableRecyclerView recyclerView;

        @Override
        public void onAttachedToRecyclerView(@Nonnull RecyclerView recyclerView) {
            super.onAttachedToRecyclerView(recyclerView);
            this.recyclerView = (SelectableRecyclerView) recyclerView;
        }

        @Override
        public void onDetachedFromRecyclerView(@Nonnull RecyclerView recyclerView) {
            super.onDetachedFromRecyclerView(recyclerView);
            this.recyclerView = null;
        }

        @Override
        public void onBindViewHolder(final H holder, int position) {
            holder.itemView.setActivated(position == selectedItem);
            holder.itemView.setOnClickListener(
                    view -> recyclerView.setSelectedItem(holder.getAdapterPosition())
            );
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
