package com.mycelium.wallet.activity.export.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mycelium.wallet.WalletApplication
import com.mycelium.wallet.activity.export.ExportAsQrBtcHDViewModel
import com.mycelium.wallet.activity.export.ExportAsQrBtcSAViewModel
import com.mycelium.wallet.activity.export.ExportAsQrViewModel
import com.mycelium.wapi.wallet.ExportableAccount
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.btc.bip44.HDAccount
import com.mycelium.wapi.wallet.btc.single.SingleAddressAccount

class ExportAsQrFactory(
    val account: WalletAccount<*>,
    val accountData: ExportableAccount.Data
) : ViewModelProvider.Factory {
    val instance = WalletApplication.getInstance()
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        when {
            account is HDAccount && (accountData.publicDataMap?.size ?: 0 > 1) ->
                ExportAsQrBtcHDViewModel(instance) as T

            account is SingleAddressAccount && accountData.publicDataMap!!.size > 1
                    && account.availableAddressTypes.size > 1 ->
                ExportAsQrBtcSAViewModel(instance) as T

            else -> ExportAsQrViewModel(instance) as T
        }
}