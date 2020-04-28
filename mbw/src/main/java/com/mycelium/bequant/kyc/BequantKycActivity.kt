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
            if (toolbarLayout.visibility == View.VISIBLE) {
                collapsePanel()
            } else {
                showPanel()
            }
        }
    }

    private fun showPanel() {
        toolbarLayout.visibility = View.VISIBLE
        val panelAnimation = ObjectAnimator.ofFloat(
                toolbarLayout,
                "translationY",
                -toolbarLayout.height.toFloat(), 0f
        )

        panelAnimation.addListener(object : Animator.AnimatorListener {
            override fun onAnimationRepeat(animation: Animator?) {

            }

            override fun onAnimationEnd(animation: Animator?) {

            }

            override fun onAnimationCancel(animation: Animator?) {

            }

            override fun onAnimationStart(animation: Animator?) {
                toolbarLayout.visibility = View.VISIBLE
            }

        })

        val animation = AnimatorSet().apply {
            play(panelAnimation)
        }
        animatePanel(animation)
    }


    private fun collapsePanel() {
        val panelAnimation = ObjectAnimator.ofFloat(
                toolbarLayout,
                "translationY",
                0f, -toolbarLayout.height.toFloat()
        )

        panelAnimation.addListener(object : Animator.AnimatorListener {
            override fun onAnimationRepeat(animation: Animator?) {

            }

            override fun onAnimationEnd(animation: Animator?) {
                toolbarLayout.visibility = View.GONE
            }

            override fun onAnimationCancel(animation: Animator?) {

            }

            override fun onAnimationStart(animation: Animator?) {

            }

        })
        val animation = AnimatorSet().apply {
            play(panelAnimation)
        }
        animatePanel(animation)
    }

    private fun animatePanel(animation: AnimatorSet) {
        animation.apply {
            duration = 300
            start()
        }
    }
}