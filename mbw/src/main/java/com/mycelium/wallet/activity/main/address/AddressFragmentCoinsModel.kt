package com.mycelium.wallet.activity.main.address

import android.app.Application
import com.mrd.bitlib.model.Address
import com.mycelium.wapi.wallet.WalletAccount

class AddressFragmentCoinsModel(app: Application): AddressFragmentViewModel(app) {

    override fun getAccountAddress(): String {
        return account.receivingAddress.get().toString()
    }

    override fun getAccountType(): WalletAccount.Type {
        return account.type
    }

    override fun getAddressPath(): String {
        return account.receivingAddress.get().bip32Path.toString()
    }

    override fun qrClickReaction() { }

}