package com.mycelium.bequant.kyc.verticalStepper

import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.annotation.ColorRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import com.mycelium.bequant.kyc.steps.adapter.StepState
import com.mycelium.wallet.R
import kotlinx.android.synthetic.main.vertical_stepper_view_item.view.*

class VerticalStepperItemView : ConstraintLayout {
    var showConnectorLine = true
    var number = 0
    var title = ""
    var state = StepState.FUTURE

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr)

    init {
        LayoutInflater.from(context).inflate(R.layout.vertical_stepper_view_item, this, true)
    }

    fun update() {
        vertical_stepper_view_item_title?.text = title
        vertical_stepper_view_item_circle?.setNumber(number)
        when (state) {
            StepState.COMPLETE -> {
                vertical_stepper_view_item_circle?.setIconCheck()
                vertical_stepper_view_item_circle?.setComplete()
                setTitleColor(R.color.white)
                if (showConnectorLine) {
                    setLineColor(R.color.bequant_green)
                }
            }
            StepState.COMPLETE_EDITABLE -> {
                vertical_stepper_view_item_circle?.setIconEdit()
                vertical_stepper_view_item_circle?.setActive()
                setTitleColor(R.color.white)
                if (showConnectorLine) {
                    setLineColor(R.color.bequant_yellow)
                }
            }
            StepState.CURRENT -> {
                vertical_stepper_view_item_circle?.setActive()
                setTitleColor(R.color.white)
                if (showConnectorLine) {
                    setLineColor(R.color.bequant_yellow)
                }
            }
            StepState.FUTURE -> {
                vertical_stepper_view_item_circle?.setInactive()
                setTitleColor(R.color.bequant_gray_6)
                if (showConnectorLine) {
                    setLineColor(R.color.bequant_gray_6)
                }
            }
        }
    }

    private fun setTitleColor(@ColorRes color: Int) {
        vertical_stepper_view_item_title?.setTextColor(ResourcesCompat.getColor(resources, color, null))
    }

    private fun setLineColor(@ColorRes color: Int) {
        line?.setBackgroundColor(ResourcesCompat.getColor(resources, color, null))
    }
}