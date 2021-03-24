package com.mycelium.wallet.external

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.settings.SettingsPreference
import com.mycelium.wallet.external.adapter.BuySellSelectAdapter
import com.mycelium.wallet.external.adapter.BuySellSelectItem
import com.mycelium.wapi.wallet.Address
import kotlinx.android.synthetic.main.activity_buysell_bank_card.*


class BuySellBackCardActivity : AppCompatActivity(R.layout.activity_buysell_bank_card) {
    private val adapter = BuySellSelectAdapter()

    lateinit var activeReceivingAddress: Address

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activeReceivingAddress = intent.getSerializableExtra("address") as Address
        val country = intent.getStringExtra("country")
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.trusted_providers)
        description.text = getString(R.string.buy_crypto_safely, activeReceivingAddress.coinType.name)
        list.adapter = adapter
        val services = SettingsPreference.getBuySellContent()?.exchangeList
                ?.filter { it.isActive() && SettingsPreference.isContentEnabled(it.parentId) && it.counties.contains(country) }?.map {
                    BuySellSelectItem(it.title, it.description, null, it.iconUrl) {
                        startActivity(Intent(Intent.ACTION_VIEW,
                                Uri.parse(it.link
                                        .replace("%currency%", activeReceivingAddress.coinType.symbol)
                                        .replace("%address%", activeReceivingAddress.toString())
                                        .replace("%lang%", MbwManager.getInstance(this).language))))
                    }
                } ?: listOf()
        if (services.size == 1) {
            finish()
            services.first().listener?.invoke()
        } else {
            adapter.submitList(services)
        }
    }
}