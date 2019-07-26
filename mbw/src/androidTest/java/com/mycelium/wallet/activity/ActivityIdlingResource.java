package com.mycelium.wallet.activity;

import android.app.Activity;
import androidx.annotation.NonNull;
import androidx.test.espresso.IdlingResource;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;

import java.util.Collection;
import java.util.Iterator;

public class ActivityIdlingResource implements IdlingResource {

    private final Class<? extends Activity> mAcivity;
    private final String mName;
    private ResourceCallback mResourceCallback;

    public ActivityIdlingResource(@NonNull Class<? extends Activity> activity) {
        mAcivity = activity;
        mName = "activity " + activity.getName() + "(@" + System.identityHashCode(activity) + ")";
    }

    @Override
    public String getName() {
        return mName;
    }

    @Override
    public boolean isIdleNow() {
        final boolean isIdle = mAcivity != getActivityInstance().getClass();
        if (isIdle && mResourceCallback != null) {
            mResourceCallback.onTransitionToIdle();
        }
        return isIdle;
    }

    @Override
    public void registerIdleTransitionCallback(ResourceCallback resourceCallback) {
        mResourceCallback = resourceCallback;
    }

    private Activity getActivityInstance() {
        final Activity[] mActivity = new Activity[1];
        final Collection<Activity> resumedActivities =
            ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED);
        Iterator<Activity> iterator = resumedActivities.iterator();
        if (iterator.hasNext()) {
            mActivity[0] = iterator.next();
        }
        return mActivity[0];
    }
}