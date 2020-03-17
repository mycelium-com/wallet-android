package com.mycelium.wallet.external

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AlertDialog
import com.mycelium.wallet.BuildConfig
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wapi.wallet.GenericAddress
import com.mycelium.wapi.wallet.eth.coins.EthMain
import com.mycelium.wapi.wallet.eth.coins.EthTest


class SepaServiceDescription : BuySellServiceDescriptor(R.string.sepa_buy_sell_title,
        R.string.sepa_buy_sell_description, R.string.sepa_buy_sell_settings_description, R.drawable.logo_sepa) {
    override fun getDescription(mbwManager: MbwManager, activeReceivingAddress: GenericAddress): Int =
            if (activeReceivingAddress.coinType == EthMain || activeReceivingAddress.coinType == EthTest) {
                R.string.sepa_eth_buy_sell_description
            } else {
                R.string.sepa_buy_sell_description
            }

    override fun launchService(activity: Activity, mbwManager: MbwManager, activeReceivingAddress: GenericAddress) {
        val buySell = if (activeReceivingAddress.coinType == EthMain || activeReceivingAddress.coinType == EthTest)
            arrayOf(activity.getString(R.string.buy_eth), activity.getString(R.string.sell_eth))
        else arrayOf(activity.getString(R.string.buy_bitcoin), activity.getString(R.string.sell_bitcoin))

        AlertDialog.Builder(activity, R.style.BuySell_Dialog)
                .setTitle(if (activeReceivingAddress.coinType == EthMain || activeReceivingAddress.coinType == EthTest)
                    activity.getString(R.string.dialog_title_buy_sell_eth_sepa) else
                    activity.getString(R.string.dialog_sepa_buy_sell_btc))
                .setItems(buySell) { _, position ->
                    when (position) {
                        0 -> {
                            activity.startActivity(Intent(Intent.ACTION_VIEW,
                                    Uri.parse(BuildConfig.SEPA_BUY_BITS_OF_GOLD)))
                        }
                        1 -> {
                            activity.startActivity(Intent(Intent.ACTION_VIEW,
                                    Uri.parse(BuildConfig.SEPA_SELL_BITS_OF_GOLD)))
                        }
                    }
                }.create().show()
    }

    override fun isEnabled(mbwManager: MbwManager) = mbwManager.metadataStorage.sepaIsEnabled

    override fun setEnabled(mbwManager: MbwManager, enabledState: Boolean) {
        mbwManager.metadataStorage.sepaIsEnabled = enabledState
    }
}