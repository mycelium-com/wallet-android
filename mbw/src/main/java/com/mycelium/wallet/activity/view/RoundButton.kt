package com.mycelium.wallet.activity.view

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.StateListDrawable
import android.os.Build
import android.util.AttributeSet
import android.util.StateSet
import android.widget.ImageButton
import com.mycelium.wallet.R
import kotlin.properties.Delegates


class RoundButton(context: Context, attrs: AttributeSet) : ImageButton(context, attrs) {
    var circleColor: Int by Delegates.observable(0) { _, _, new ->
        val drawable: Drawable
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            drawable = resources.getDrawable(R.drawable.round_background, context.theme)

            val bg = (if (drawable is RippleDrawable)
                drawable.getDrawable(0) else drawable) as GradientDrawable
            bg.setColor(new)
        } else {
            val defaultDrawable = resources.getDrawable(R.drawable.round_background).mutate()
            val pressedDrawable = resources.getDrawable(R.drawable.round_background).mutate()
            (defaultDrawable as GradientDrawable).setColor(new)
            val pressedColor = Color.argb(128, Color.red(new), Color.green(new), Color.blue(new))
            (pressedDrawable as GradientDrawable).setColor(pressedColor)
            drawable = StateListDrawable()
            drawable.addState(intArrayOf(android.R.attr.state_pressed), pressedDrawable)
            drawable.addState(StateSet.WILD_CARD, defaultDrawable)
        }
        background = drawable
    }

    init {
        val a = context.theme.obtainStyledAttributes(attrs, R.styleable.RoundButton, 0, 0)
        try {
            circleColor = a.getColor(R.styleable.RoundButton_backgroundColor
                    , resources.getColor(R.color.black))
        } finally {
            a.recycle()
        }
    }
}
