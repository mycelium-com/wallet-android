package com.mycelium.wallet.external

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.modern.ModernMain
import com.mycelium.wallet.activity.modern.Toaster
import com.mycelium.wallet.activity.settings.SettingsPreference
import com.mycelium.wallet.activity.settings.SettingsPreference.getBuySellContent
import com.mycelium.wallet.external.adapter.BuySellSelectAdapter
import com.mycelium.wallet.external.adapter.BuySellSelectItem
import com.mycelium.wapi.wallet.btc.WalletBtcAccount
import com.mycelium.wapi.wallet.eth.EthAccount
import kotlinx.android.synthetic.main.buy_sell_service_selector.*

class BuySellSelectActivity : AppCompatActivity() {
    private val adapter = BuySellSelectAdapter()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.buy_sell_service_selector)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.buysell_bitcoin_title,
                MbwManager.getInstance(this).selectedAccount.coinType.name)

        val mbwManager = MbwManager.getInstance(this)
        lvServices.adapter = adapter
        val items: List<BuySellSelectItem> = mbwManager.environmentSettings.buySellServices.filter { it.isEnabled(mbwManager) }
                .map { buySellService ->
                    BuySellSelectItem(getString(buySellService.title),
                            getString(buySellService.getDescription(mbwManager, mbwManager.selectedAccount.receiveAddress)),
                            buySellService.getIcon(this),
                            null) {
                        if (mbwManager.selectedAccount is WalletBtcAccount || mbwManager.selectedAccount is EthAccount) {
                            buySellService.launchService(this@BuySellSelectActivity, mbwManager, mbwManager.selectedAccount.receiveAddress)
                        } else {
                            Toaster(this@BuySellSelectActivity).toast(R.string.buy_sell_select_activity_warning, true)
                        }
                    }
                } +
                (getBuySellContent()?.listItem?.filter { it.isActive() && SettingsPreference.isContentEnabled(it.parentId) }?.map {
                    BuySellSelectItem(it.title, it.description, null, it.iconUrl) {
                        try {
                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it.link)))
                        } catch (ignored: ActivityNotFoundException) {
                        }
                    }
                } ?: listOf())

        // if there is just one option, auto-click through
        if (items.size == 1) {
            items.first().listener?.invoke()
        }
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