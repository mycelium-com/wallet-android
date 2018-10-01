package com.mycelium.wallet.activity.main.address

import android.app.Application
import com.mrd.bitlib.model.Address
import com.mrd.bitlib.model.AddressType
import com.mycelium.wallet.activity.util.QrImageView
import com.mycelium.wapi.wallet.bip44.HDAccount
import com.mycelium.wapi.wallet.single.SingleAddressAccount

class AddressFragmentBtcModel(val app: Application) : AddressFragmentViewModel(app) {

    override fun qrClickReaction() {
        val addresses = mutableListOf<AddressType>()
        addresses.add(AddressType.P2WPKH)
        addresses.add(AddressType.P2SH_P2WPKH)

        model.position = (model.position + 1) % addresses.size

        if (account is SingleAddressAccount) {
            model.accountAddress.value = (account as SingleAddressAccount).getAddress(addresses[model.position]).toString()
        } else {
            model.accountAddress.value = (account as HDAccount).getReceivingAddress(addresses[model.position]).toString()
        }
        model.addressPath.value =
                when(showBip44Path){
                    true -> Address.fromString(model.accountAddress.value).bip32Path.toString()
                    false -> ""
                }
    }

}