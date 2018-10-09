package com.mycelium.wallet.activity.main.address

import android.app.Application
import android.support.v4.app.FragmentActivity
import com.mrd.bitlib.model.AddressType
import com.mycelium.wallet.event.ReceivingAddressChanged
import com.mycelium.wapi.wallet.btc.AbstractBtcAccount
import com.mycelium.wapi.wallet.btc.BtcAddress

class AddressFragmentBtcModel(val app: Application) : AddressFragmentViewModel(app) {
    lateinit var currentType: AddressType

    override fun init() {
        super.init()
        currentType = model.accountAddress.value!!.type
    }

    override fun qrClickReaction(activity: FragmentActivity) {
        currentType = if (currentType == AddressType.P2SH_P2WPKH) {
            AddressType.P2WPKH
        } else {
            AddressType.P2SH_P2WPKH
        }


        (model.account as AbstractBtcAccount).setDefaultAddressType(currentType)

        mbwManager.eventBus.post(ReceivingAddressChanged(model.accountAddress.value!!))
        model.onAddressChange()
    }

}