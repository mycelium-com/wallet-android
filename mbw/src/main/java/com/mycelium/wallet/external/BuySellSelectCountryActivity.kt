package com.mycelium.wallet.external

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import com.mycelium.wallet.R
import com.mycelium.wallet.external.adapter.BuySellSelectCountryAdapter
import kotlinx.android.synthetic.main.activity_buysell_select_country.*


class BuySellSelectCountryActivity : AppCompatActivity(R.layout.activity_buysell_select_country) {


    private val adapter = BuySellSelectCountryAdapter()

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
        adapter.submitList((countriesMercuryo + countrySimplex).toSet().sorted())

        search.doOnTextChanged { text, _, _, _ ->
            adapter.submitList((countriesMercuryo + countrySimplex).toSet().filter { country ->
                text?.split(" ")?.all { country.contains(it ?: "", true) } ?: false
            }.sorted())
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

    companion object {
        val countriesMercuryo = listOf("Bangladesh", "Bolivia", "Burundi", "Central African Republic",
                "China", "Colombia", "Cuba", "the Democratic Republic of the Congo", "Ecuador", "Iran",
                "Iraq", "Kyrgyzstan", "Lebanon", "Libya", "Mali", "Morocco", "Nepal", "Nicaragua",
                "North Korea", "Pakistan", "Saudi Arabia", "Somalia", "Sudan and Darfur", "South Sudan",
                "Syria", "Vietnam", "Venezuela", "Yemen", "Zimbabwe")

        val countrySimplex = listOf("Australia", "Azerbaijan", "Brazil", "United Kingdom", "Bulgaria",
                "Canada", "Chile", "Colombia", "Costa Rica", "Czech Republic", "Denmark",
                "Dominican Republic", "Europe", "Georgia", "Hong Kong", "Hungary", "India",
                "Indonesia", "Israel", "Japan", "Kazakhstan", "Malaysia", "Mexico", "Moldova",
                "Morocco", "Namibia", "Taiwan", "New Zealand", "Nigeria", "Norway", "Peru",
                "Uruguay", "Philippines", "Poland", "Qatar", "Romania",
                "Russia", "Saudi Arabia", "Singapore", "South Africa", "South Korea",
                "Sweden", "Switzerland", "Turkey", "Ukraine", "United Arab Emirates",
                "United States", "Uzbekistan", "Vietnam")
    }
}