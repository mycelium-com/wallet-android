package com.mycelium.bequant.kyc

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mycelium.wallet.R
import kotlinx.android.synthetic.main.activity_bequant_market.*


class BequantKycActivity : AppCompatActivity(R.layout.activity_bequant_kyc) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
    }
}