package com.mycelium.wallet.activity.main.address

import android.app.Application
import android.support.v4.app.FragmentActivity
import com.mrd.bitlib.model.AddressType
import com.mycelium.wapi.wallet.btc.bip44.HDAccount
import com.mycelium.wapi.wallet.btc.single.SingleAddressAccount

class AddressFragmentBtcModel(val app: Application) : AddressFragmentViewModel(app) {
    private lateinit var currentType: AddressType

    override fun qrClickReaction(activity: FragmentActivity) {
        currentType = model.type.value!!
        currentType = if (currentType == AddressType.P2SH_P2WPKH) {
            AddressType.P2WPKH
        } else {
            AddressType.P2SH_P2WPKH
        }

        if (model.account is SingleAddressAccount) {
            (model.account as SingleAddressAccount).setDefaultAddressType(currentType)
        } else {
            (model.account as HDAccount).setDefaultAddressType(currentType)
        }
        model.onAddressChange()
    }

}