package com.mycelium.wallet.activity.main.address

import android.app.Activity
import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Html
import android.view.View
import com.mrd.bitlib.model.Address
import com.mycelium.wallet.BitcoinUriWithAddress
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.bip44.Bip44BCHAccount
import com.mycelium.wapi.wallet.single.SingleAddressBCHAccount
import kotlinx.android.synthetic.main.address_fragment_qr.*

abstract class AddressFragmentViewModel(val context: Application) : AndroidViewModel(context) {
    protected val mbwManager = MbwManager.getInstance(context)!!
    protected lateinit var model: AddressFragmentModel
    protected lateinit var account: WalletAccount
    protected val showBip44Path: Boolean = mbwManager.metadataStorage.showBip44Path

    open fun init() {
        if (::model.isInitialized) {
            throw IllegalStateException("This method should be called only once.")
        }
        this.account = mbwManager.selectedAccount
    }

    abstract fun getAccountLabel(): String

    abstract fun getAccountAddress(): String

    open fun getAddressPath(): String {
        if (showBip44Path && model.addressPath.value != null) {
            return model.addressPath.value!!
        } else {
            return ""
        }
    }

    fun getAddressUri() : String{
        return BitcoinUriWithAddress.fromAddress(model.accountAddress.value).toString()
    }

    fun getDrawableForAccount(resources: Resources) : Drawable {
        return Utils.getDrawableForAccount(account, true, resources)
    }

    open fun qrClickReaction(activity: Activity) {}

    fun isInitialized() = ::model.isInitialized
}