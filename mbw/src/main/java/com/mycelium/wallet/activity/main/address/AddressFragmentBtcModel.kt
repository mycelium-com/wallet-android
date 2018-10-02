package com.mycelium.wallet.activity.main.address

import android.app.Application
import com.mrd.bitlib.model.Address
import com.mrd.bitlib.model.AddressType
import com.mycelium.wapi.wallet.bip44.HDAccount
import com.mycelium.wapi.wallet.single.SingleAddressAccount

class AddressFragmentBtcModel(val app: Application) : AddressFragmentViewModel(app) {

    var currentType = AddressType.P2WPKH

    override fun qrClickReaction() {
        currentType = if (currentType == AddressType.P2SH_P2WPKH) {
            AddressType.P2WPKH
        } else {
            AddressType.P2SH_P2WPKH
        }

        if (account is SingleAddressAccount) {
            model.accountAddress.value = (account as SingleAddressAccount).getAddress(currentType).toString()
        } else {
            model.accountAddress.value = (account as HDAccount).getReceivingAddress(currentType).toString()
        }
        model.addressPath.value =
                when (showBip44Path) {
                    true -> Address.fromString(model.accountAddress.value).bip32Path.toString()
                    false -> ""
                }

    }

}