package com.mycelium.view;

import android.support.v7.widget.RecyclerView;
import android.view.View;

public class ItemCentralizer extends RecyclerView.OnScrollListener {
    @Override
    public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
        super.onScrollStateChanged(recyclerView, newState);
        if (newState == RecyclerView.SCROLL_STATE_IDLE) {
            int childForCentralize = -1;
            int minDistance = Integer.MAX_VALUE;
            for (int i = 0; i < recyclerView.getChildCount(); i++) {
                View view = recyclerView.getChildAt(i);
                int distance = recyclerView.getWidth() / 2 - (view.getLeft() + view.getWidth() / 2);
                if (Math.abs(distance) < Math.abs(minDistance)) {
                    childForCentralize = i;
                    minDistance = distance;
                }
            }
            if (childForCentralize != -1) {
                View view = recyclerView.getChildAt(childForCentralize);
                int distance = recyclerView.getWidth() / 2 - (view.getLeft() + view.getWidth() / 2);
                if(distance != 0) {
                    recyclerView.smoothScrollBy(-distance, 0);
                }
            }
        }
    }
}
