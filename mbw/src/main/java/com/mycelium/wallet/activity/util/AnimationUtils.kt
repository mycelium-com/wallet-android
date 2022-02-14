package com.mycelium.wallet.activity.util

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.view.View

/**
 * Created by elvis on 07.09.17.
 */
object AnimationUtils {
    private const val DURATION: Long = 400
    fun collapse(view: View, end: (() -> Unit)?) {
        val layoutParams = view.layoutParams
        val anim = ValueAnimator.ofInt(view.height, 0)
        anim.addUpdateListener { valueAnimator: ValueAnimator ->
            layoutParams.height = valueAnimator.animatedValue as Int
            view.layoutParams = layoutParams
        }
        anim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                view.visibility = View.GONE
                end?.invoke()
            }
        })
        anim.duration = DURATION
        anim.start()
    }

    fun expand(view: View, end: (() -> Unit)?) {
        view.visibility = View.VISIBLE
        val widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(
            view.resources.displayMetrics.widthPixels,
            View.MeasureSpec.AT_MOST
        )
        val heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        view.measure(widthMeasureSpec, heightMeasureSpec)
        val height = view.measuredHeight
        val layoutParams = view.layoutParams
        val anim = ValueAnimator.ofInt(0, height)
        anim.addUpdateListener { valueAnimator: ValueAnimator ->
            layoutParams.height = valueAnimator.animatedValue as Int
            view.layoutParams = layoutParams
        }
        anim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                end?.invoke()
            }
        })
        anim.duration = DURATION
        anim.start()
    }
}