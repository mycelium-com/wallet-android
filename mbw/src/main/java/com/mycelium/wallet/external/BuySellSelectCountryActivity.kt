package com.mycelium.wallet.external

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.settings.SettingsPreference
import com.mycelium.wallet.external.adapter.BuySellSelectCountryAdapter
import kotlinx.android.synthetic.main.activity_buysell_select_country.*


class BuySellSelectCountryActivity : AppCompatActivity(R.layout.activity_buysell_select_country) {


    private val adapter = BuySellSelectCountryAdapter()
    private val availableCounties = SettingsPreference.getBuySellContent()?.exchangeList
            ?.filter { it.isActive() && SettingsPreference.isContentEnabled(it.parentId) }
            ?.flatMap { it.counties }?.toSet()?.sorted()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.select_your_country)
        list.adapter = adapter
        adapter.clickListener = {
            finish()
            startActivity(Intent(this, BuySellBackCardActivity::class.java)
                    .putExtras(intent)
                    .putExtra("country", it))
        }
        adapter.submitList(availableCounties)
        search.doOnTextChanged { text, _, _, _ ->
            adapter.submitList(availableCounties?.filter { country ->
                text?.split(" ")?.all { country.contains(it, true) } ?: false
            })
        }
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