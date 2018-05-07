package android.support.v7.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

public class InfiniteLinearLayoutManager extends CenterLayoutManager {

    public InfiniteLinearLayoutManager(Context context) {
        super(context);
    }

    public InfiniteLinearLayoutManager(Context context, int orientation, boolean reverseLayout) {
        super(context, orientation, reverseLayout);
    }

    public InfiniteLinearLayoutManager(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    LayoutState createLayoutState() {
        return new InfiniteLayoutState();
    }

    class InfiniteLayoutState extends LinearLayoutManager.LayoutState {
        @Override
        boolean hasMore(RecyclerView.State state) {
            return mRecyclerView.getAdapter().getItemCount() > 1 || super.hasMore(state);
        }

        View next(RecyclerView.Recycler recycler) {
            if (mScrapList != null) {
                return super.next(recycler);
            }
            int position = mCurrentPosition;
            int itemCount = mRecyclerView.getAdapter().getItemCount();
            if (itemCount > 0) {
                position = position % itemCount;
                position = position < 0 ? itemCount + position : position;
            }
            final View view = recycler.getViewForPosition(position);
            mCurrentPosition += mItemDirection;
            return view;
        }
    }
}
