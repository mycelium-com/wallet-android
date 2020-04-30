package com.mycelium.bequant.kyc

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.mycelium.wallet.R
import kotlinx.android.synthetic.main.activity_bequant_kyc.*
import kotlinx.android.synthetic.main.activity_bequant_market.toolbar


class BequantKycActivity : AppCompatActivity(R.layout.activity_bequant_kyc) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        val verticalAdapter = MainStepperAdapter(this)
        stepper.setStepperAdapter(verticalAdapter)
        toolbar.setOnClickListener {
            expandable_layout.toggle()
        }
    }
}