package com.mycelium.bequant.market

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.appcompat.app.AppCompatActivity
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.modern.ModernMain
import kotlinx.android.synthetic.main.activity_bequant_market.*


class BequantMarketActivity : AppCompatActivity(R.layout.activity_bequant_market) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        val logoMenuClick = { _: View ->
            val isOpened = logoMenu.visibility == VISIBLE
            logoMenu.visibility = if (isOpened) GONE else VISIBLE
            logoArrow.setImageDrawable(logoArrow.resources.getDrawable(
                    if (isOpened) R.drawable.ic_arrow_drop_down else R.drawable.ic_arrow_drop_down_active))
        }
        logoButton.setOnClickListener(logoMenuClick)
        logoMenu.setOnClickListener(logoMenuClick)
        myceliumWallet.setOnClickListener {
            finish()
            startActivity(Intent(this, ModernMain::class.java))
        }
    }
}