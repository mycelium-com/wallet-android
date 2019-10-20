package com.mycelium.wallet.activity.view

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.webkit.WebView

/**
 * class copied from
 * https://github.com/AhmadNemati/ClickableWebView/blob/master/clickablewebview/src/main/java/com/ahmadnemati/clickablewebview/ClickableWebView.java
 */
class ClickableWebView(context: Context, attrs: AttributeSet) : WebView(context, attrs), View.OnClickListener, View.OnTouchListener {
    var imageClicklistener: ((String?) -> Unit)? = null
    private var startClickTime: Long = 0

    init {
        setOnClickListener(this)
        setOnTouchListener(this)
    }


    override fun onClick(view: View) {
        val hr = hitTestResult
        try {
            if (hr.type == IMAGE_TYPE) {
                imageClicklistener?.invoke(hr.extra)
            }
        } catch (e: Exception) {
            e.stackTrace
        }
    }

    override fun onTouch(view: View, event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                startClickTime = System.currentTimeMillis()
            }
            MotionEvent.ACTION_UP -> {
                val clickDuration = System.currentTimeMillis() - startClickTime
                if (clickDuration < MAX_CLICK_DURATION) {
                    performClick()
                }
            }
        }
        return false
    }

    companion object {
        private const val MAX_CLICK_DURATION = 200
        private const val IMAGE_TYPE = 5
    }
}