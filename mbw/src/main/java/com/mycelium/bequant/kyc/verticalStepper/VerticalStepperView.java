package com.mycelium.bequant.kyc.verticalStepper;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;

import androidx.annotation.Nullable;

import java.util.ArrayList;

public class VerticalStepperView extends ListView {
    public VerticalStepperView(Context context) {
        super(context);
        initialize(context);
    }

    public VerticalStepperView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context);
    }

    public VerticalStepperView(
            Context context,
            AttributeSet attrs,
            int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context);
    }

    @TargetApi(21)
    public VerticalStepperView(
            Context context,
            AttributeSet attrs,
            int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initialize(context);
    }

    private void initialize(Context context) {
        setDivider(null);
        setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(
                    AdapterView<?> parent,
                    View view,
                    int position,
                    long id) {
                getAdapter().jumpTo(position);
            }
        });
    }

    @Override
    public VerticalStepperAdapter getAdapter() {
        return (VerticalStepperAdapter) super.getAdapter();
    }

    @Override
    public void setAdapter(ListAdapter adapter) {
        if (!(adapter instanceof VerticalStepperAdapter))
            throw new IllegalArgumentException(
                    "Must be a VerticalStepperAdapter");
        super.setAdapter(adapter);
    }

    public void setStepperAdapter(VerticalStepperAdapter adapter) {
        setAdapter(adapter);
    }

    @Nullable
    @Override
    public Parcelable onSaveInstanceState() {
        SparseArray<View> contentViews = getAdapter().getContentViews();
        ArrayList<Bundle> containers = new ArrayList<>(contentViews.size());

        for (int i = 0; i < contentViews.size(); i++) {
            int id = contentViews.keyAt(i);
            View contentView = contentViews.valueAt(i);
            SparseArray<Parcelable> container = new SparseArray<>(1);
            contentView.saveHierarchyState(container);

            Bundle bundle = new Bundle(2);
            bundle.putInt("id", id);
            bundle.putSparseParcelableArray("container", container);

            containers.add(bundle);
        }

        Bundle bundle = new Bundle(3);
        bundle.putParcelable("super", super.onSaveInstanceState());
        bundle.putParcelableArrayList("containers", containers);
        bundle.putInt("focus", getAdapter().getFocus());

        return bundle;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            Bundle bundle = (Bundle) state;
            super.onRestoreInstanceState(bundle.getParcelable("super"));

            ArrayList<Bundle> containers = bundle
                    .getParcelableArrayList("containers");
            SparseArray<View> contentViews = getAdapter().getContentViews();

            for (Bundle contentViewBundle : containers) {
                int id = contentViewBundle.getInt("id");
                SparseArray<Parcelable> container = contentViewBundle
                        .getSparseParcelableArray("container");

                View contentView = contentViews.get(id);
                if (contentView != null) {
                    contentView.restoreHierarchyState(container);
                }
            }

            getAdapter().jumpTo(bundle.getInt("focus"));
        } else
            super.onRestoreInstanceState(state);
    }

    @Override
    protected void dispatchSaveInstanceState(SparseArray<Parcelable> container) {
        dispatchFreezeSelfOnly(container);
    }

    @Override
    protected void dispatchRestoreInstanceState(
            SparseArray<Parcelable> container) {
        dispatchThawSelfOnly(container);
    }
}
