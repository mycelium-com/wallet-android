package com.mycelium.wallet.activity.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import com.mycelium.wallet.R
import kotlinx.android.synthetic.main.button_big_round_with_text.view.*


class RoundButtonWithText(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs) {
    init {
        View.inflate(context, R.layout.button_big_round_with_text, this)

        val a = context.theme.obtainStyledAttributes(attrs, R.styleable.RoundButtonWithText, 0, 0)
        try {
            val circleColor = a.getColor(R.styleable.RoundButtonWithText_backgroundColor
                    , resources.getColor(R.color.black))
            roundButton.circleColor = circleColor

            val icon = a.getDrawable(R.styleable.RoundButtonWithText_icon)
            roundButton.setImageDrawable(icon)

            val text = a.getString(R.styleable.RoundButtonWithText_text)
            textView.text = text
        } finally {
            a.recycle()
        }
    }

    override fun setOnClickListener(l: OnClickListener?) {
        roundButton.setOnClickListener(l)
    }
}
