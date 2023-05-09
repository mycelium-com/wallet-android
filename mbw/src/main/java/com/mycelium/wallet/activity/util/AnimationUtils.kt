package com.mycelium.wallet.activity.util

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.graphics.drawable.AnimationDrawable
import android.util.TypedValue
import android.view.View
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.mycelium.wallet.R


private const val DURATION: Long = 400

fun View.cancelAnimation() {
    (getTag(R.id.animator) as? ValueAnimator)?.cancel()
}

fun View.collapse(end: (() -> Unit)? = null) {
    cancelAnimation()
    val newLayoutParams = layoutParams
    val endBlock = {
        setTag(R.id.animator, null)
        visibility = View.GONE
        end?.invoke()
    }
    if (height != 0) {
        val anim = ValueAnimator.ofInt(height, 0).apply {
            addUpdateListener { valueAnimator ->
                newLayoutParams.height = valueAnimator.animatedValue as Int
                layoutParams = newLayoutParams
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    endBlock()
                }
            })
            duration = DURATION
        }
        setTag(R.id.animator, anim)
        anim.start()
    } else {
        endBlock()
    }
}

fun View.expand(end: (() -> Unit)? = null) {
    cancelAnimation()
    visibility = View.VISIBLE
    val widthMeasureSpec = View.MeasureSpec.makeMeasureSpec((parent as View).width, View.MeasureSpec.EXACTLY);
    val heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
    measure(widthMeasureSpec, heightMeasureSpec)
    val calcHeight = measuredHeight
    val newLayoutParams = layoutParams
    val endBlock = {
        setTag(R.id.animator, null)
        end?.invoke()
    }
    if (height != calcHeight && measuredWidth != 0) {
        val anim = ValueAnimator.ofInt(height, calcHeight).apply {
            addUpdateListener { valueAnimator ->
                newLayoutParams.height = valueAnimator.animatedValue as Int
                layoutParams = newLayoutParams
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    endBlock()
                }
            })
            duration = DURATION
        }
        setTag(R.id.animator, anim)
        anim.start()
    } else {
        endBlock()
    }
}

fun TextView.startCursor() {
    val rightDrawable = ResourcesCompat.getDrawable(resources, R.drawable.input_cursor, context.theme)
    setCompoundDrawablesWithIntrinsicBounds(null, null, rightDrawable, null)
    post { (rightDrawable as? AnimationDrawable)?.takeUnless(AnimationDrawable::isRunning)?.start() }
    hint = null
}

fun TextView.stopCursor() {
    setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
    hint = "0"
}

fun TextView.resizeTextView() {
    setTextSize(TypedValue.COMPLEX_UNIT_SP, when (text.toString().length) {
        in 0..10 -> 24f
        in 11..16 -> 22f
        else -> 18f
    })
}