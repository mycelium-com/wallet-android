package com.mycelium.bequant.kyc.verticalStepper

import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.mycelium.wallet.R

class VerticalStepperItemCircleView : FrameLayout {
    private var number: TextView? = null
    private var icon: ImageView? = null

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)

    init {
        LayoutInflater.from(context).inflate(R.layout.vertical_stepper_view_item_circle, this, true)

        number = findViewById<View>(R.id.vertical_stepper_view_item_circle_number) as TextView
        icon = findViewById<View>(R.id.vertical_stepper_view_item_circle_icon) as ImageView
    }

    fun setActive() {
        background = ContextCompat.getDrawable(context, R.drawable.vertical_stepper_view_item_circle_active)
        number?.setTextColor(ResourcesCompat.getColor(resources, R.color.bequant_gray_3, null))
    }

    fun setComplete() {
        background = ContextCompat.getDrawable(context, R.drawable.vertical_stepper_view_item_circle_completed)
    }

    fun setInactive() {
        background = ContextCompat.getDrawable(context, R.drawable.vertical_stepper_view_item_circle_inactive)
        number?.setTextColor(ResourcesCompat.getColor(resources, R.color.white, null))
    }

    fun setError() {
        background = ContextCompat.getDrawable(context, R.drawable.vertical_stepper_view_item_circle_error)
    }
    
    fun setNumber(value: Int) {
        icon!!.visibility = View.GONE
        number!!.visibility = View.VISIBLE
        number!!.text = value.toString()
    }

    fun setIconCheck() {
        setIconResource(R.drawable.ic_vertical_stepper_done)
    }

    fun setIconEdit() {
        setIconResource(R.drawable.ic_vertical_stepper_edit)
    }

    fun setIconResource(id: Int) {
        number!!.visibility = View.GONE
        icon!!.visibility = View.VISIBLE
        icon!!.setImageResource(id)
    }
}