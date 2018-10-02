package com.mycelium.wallet.activity.main.address

import android.app.Application
import android.support.v4.app.FragmentActivity
import com.mrd.bitlib.model.AddressType
import com.mycelium.wapi.wallet.bip44.HDAccount
import com.mycelium.wapi.wallet.single.SingleAddressAccount

class AddressFragmentBtcModel(val app: Application) : AddressFragmentViewModel(app) {
    private lateinit var currentType: AddressType

    override fun qrClickReaction(activity: FragmentActivity) {
        currentType = model.accountAddress.value!!.type
        currentType = if (currentType == AddressType.P2SH_P2WPKH) {
            AddressType.P2WPKH
        } else {
            AddressType.P2SH_P2WPKH
        }

        if (account is SingleAddressAccount) {
            model.accountAddress.value = (account as SingleAddressAccount).getAddress(currentType)
        } else {
            model.accountAddress.value = (account as HDAccount).getReceivingAddress(currentType)
        }
        model.addressPath.value =
                when (showBip44Path) {
                    true -> model.accountAddress.value!!.bip32Path.toString()
                    false -> ""
                }
    }

}