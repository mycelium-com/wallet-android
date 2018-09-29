package com.mycelium.wallet.activity.main.address

import android.app.Application
import com.mrd.bitlib.model.Address
import com.mrd.bitlib.model.AddressType
import com.mycelium.wallet.coinapult.CoinapultAccount
import com.mycelium.wallet.event.HdAccountCreated
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.bip44.HDAccount
import com.mycelium.wapi.wallet.single.SingleAddressAccount

class AddressFragmentBtcModel(val app: Application) : AddressFragmentViewModel(app) {

    var addresses = mutableListOf<AddressType>()
    private var position = 0

    override fun init() {
        super.init()
        addresses.addAll(AddressType.values())
        addresses.remove(AddressType.P2PKH)
        position = 0
    }

    override fun getAccountType(): WalletAccount.Type {
        if (account is HDAccount) {
            return WalletAccount.Type.BTCBIP44
        }
        return WalletAccount.Type.BTCSINGLEADDRESS
    }


    override fun getAccountAddress(): String {
        if (account is SingleAddressAccount) {
            return (account as SingleAddressAccount).getAddress(addresses[position]).toString()
        } else {
            return (account as HDAccount).getReceivingAddress(addresses[position]).toString()
        }
    }

    override fun qrClickReaction() {
        position = (position + 1) % (2)
    }

}