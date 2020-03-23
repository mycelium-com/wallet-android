package com.mycelium.bequant.intro

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.tabs.TabLayoutMediator
import com.mycelium.bequant.market.BequantMarketActivity
import com.mycelium.wallet.R
import kotlinx.android.synthetic.main.activity_bequant_intro.*

class BequantIntroActivity : AppCompatActivity(R.layout.activity_bequant_intro) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.run {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(false)
            setHomeAsUpIndicator(resources.getDrawable(R.drawable.ic_bequant_clear))
        }
        pager.adapter = IntroPagerAdapter(this)
        TabLayoutMediator(tabs, pager) { tab, _ ->
        }.attach()

        create.setOnClickListener {
            finish()
            startActivity(Intent(this, BequantMarketActivity::class.java))
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean =
            when (item?.itemId) {
                android.R.id.home -> {
                    finish()
                    true
                }
                else -> super.onOptionsItemSelected(item)
            }
}