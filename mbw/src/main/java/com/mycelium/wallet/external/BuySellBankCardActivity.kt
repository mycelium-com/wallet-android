package com.mycelium.wallet.external

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.settings.SettingsPreference
import com.mycelium.wallet.external.adapter.BuySellSelectAdapter
import com.mycelium.wallet.external.adapter.BuySellSelectItem
import com.mycelium.wallet.external.partner.openLink
import com.mycelium.wapi.wallet.Address
import com.mycelium.wapi.wallet.Util
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import kotlinx.android.synthetic.main.activity_buysell_bank_card.*


class BuySellBankCardActivity : AppCompatActivity(R.layout.activity_buysell_bank_card) {
    private val adapter = BuySellSelectAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val address = intent.getSerializableExtra("address") as Address
        val country = intent.getStringExtra("country")
        val currency = intent.getSerializableExtra("currency") as CryptoCurrency
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.trusted_providers)
        description.text = getString(R.string.buy_crypto_safely, currency.name)
        list.adapter = adapter
        adapter.submitList(SettingsPreference.getBuySellContent()?.exchangeList
                ?.filter {
                    it.isActive() && SettingsPreference.isContentEnabled(it.parentId) &&
                            it.counties.contains(country) &&
                            it.cryptoCurrencies.contains(Util.trimTestnetSymbolDecoration(currency.symbol))
                }?.map {
                    BuySellSelectItem(it.title, it.description.replace("%currency%", currency.name), null, it.iconUrl) {
                        openLink(it.link
                                .replace("%currency%", currency.symbol)
                                .replace("%address%", address.toString())
                                .replace("%lang%", MbwManager.getInstance(this).language))
                    }
                } ?: listOf())
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