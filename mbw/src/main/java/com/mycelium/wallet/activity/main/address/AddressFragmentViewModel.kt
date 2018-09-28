package com.mycelium.wallet.activity.main.address

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.os.Bundle
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.colu.json.Asset
import com.mycelium.wapi.wallet.WalletAccount
import com.satoshilabs.trezor.protobuf.TrezorType

abstract class AddressFragmentViewModel(val context: Application) : AndroidViewModel(context) {
    protected val mbwManager = MbwManager.getInstance(context)!!
    protected lateinit var model: AddressFragmentModel
    protected lateinit var account: WalletAccount

    open fun init(acc: WalletAccount) {
        if (::model.isInitialized) {
            throw IllegalStateException("This method should be called only once.")
        }
        this.account = acc
    }

    fun getAccountLabel(): String {
        return mbwManager.metadataStorage.getLabelByAccount(account.id)
    }

    fun getAccountAddress(): String {
        return account.receivingAddress.get().toString()
    }

    fun getAccountType(): WalletAccount.Type {
        return account.type
    }

    fun getAddressPath(): String {
        return account.receivingAddress.get().bip32Path.toString()
    }

    fun qrClickReaction() {}

    fun isInitialized() = ::model.isInitialized
}