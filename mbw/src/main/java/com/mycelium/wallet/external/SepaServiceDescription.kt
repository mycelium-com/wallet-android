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
        if (activeReceivingAddress.coinType == EthMain || activeReceivingAddress.coinType == EthTest) {
            AlertDialog.Builder(activity, R.style.MyceliumModern_Dialog_BlueButtons)
                    .setItems(arrayOf("BUY", "SELL")) { _, position ->
                        when (position) {
                            0 -> {
                                activity.startActivity(Intent(Intent.ACTION_VIEW,
                                        Uri.parse(BuildConfig.SEPA_BUY_ETH_BITS_OF_GOLD)))
                            }
                            1 -> {
                                activity.startActivity(Intent(Intent.ACTION_VIEW,
                                        Uri.parse(BuildConfig.SEPA_SELL_ETH_BITS_OF_GOLD)))
                            }
                        }
                    }.create().show()
        } else {
            activity.startActivity(Intent(Intent.ACTION_VIEW,
                    Uri.parse(String.format(BuildConfig.SEPA, activeReceivingAddress.toString()))))
        }
    }

    override fun isEnabled(mbwManager: MbwManager) = mbwManager.metadataStorage.sepaIsEnabled

    override fun setEnabled(mbwManager: MbwManager, enabledState: Boolean) {
        mbwManager.metadataStorage.sepaIsEnabled = enabledState
    }
}