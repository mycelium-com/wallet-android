package com.mycelium.wallet.activity.main.address

import android.app.Application
import android.arch.lifecycle.MutableLiveData
import android.support.v4.app.FragmentActivity
import asShortStringRes
import com.google.common.base.Optional
import com.mrd.bitlib.model.AddressType
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.event.ReceivingAddressChanged
import com.mycelium.wapi.wallet.AbstractAccount

class AddressFragmentBtcModel(val app: Application) : AddressFragmentViewModel(app) {
    lateinit var currentType: AddressType
    var nextTypeLabel: MutableLiveData<String> = MutableLiveData()

    override fun init() {
        super.init()
        currentType = model.accountAddress.value!!.type
        setNextLabel()
    }

    override fun qrClickReaction(activity: FragmentActivity) {
        currentType = getNextType()
        setNextLabel()

        (model.account as AbstractAccount).setDefaultAddressType(currentType)

        MbwManager.getEventBus().post(ReceivingAddressChanged(Optional.of(model.accountAddress.value!!)))
        model.onAddressChange()
    }

    private fun setNextLabel() {
        val nextTypeShort = app.getString(getNextType().asShortStringRes())
        nextTypeLabel.value = app.getString(R.string.tap_next, nextTypeShort)
    }

    private fun getNextType(): AddressType {
        val addressTypes = (model.account as AbstractAccount).availableAddressTypes
        val currentAddressTypeIndex = addressTypes.lastIndexOf(currentType)
        return addressTypes[(currentAddressTypeIndex + 1) % addressTypes.size]
    }
}
