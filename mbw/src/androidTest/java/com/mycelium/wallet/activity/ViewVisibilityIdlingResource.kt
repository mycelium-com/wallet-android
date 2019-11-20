package com.mycelium.wallet.activity

import android.app.Activity
import android.os.Handler
import android.view.View

import androidx.annotation.IdRes
import androidx.test.espresso.IdlingResource

import java.lang.ref.WeakReference

class ViewVisibilityIdlingResource(view: View, private val visibility: Int) : IdlingResource {
    private val viewWeakReference: WeakReference<View> = WeakReference(view)
    private val name: String

    private var resourceCallback: IdlingResource.ResourceCallback? = null

    constructor(activity: Activity, @IdRes viewId: Int, visibility: Int) : this(activity.findViewById<View>(viewId), visibility)

    init {
        name = "View Visibility for view " + view.id + "(@" + System.identityHashCode(viewWeakReference) + ")"
    }

    override fun getName(): String = name

    override fun isIdleNow(): Boolean {
        val view = viewWeakReference.get()
        val isIdle = view == null || view.visibility == visibility
        if (isIdle) {
            if (resourceCallback != null) {
                resourceCallback!!.onTransitionToIdle()
            }
        } else {
            Handler().postDelayed({ isIdleNow }, IDLE_POLL_DELAY_MILLIS)
        }

        return isIdle
    }

    override fun registerIdleTransitionCallback(resourceCallback: IdlingResource.ResourceCallback) {
        this.resourceCallback = resourceCallback
    }

    companion object {
        private const val IDLE_POLL_DELAY_MILLIS = 100L
    }
}