package com.mycelium.bequant.market

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mycelium.wallet.R
import kotlinx.android.synthetic.main.activity_bequant_market.*


class BequantMarketActivity : AppCompatActivity(R.layout.activity_bequant_market) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSupportActionBar(toolbar)
        supportActionBar?.run {
            title = "Powered by"
            subtitle = "BEQUANT"
            setDisplayShowHomeEnabled(true)
            setIcon(R.drawable.action_bar_logo)
        }
    }
}