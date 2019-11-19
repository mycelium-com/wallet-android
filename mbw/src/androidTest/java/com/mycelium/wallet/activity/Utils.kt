package com.mycelium.wallet.activity

import android.app.Activity
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.core.internal.deps.guava.collect.Iterables
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

inline fun <reified T : Activity> waitForActivity(): Activity? {
    var currentActivity: Activity?

    runBlocking {
        withTimeout(10000L) {
            do {
                delay(300L)
                currentActivity = getCurrentActivity()
            } while (currentActivity !is T)
        }
    }
    return getCurrentActivity();
}

inline fun <reified T : Activity> waitWhileAcitvity(): ActivityIdlingResource {
    val activityIdlingResource = ActivityIdlingResource(T::class.java)
    IdlingRegistry.getInstance().register(activityIdlingResource)
    return activityIdlingResource
}

fun getCurrentActivity(): Activity? {
    InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    val arrayOfActivities = arrayOfNulls<Activity>(1)
    InstrumentationRegistry.getInstrumentation().runOnMainSync {
        val activities = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)
        arrayOfActivities[0] = Iterables.getOnlyElement(activities)
    }
    return arrayOfActivities[0]
}