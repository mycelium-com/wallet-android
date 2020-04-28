package com.mycelium.bequant.kyc.verticalStepper;

import android.content.Context;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static com.mycelium.bequant.kyc.verticalStepper.VerticalStepperItemView.STATE_ACTIVE;
import static com.mycelium.bequant.kyc.verticalStepper.VerticalStepperItemView.STATE_COMPLETE;
import static com.mycelium.bequant.kyc.verticalStepper.VerticalStepperItemView.STATE_INACTIVE;

public abstract class VerticalStepperAdapter extends BaseAdapter {
    private int focus = 0;

    private SparseArray<View> contentViews = new SparseArray<View>(getCount());

    public VerticalStepperAdapter(Context context) {
        for (int i = 0; i < getCount(); i++) {
            getContentView(context, i);
        }
    }

    public SparseArray<View> getContentViews() {
        return contentViews;
    }

    @Override
    public boolean isEnabled(int position) {
        return isEditable(position) && getState(position) == STATE_COMPLETE;
    }

    @NonNull
    public abstract CharSequence getTitle(int position);

    @Nullable
    public abstract CharSequence getSummary(int position);

    public abstract boolean isEditable(int position);

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Context context = parent.getContext();
        VerticalStepperItemView itemView;

        if (convertView == null) {
            itemView = new VerticalStepperItemView(context);
        } else {
            itemView = (VerticalStepperItemView) convertView;
        }

        applyData(context, itemView, position);

        return itemView;
    }

    public int getState(int position) {
        if (position == focus)
            return STATE_ACTIVE;
        else if (position < focus)
            return STATE_COMPLETE;
        else
            return STATE_INACTIVE;
    }

    private int getCircleNumber(int position) {
        return position + 1;
    }

    private boolean showConnectorLine(int position) {
        return position < getCount() - 1;
    }

    @NonNull
    public abstract View onCreateContentView(Context context, int position);

    private View getContentView(Context context, int position) {
        int id = (int) getItemId(position);
        View contentView = contentViews.get(id);

        if (contentView == null) {
            contentView = onCreateContentView(context, position);
            contentViews.put(id, contentView);
        }

        return contentView;
    }

    public void invalidateContentView(int position) {
        int id = (int) getItemId(position);
        contentViews.remove(id);
        notifyDataSetChanged();
    }

    private void applyData(
            Context context,
            VerticalStepperItemView itemView,
            int position) {
        itemView.setState(getState(position));
        itemView.setCircleNumber(getCircleNumber(position));
        itemView.setTitle(getTitle(position));
        itemView.setEditable(isEditable(position));
        itemView.setShowConnectorLine(showConnectorLine(position));
    }

    public int getFocus() {
        return focus;
    }

    public void jumpTo(int position) {
        if (focus != position) {
            focus = position;
            notifyDataSetChanged();
        }
    }

    public boolean hasPrevious() {
        return focus > 0;
    }

    @Nullable
    public void previous() {
        if (hasPrevious()) {
            jumpTo(focus - 1);
        }
    }

    public boolean hasNext() {
        return focus < getCount() - 1;
    }

    @Nullable
    public void next() {
        if (hasNext()) {
            jumpTo(focus + 1);
        }
    }
}