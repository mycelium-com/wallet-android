package com.mycelium.wallet.activity.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.mycelium.wallet.R
import com.mycelium.wallet.databinding.ButtonBigRoundWithTextBinding


class RoundButtonWithText(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs) {

    var binding: ButtonBigRoundWithTextBinding
    init {
        binding = ButtonBigRoundWithTextBinding.inflate(LayoutInflater.from(context), this, true)

        val a = context.theme.obtainStyledAttributes(attrs, R.styleable.RoundButtonWithText, 0, 0)
        try {
            val circleColor = a.getColor(R.styleable.RoundButtonWithText_backgroundColor
                    , resources.getColor(R.color.black))
            binding.roundButton.circleColor = circleColor

            val icon = a.getDrawable(R.styleable.RoundButtonWithText_icon)
            binding.roundButton.setImageDrawable(icon)

            val text = a.getString(R.styleable.RoundButtonWithText_text)
            binding.textView.text = text
        } finally {
            a.recycle()
        }
    }

    override fun setOnClickListener(l: OnClickListener?) {
        binding.roundButton.setOnClickListener(l)
    }
}
