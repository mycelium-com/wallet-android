package com.mycelium.wallet.external

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.modern.ModernMain
import com.mycelium.wallet.activity.modern.Toaster
import com.mycelium.wallet.activity.settings.SettingsPreference
import com.mycelium.wallet.activity.settings.SettingsPreference.getBuySellContent
import com.mycelium.wallet.databinding.BuySellServiceSelectorBinding
import com.mycelium.wallet.external.adapter.BuySellSelectAdapter
import com.mycelium.wallet.external.adapter.BuySellSelectItem
import com.mycelium.wallet.external.partner.startContentLink
import com.mycelium.wapi.wallet.btc.WalletBtcAccount
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.erc20.ERC20Account
import com.mycelium.wapi.wallet.eth.EthAccount

class BuySellSelectActivity : AppCompatActivity() {
    private val adapter = BuySellSelectAdapter()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = BuySellServiceSelectorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val currency = intent.getSerializableExtra("currency") as CryptoCurrency
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.buysell_bitcoin_title, currency.name)
        val mbwManager = MbwManager.getInstance(this)
        binding.lvServices.adapter = adapter
        val items: List<BuySellSelectItem> = mbwManager.environmentSettings.buySellServices.filter { it.isEnabled(mbwManager) }
                .map { buySellService ->
                    val account = mbwManager.selectedAccount
                    BuySellSelectItem(getString(buySellService.title),
                            getString(buySellService.getDescription(mbwManager, account.receiveAddress), currency.name),
                            buySellService.getIcon(this),
                            null) {
                        if (account is WalletBtcAccount || account is EthAccount || account is ERC20Account) {
                            buySellService.launchService(this@BuySellSelectActivity, mbwManager, account.receiveAddress, currency)
                        } else {
                            Toaster(this@BuySellSelectActivity).toast(R.string.buy_sell_select_activity_warning, true)
                        }
                    }
                } +
                (getBuySellContent()?.listItem?.filter { it.isActive() && SettingsPreference.isContentEnabled(it.parentId) }?.map {
                    BuySellSelectItem(it.title, it.description, null, it.iconUrl) {
                        startContentLink(it.link)
                    }
                } ?: listOf())
        adapter.submitList(items)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            val intent = Intent(this, ModernMain::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}