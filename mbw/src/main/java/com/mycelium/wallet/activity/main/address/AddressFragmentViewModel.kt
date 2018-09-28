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

    abstract fun getAccountAddress(): String

    abstract fun getAccountType() : WalletAccount.Type

    fun getAccountLabel() : String {
        return mbwManager.metadataStorage.getLabelByAccount(account.id)
    }
    abstract fun getAddressPath() : String

    abstract fun qrClickReaction()

    fun isInitialized() = ::model.isInitialized
}