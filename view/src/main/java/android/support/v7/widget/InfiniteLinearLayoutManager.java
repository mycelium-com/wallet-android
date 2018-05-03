package android.support.v7.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

public class InfiniteLinearLayoutManager extends LinearLayoutManager {

    private static final String TAG = "InfiniteLLManager";
    private final LayoutChunkResult mLayoutChunkResult = new LayoutChunkResult();

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

//    int fill(RecyclerView.Recycler recycler, LayoutState layoutState,
//             RecyclerView.State state, boolean stopOnFocusable) {
//        // max offset we should set is mFastScroll + available
//        final int start = layoutState.mAvailable;
//        if (layoutState.mScrollingOffset != LayoutState.SCROLLING_OFFSET_NaN) {
//            // TODO ugly bug fix. should not happen
//            if (layoutState.mAvailable < 0) {
//                layoutState.mScrollingOffset += layoutState.mAvailable;
//            }
//            recycleByLayoutState(recycler, layoutState);
//        }
//        int remainingSpace = layoutState.mAvailable + layoutState.mExtra;
//        LayoutChunkResult layoutChunkResult = mLayoutChunkResult;
//        while ((layoutState.mInfinite || remainingSpace > 0) && layoutState.hasMore(state)) {
//            layoutChunkResult.resetInternal();
//            if (VERBOSE_TRACING) {
//                TraceCompat.beginSection("LLM LayoutChunk");
//            }
//            layoutChunk(recycler, state, layoutState, layoutChunkResult);
//            if (VERBOSE_TRACING) {
//                TraceCompat.endSection();
//            }
//            if (layoutChunkResult.mFinished) {
//                break;
//            }
//            layoutState.mOffset += layoutChunkResult.mConsumed * layoutState.mLayoutDirection;
//            /**
//             * Consume the available space if:
//             * * layoutChunk did not request to be ignored
//             * * OR we are laying out scrap children
//             * * OR we are not doing pre-layout
//             */
//            if (!layoutChunkResult.mIgnoreConsumed || layoutState.mScrapList != null
//                    || !state.isPreLayout()) {
//                layoutState.mAvailable -= layoutChunkResult.mConsumed;
//                // we keep a separate remaining space because mAvailable is important for recycling
//                remainingSpace -= layoutChunkResult.mConsumed;
//            }
//
//            if (layoutState.mScrollingOffset != LayoutState.SCROLLING_OFFSET_NaN) {
//                layoutState.mScrollingOffset += layoutChunkResult.mConsumed;
//                if (layoutState.mAvailable < 0) {
//                    layoutState.mScrollingOffset += layoutState.mAvailable;
//                }
//                recycleByLayoutState(recycler, layoutState);
//            }
//            if (stopOnFocusable && layoutChunkResult.mFocusable) {
//                break;
//            }
//        }
//        if (DEBUG) {
//            validateChildOrder();
//        }
//        return start - layoutState.mAvailable;
//    }
//
//    private void recycleByLayoutState(RecyclerView.Recycler recycler, LayoutState layoutState) {
//        if (!layoutState.mRecycle || layoutState.mInfinite) {
//            return;
//        }
//        if (layoutState.mLayoutDirection == LayoutState.LAYOUT_START) {
//            recycleViewsFromEnd(recycler, layoutState.mScrollingOffset);
//        } else {
//            recycleViewsFromStart(recycler, layoutState.mScrollingOffset);
//        }
//    }
//
//    private void recycleViewsFromStart(RecyclerView.Recycler recycler, int dt) {
//        if (dt < 0) {
//            if (DEBUG) {
//                Log.d(TAG, "Called recycle from start with a negative value. This might happen"
//                        + " during layout changes but may be sign of a bug");
//            }
//            return;
//        }
//        // ignore padding, ViewGroup may not clip children.
//        final int limit = dt;
//        final int childCount = getChildCount();
//        if (mShouldReverseLayout) {
//            for (int i = childCount - 1; i >= 0; i--) {
//                View child = getChildAt(i);
//                if (mOrientationHelper.getDecoratedEnd(child) > limit
//                        || mOrientationHelper.getTransformedEndWithDecoration(child) > limit) {
//                    // stop here
//                    recycleChildren(recycler, childCount - 1, i);
//                    return;
//                }
//            }
//        } else {
//            for (int i = 0; i < childCount; i++) {
//                View child = getChildAt(i);
//                if (mOrientationHelper.getDecoratedEnd(child) > limit
//                        || mOrientationHelper.getTransformedEndWithDecoration(child) > limit) {
//                    // stop here
//                    recycleChildren(recycler, 0, i);
//                    return;
//                }
//            }
//        }
//    }
//
//    private void recycleViewsFromEnd(RecyclerView.Recycler recycler, int dt) {
//        final int childCount = getChildCount();
//        if (dt < 0) {
//            if (DEBUG) {
//                Log.d(TAG, "Called recycle from end with a negative value. This might happen"
//                        + " during layout changes but may be sign of a bug");
//            }
//            return;
//        }
//        final int limit = mOrientationHelper.getEnd() - dt;
//        if (mShouldReverseLayout) {
//            for (int i = 0; i < childCount; i++) {
//                View child = getChildAt(i);
//                if (mOrientationHelper.getDecoratedStart(child) < limit
//                        || mOrientationHelper.getTransformedStartWithDecoration(child) < limit) {
//                    // stop here
//                    recycleChildren(recycler, 0, i);
//                    return;
//                }
//            }
//        } else {
//            for (int i = childCount - 1; i >= 0; i--) {
//                View child = getChildAt(i);
//                if (mOrientationHelper.getDecoratedStart(child) < limit
//                        || mOrientationHelper.getTransformedStartWithDecoration(child) < limit) {
//                    // stop here
//                    recycleChildren(recycler, childCount - 1, i);
//                    return;
//                }
//            }
//        }
//    }
//
//    private void recycleChildren(RecyclerView.Recycler recycler, int startIndex, int endIndex) {
//        if (startIndex == endIndex) {
//            return;
//        }
//        if (DEBUG) {
//            Log.d(TAG, "Recycling " + Math.abs(startIndex - endIndex) + " items");
//        }
//        if (endIndex > startIndex) {
//            for (int i = endIndex - 1; i >= startIndex; i--) {
//                removeAndRecycleViewAt(i, recycler);
//            }
//        } else {
//            for (int i = startIndex; i > endIndex; i--) {
//                removeAndRecycleViewAt(i, recycler);
//            }
//        }
//    }

    class InfiniteLayoutState extends LayoutState {
        @Override
        boolean hasMore(RecyclerView.State state) {
            return true;
        }

        View next(RecyclerView.Recycler recycler) {
            if (mScrapList != null) {
                return nextViewFromScrapList();
            }
            int position = mCurrentPosition;
            int childCount = mRecyclerView.getAdapter().getItemCount();
            if (childCount > 0 && position >= childCount) {
                position = position % childCount;
            } else if (position < 0) {
                position = childCount - Math.abs(position) % childCount;
            }
            final View view = recycler.getViewForPosition(position);
            mCurrentPosition += mItemDirection;
            return view;
        }

        private View nextViewFromScrapList() {
            final int size = mScrapList.size();
            for (int i = 0; i < size; i++) {
                final View view = mScrapList.get(i).itemView;
                final RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) view.getLayoutParams();
                if (lp.isItemRemoved()) {
                    continue;
                }
                if (mCurrentPosition == lp.getViewLayoutPosition()) {
                    assignPositionFromScrapList(view);
                    return view;
                }
            }
            return null;
        }
    }
}
