package com.mycelium.bequant.kyc

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.mycelium.wallet.R
import androidx.activity.viewModels


class BequantKycActivity : AppCompatActivity(R.layout.activity_bequant_kyc) {
    val viewModel: BequantKycViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.run {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
            setHomeAsUpIndicator(resources.getDrawable(R.drawable.ic_back_arrow))
        }
        viewModel.title.observe(this, Observer {
            supportActionBar?.title = it
        })
    }
}