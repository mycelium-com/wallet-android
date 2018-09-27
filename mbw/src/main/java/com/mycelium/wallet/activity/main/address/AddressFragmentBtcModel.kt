package com.mycelium.wallet.activity.main.address

import android.app.Application
import com.mrd.bitlib.model.Address
import com.mrd.bitlib.model.AddressType
import com.mycelium.wallet.event.HdAccountCreated
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.bip44.HDAccount
import com.mycelium.wapi.wallet.single.SingleAddressAccount

class AddressFragmentBtcModel(val app: Application) : AddressFragmentViewModel(app) {

    val addresses: List<AddressType> = AddressType.values().toList()
    private var position = 0

    override fun init(acc: WalletAccount) {
        super.init(acc)
        position = 0
    }

    override fun getAccountType(): WalletAccount.Type {
        if(account is HDAccount){
            return WalletAccount.Type.BTCBIP44
        }
        return WalletAccount.Type.BTCSINGLEADDRESS
    }

    override fun getAddressPath(): String {
        if(account is SingleAddressAccount){
            return (account as SingleAddressAccount).getAddress(addresses[position]).bip32Path.toString()
        } else {
            (account as HDAccount).allAddresses.forEach {
                if (it.type == addresses[position]) {
                    return it.bip32Path.toString()
                }
            }
        }
        return ""
    }

    override fun getAccountAddress(): String {
        if(account is SingleAddressAccount){
            return (account as SingleAddressAccount).getAddress(addresses[position]).shortAddress
        } else {
            (account as HDAccount).allAddresses.forEach {
                if (it.type == addresses[position]) {
                    return it.shortAddress
                }
            }
        }
        return ""
    }

    override fun qrClickReaction() {
        position = (position + 1) % AddressType.values().size
    }

}