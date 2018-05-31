package com.mycelium.wallet.activity.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.mycelium.wallet.R;

import java.util.ArrayList;
import java.util.List;

public class ViewPagerIndicator extends LinearLayout {
    private static final float SCALE = 1.6f;
    private static final int NO_SCALE = 1;
    private static final int DEF_VALUE = 10;
    private static final int DEF_ICON = R.drawable.pager_indicator_dot;
    private static final int DEF_ICON_SELECTED = R.drawable.pager_indicator_dot_selected;

    private int mPageCount;
    private int mSelectedIndex;
    private int mItemWidth = DEF_VALUE;
    private int mItemHeight = DEF_VALUE;
    private int mDelimiterSize = DEF_VALUE;
    private int mItemIcon = DEF_ICON;
    private int mItemIconSelected = DEF_ICON_SELECTED;

    @NonNull
    private final List<ImageView> mIndexImages = new ArrayList<>();
    @Nullable
    private ViewPager.OnPageChangeListener mListener;

    public ViewPagerIndicator(@NonNull final Context context) {
        this(context, null);
    }

    public ViewPagerIndicator(@NonNull final Context context, @Nullable final AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ViewPagerIndicator(@NonNull final Context context, @Nullable final AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOrientation(HORIZONTAL);
        final TypedArray attributes = context.getTheme().obtainStyledAttributes(attrs, R.styleable.ViewPagerIndicator, 0, 0);
        try {
            mItemWidth = attributes.getDimensionPixelSize(R.styleable.ViewPagerIndicator_itemWidth, DEF_VALUE);
            mItemHeight = attributes.getDimensionPixelSize(R.styleable.ViewPagerIndicator_itemHeight, mItemWidth);
            mDelimiterSize = attributes.getDimensionPixelSize(R.styleable.ViewPagerIndicator_delimiterSize, DEF_VALUE);
            mItemIcon = attributes.getResourceId(R.styleable.ViewPagerIndicator_itemIcon, DEF_ICON);
            mItemIconSelected = attributes.getResourceId(R.styleable.ViewPagerIndicator_itemIconSelected, DEF_ICON_SELECTED);
        } finally {
            attributes.recycle();
        }
        if (isInEditMode()) {
            createEditModeLayout();
        }
    }

    private void createEditModeLayout() {
        for (int i = 0; i < 5; ++i) {
            final FrameLayout boxedItem = createBoxedItem(i);
            addView(boxedItem);
            if (i == 1) {
                final View item = boxedItem.getChildAt(0);
                final ViewGroup.LayoutParams layoutParams = item.getLayoutParams();
                layoutParams.height *= SCALE;
                layoutParams.width *= SCALE;
                item.setLayoutParams(layoutParams);
            }
        }
    }

    public void setupWithViewPager(@NonNull final ViewPager viewPager) {
        setPageCount(viewPager.getAdapter().getCount());
        viewPager.addOnPageChangeListener(new OnPageChangeListener());
    }

    public void addOnPageChangeListener(final ViewPager.OnPageChangeListener listener) {
        mListener = listener;
    }

    private void setSelectedIndex(final int selectedIndex) {
        if (selectedIndex < 0 || selectedIndex > mPageCount - 1) {
            return;
        }

        final ImageView unselectedView = mIndexImages.get(mSelectedIndex);
        unselectedView.setImageResource(mItemIcon);
//        unselectedView.animate().scaleX(NO_SCALE).scaleY(NO_SCALE).setDuration(300).start();

        final ImageView selectedView = mIndexImages.get(selectedIndex);
        selectedView.setImageResource(mItemIconSelected);
//        selectedView.animate().scaleX(SCALE).scaleY(SCALE).setDuration(300).start();

        mSelectedIndex = selectedIndex;
    }

    private void setPageCount(final int pageCount) {
        mPageCount = pageCount;
        mSelectedIndex = 0;
        removeAllViews();
        mIndexImages.clear();

        for (int i = 0; i < pageCount; ++i) {
            addView(createBoxedItem(i));
        }

        setSelectedIndex(mSelectedIndex);
    }

    @NonNull
    private FrameLayout createBoxedItem(final int position) {
        final FrameLayout box = new FrameLayout(getContext());
        final ImageView item = createItem();
        box.addView(item);
        mIndexImages.add(item);

        final LinearLayout.LayoutParams boxParams = new LinearLayout.LayoutParams(
                (int) (mItemWidth * SCALE),
                (int) (mItemHeight * SCALE)
        );
        if (position > 0) {
            boxParams.setMargins(mDelimiterSize, 0, 0, 0);
        }
        box.setLayoutParams(boxParams);
        return box;
    }

    @NonNull
    private ImageView createItem() {
        final ImageView index = new ImageView(getContext());
        final FrameLayout.LayoutParams indexParams = new FrameLayout.LayoutParams(
                mItemWidth,
                mItemHeight
        );
        indexParams.gravity = Gravity.CENTER;
        index.setLayoutParams(indexParams);
        index.setImageResource(mItemIcon);
        index.setScaleType(ImageView.ScaleType.FIT_CENTER);
        return index;
    }

    private class OnPageChangeListener
            implements ViewPager.OnPageChangeListener {
        @Override
        public void onPageScrolled(final int position, final float positionOffset, final int positionOffsetPixels) {
            if (mListener != null) {
                mListener.onPageScrolled(position, positionOffset, positionOffsetPixels);
            }
        }

        @Override
        public void onPageSelected(final int position) {
            setSelectedIndex(position);
            if (mListener != null) {
                mListener.onPageSelected(position);
            }
        }

        @Override
        public void onPageScrollStateChanged(final int state) {
            if (mListener != null) {
                mListener.onPageScrollStateChanged(state);
            }
        }
    }
}