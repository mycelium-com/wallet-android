package com.mycelium.wallet.activity.main.address

import android.app.Application
import android.support.v4.app.FragmentActivity
import com.google.common.base.Optional
import com.mrd.bitlib.model.AddressType
import com.mycelium.wallet.event.ReceivingAddressChanged
import com.mycelium.wapi.wallet.AbstractAccount

class AddressFragmentBtcModel(val app: Application) : AddressFragmentViewModel(app) {
    private lateinit var currentType: AddressType

    override fun qrClickReaction(activity: FragmentActivity) {
        currentType = model.accountAddress.value!!.type
        currentType = if (currentType == AddressType.P2SH_P2WPKH) {
            AddressType.P2WPKH
        } else {
            AddressType.P2SH_P2WPKH
        }


        (model.account as AbstractAccount).setDefaultAddressType(currentType)

        mbwManager.eventBus.post(ReceivingAddressChanged(Optional.of(model.accountAddress.value!!)))
        model.onAddressChange()
    }

}