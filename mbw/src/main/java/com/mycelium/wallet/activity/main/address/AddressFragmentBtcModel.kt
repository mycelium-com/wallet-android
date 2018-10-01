package com.mycelium.wallet.activity.main.address

import android.app.Activity
import android.app.Application
import com.mrd.bitlib.model.Address
import com.mrd.bitlib.model.AddressType
import com.mycelium.wallet.coinapult.CoinapultAccount
import com.mycelium.wallet.event.HdAccountCreated
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.bip44.HDAccount
import com.mycelium.wapi.wallet.single.SingleAddressAccount

class AddressFragmentBtcModel(val app: Application) : AddressFragmentViewModel(app) {


    override fun init() {
        super.init()
        model = AddressFragmentModel(context, account, true)
    }

    override fun getAccountAddress(): String {
        return model.accountAddress.value.toString()
    }

    override fun getAccountLabel(): String {
        return model.accountLabel.value!!
    }

    override fun qrClickReaction(activity: Activity) {
        model.changePosition()
    }

}