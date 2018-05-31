package com.mycelium.wallet.activity.view;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

public class DividerItemDecoration extends RecyclerView.ItemDecoration {
    private Drawable mDivider;
    private int mOrientation;
    private Rect imgPadding = new Rect();
    private int fromItem = 0;

    public DividerItemDecoration(Drawable divider, int orientation) {
        mDivider = divider;
        mOrientation = orientation;
        mDivider.getPadding(imgPadding);
    }

    public void setFromItem(int fromItem) {
        this.fromItem = fromItem;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        super.getItemOffsets(outRect, view, parent, state);

        if (parent.getChildAdapterPosition(view) == 0) {
            return;
        }

        outRect.top = mDivider.getIntrinsicHeight();
    }

    @Override
    public void onDraw(Canvas canvas, RecyclerView parent, RecyclerView.State state) {
        if (mOrientation == LinearLayoutManager.VERTICAL) {
            drawVertical(canvas, parent);
        } else {
            drawHorizontal(canvas, parent);
        }
    }

    private void drawHorizontal(Canvas canvas, RecyclerView parent) {
        int dividerTop = parent.getPaddingTop() + imgPadding.top;
        int dividerBottom = parent.getHeight() - parent.getPaddingBottom() - imgPadding.bottom;

        int childCount = parent.getChildCount();
        for (int i = fromItem; i < childCount - 1; i++) {
            View child = parent.getChildAt(i);

            RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();

            int dividerLeft = child.getRight() + params.rightMargin;
            int dividerRight = dividerLeft + mDivider.getIntrinsicWidth();

            mDivider.setBounds(dividerLeft, dividerTop, dividerRight, dividerBottom);
            mDivider.draw(canvas);
        }
    }

    private void drawVertical(Canvas canvas, RecyclerView parent) {
        int dividerLeft = parent.getPaddingLeft();
        int dividerRight = parent.getWidth() - parent.getPaddingRight();

        int childCount = parent.getChildCount();
        for (int i = fromItem; i < childCount - 1; i++) {
            View child = parent.getChildAt(i);

            RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();

            int dividerTop = child.getBottom() + params.bottomMargin;
            int dividerBottom = dividerTop + mDivider.getIntrinsicHeight();

            mDivider.setBounds(dividerLeft, dividerTop, dividerRight, dividerBottom);
            mDivider.draw(canvas);
        }
    }
}

