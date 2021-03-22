package com.mycelium.wallet.external

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import com.mycelium.wallet.BuildConfig
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
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
        val services = mutableListOf<BuySellSelectItem>()
        if (BuySellSelectCountryActivity.countriesMercuryo.contains(country)) {
            services.add(BuySellSelectItem("Mercuryo",
                    "Buy crypto instantly with low fees using debit/credit cards, Apple, Google Pay",
                    AppCompatResources.getDrawable(this, R.drawable.ic_mercurio)) {
                startActivity(Intent(Intent.ACTION_VIEW,
                        Uri.parse(BuildConfig.MERCURIO +
                                "&currency=${activeReceivingAddress.coinType.symbol}" +
                                "&address=$activeReceivingAddress" +
                                "&lang=${MbwManager.getInstance(this).language}")))

            })
        }
        if (BuySellSelectCountryActivity.countrySimplex.contains(country)) {
            services.add(BuySellSelectItem("Simplex",
                    getString(R.string.buy_crypto_safely, activeReceivingAddress.coinType.name),
                    AppCompatResources.getDrawable(this, R.drawable.ic_simplex)) {
                startActivity(Intent(Intent.ACTION_VIEW,
                        Uri.parse(BuildConfig.SIMPLEX +
                                "?crypto=${activeReceivingAddress.coinType.symbol}" +
                                "&address=$activeReceivingAddress")))

            })
        }
        if (services.size == 1) {
            finish()
            services.first().listener?.invoke()
        } else {
            adapter.submitList(services)
        }
    }
}