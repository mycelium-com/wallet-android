package com.mycelium.wallet.activity

import android.app.Activity
import androidx.test.espresso.IdlingResource
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage

class ActivityIdlingResource(private val acivity: Class<out Activity>) : IdlingResource {

    private val name = "activity " + acivity.name + "(@" + System.identityHashCode(acivity) + ")"

    private var resourceCallback: IdlingResource.ResourceCallback? = null

    private val activityInstance: Activity?
        get() {
            val arrayOfActivities = arrayOfNulls<Activity>(1)
            val resumedActivities = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)
            val iterator = resumedActivities.iterator()
            if (iterator.hasNext()) {
                arrayOfActivities[0] = iterator.next()
            }
            return arrayOfActivities[0]
        }

    override fun getName(): String {
        return name
    }

    override fun isIdleNow(): Boolean {
        if (activityInstance == null || resourceCallback == null){
            return false
        }
        val isIdle = acivity != activityInstance!!.javaClass
        if (isIdle && resourceCallback != null) {
            resourceCallback!!.onTransitionToIdle()
        }
        return isIdle
    }

    override fun registerIdleTransitionCallback(resourceCallback: IdlingResource.ResourceCallback) {
        this.resourceCallback = resourceCallback
    }
}