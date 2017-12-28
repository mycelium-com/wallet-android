package com.mycelium.wallet.activity.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.mycelium.wallet.R


class RoundButtonWithText(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs) {

    init {
        View.inflate(context, R.layout.button_big_round_with_text, this)

        val a = context.theme.obtainStyledAttributes(attrs, R.styleable.RoundButtonWithText, 0, 0)
        try {
            val roundButton = findViewById(R.id.roundButton) as RoundButton

            val circleColor = a.getColor(R.styleable.RoundButtonWithText_backgroundColor
                    , resources.getColor(R.color.black))
            roundButton.circleColor = circleColor

            val icon = a.getDrawable(R.styleable.RoundButtonWithText_icon)
            roundButton.setImageDrawable(icon)

            val text = a.getString(R.styleable.RoundButtonWithText_text)
            val textView = findViewById(R.id.textView) as TextView
            textView.text = text
        } finally {
            a.recycle()
        }
    }

    override fun setOnClickListener(l: OnClickListener?) {
        val roundButton = findViewById(R.id.roundButton) as RoundButton
        roundButton.setOnClickListener(l)
    }
}
