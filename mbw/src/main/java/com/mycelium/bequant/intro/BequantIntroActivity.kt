package com.mycelium.bequant.intro

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.tabs.TabLayoutMediator
import com.mycelium.bequant.BequantPreference
import com.mycelium.bequant.market.BequantMarketActivity
import com.mycelium.bequant.sign.SignActivity
import com.mycelium.wallet.R
import com.mycelium.wallet.databinding.ActivityBequantIntroBinding

class BequantIntroActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityBequantIntroBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.run {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(false)
            setHomeAsUpIndicator(resources.getDrawable(R.drawable.ic_bequant_clear))
        }
        binding.pager.adapter = IntroPagerAdapter(this)
        TabLayoutMediator(binding.tabs, binding.pager) { _, _ ->
        }.attach()

        binding.create.setOnClickListener {
            BequantPreference.setIntroShown()
            startActivity(Intent(this, SignActivity::class.java))
            finish()
        }
        if (BequantPreference.isLogged()) {
            BequantMarketActivity.start(this)
            finish()
        }
//        else if (BequantPreference.isIntroShown()) {
//            startActivity(Intent(this, SignActivity::class.java))
//            finish()
//        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
            when (item.itemId) {
                android.R.id.home -> {
                    finish()
                    true
                }
                else -> super.onOptionsItemSelected(item)
            }
}