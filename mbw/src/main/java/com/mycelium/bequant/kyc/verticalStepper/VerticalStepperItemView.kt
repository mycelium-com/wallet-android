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
import com.mycelium.wallet.databinding.VerticalStepperViewItemBinding

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

    private val binding =
        VerticalStepperViewItemBinding.inflate(LayoutInflater.from(context), this)

    fun update() {
        binding.verticalStepperViewItemTitle?.text = title
        binding.verticalStepperViewItemCircle?.setNumber(number)
        when (state) {
            StepState.COMPLETE -> {
                binding.verticalStepperViewItemCircle?.setIconCheck()
                binding.verticalStepperViewItemCircle?.setComplete()
                setTitleColor(R.color.white)
                if (showConnectorLine) {
                    setLineColor(R.color.bequant_green)
                }
            }
            StepState.COMPLETE_EDITABLE -> {
                binding.verticalStepperViewItemCircle?.setIconEdit()
                binding.verticalStepperViewItemCircle?.setActive()
                setTitleColor(R.color.white)
                if (showConnectorLine) {
                    setLineColor(R.color.bequant_yellow)
                }
            }
            StepState.CURRENT -> {
                binding.verticalStepperViewItemCircle?.setActive()
                setTitleColor(R.color.white)
                if (showConnectorLine) {
                    setLineColor(R.color.bequant_yellow)
                }
            }
            StepState.FUTURE -> {
                binding.verticalStepperViewItemCircle?.setInactive()
                setTitleColor(R.color.bequant_gray_6)
                if (showConnectorLine) {
                    setLineColor(R.color.bequant_gray_6)
                }
            }
            StepState.ERROR -> {
                binding.verticalStepperViewItemCircle?.setError()
                binding.verticalStepperViewItemCircle?.setIconError()
                setTitleColor(R.color.white)
                if (showConnectorLine) {
                    setLineColor(R.color.bequant_red)
                }
            }
        }
    }

    private fun setTitleColor(@ColorRes color: Int) {
        binding.verticalStepperViewItemTitle?.setTextColor(ResourcesCompat.getColor(resources, color, null))
    }

    private fun setLineColor(@ColorRes color: Int) {
        binding.line?.setBackgroundColor(ResourcesCompat.getColor(resources, color, null))
    }
}