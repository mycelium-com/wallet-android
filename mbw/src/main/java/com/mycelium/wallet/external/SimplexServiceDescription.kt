package com.mycelium.wallet.external

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.simplex.SimplexMainActivity
import com.mycelium.wapi.wallet.GenericAddress
import com.mycelium.wapi.wallet.eth.coins.EthMain
import com.mycelium.wapi.wallet.eth.coins.EthTest

class SimplexServiceDescription : BuySellServiceDescriptor(R.string.si_buy_sell, R.string.si_buy_sell_description, R.string.si_setting_show_button_summary, R.drawable.credit_card_buy) {

    override fun getDescription(mbwManager: MbwManager, activeReceivingAddress: GenericAddress): Int =
            if (activeReceivingAddress.coinType == EthMain || activeReceivingAddress.coinType == EthTest) {
                R.string.si_eth_buy_sell_description
            } else {
                R.string.si_buy_sell_description
            }

    override fun launchService(context: Activity, mbwManager: MbwManager, activeReceivingAddress: GenericAddress) {
        if (!mbwManager.selectedAccount.canSpend()) {
            Toast.makeText(context, R.string.lt_warning_watch_only_account, Toast.LENGTH_LONG).show()
            return
        }
        val receivingAddress = mbwManager.selectedAccount.receiveAddress
        if (receivingAddress != null) {
            val regions = if (activeReceivingAddress.coinType == EthMain || activeReceivingAddress.coinType == EthTest) {
                mapOf<String, Boolean>(context.getString(R.string.europe) to true, context.getString(R.string.asia) to false,
                        context.getString(R.string.united_states) to false, context.getString(R.string.australia) to false)
            } else {
                mapOf(context.getString(R.string.europe) to true, context.getString(R.string.asia) to true,
                        context.getString(R.string.united_states) to true, context.getString(R.string.australia) to true)
            }
            val adapter = Adapter(context).apply {
                this.setData(regions)
            }
            AlertDialog.Builder(context, R.style.BuySell_Dialog)
                    .setTitle(R.string.select_you_region)
                    .setAdapter(adapter) { _, i ->
                        if (regions[adapter.getItem(i)] == true) {
                            context.startActivity(Intent(context, SimplexMainActivity::class.java)
                                    .putExtra("walletAddress", receivingAddress.toString()))
                        }
                    }
                    .create().show()
        } else {
            Toast.makeText(context, "Simplex cannot start - no available address.", Toast.LENGTH_LONG).show()
        }
    }

    override fun isEnabled(mbwManager: MbwManager): Boolean =
            mbwManager.metadataStorage.simplexIsEnabled

    override fun setEnabled(mbwManager: MbwManager, enabledState: Boolean) {
        mbwManager.metadataStorage.simplexIsEnabled = enabledState
    }


    internal class Adapter(context: Context) : ArrayAdapter<String>(context, R.layout.item_dialog_simplex) {
        val content = mutableMapOf<String, Boolean>()

        fun setData(data: Map<String, Boolean>) {
            content.clear()
            content.putAll(data)
            clear()
            addAll(content.keys)
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View =
                super.getView(position, convertView, parent).apply {
                    isEnabled = content[getItem(position)] ?: true
                }
    }
}