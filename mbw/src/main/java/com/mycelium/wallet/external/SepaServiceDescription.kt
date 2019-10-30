package com.mycelium.wallet.external

import android.app.Activity
import android.content.Intent
import android.net.Uri
import com.google.common.base.Optional
import com.mrd.bitlib.model.Address
import com.mycelium.wallet.BuildConfig
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R


class SepaServiceDescription : BuySellServiceDescriptor(R.string.sepa_buy_sell_title,
        R.string.sepa_buy_sell_description, R.string.sepa_buy_sell_settings_description, R.drawable.logo_sepa) {
    override fun launchService(activity: Activity, mbwManager: MbwManager, activeReceivingAddress: Optional<Address>) {

        val intent = Intent(Intent.ACTION_VIEW,
                Uri.parse(String.format(BuildConfig.SEPA, activeReceivingAddress.get().toString())))
        activity.startActivity(intent)
    }

    override fun isEnabled(mbwManager: MbwManager) = mbwManager.metadataStorage.sepaIsEnabled

    override fun setEnabled(mbwManager: MbwManager, enabledState: Boolean) {
        mbwManager.metadataStorage.sepaIsEnabled = enabledState
    }
}