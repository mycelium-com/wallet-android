package com.mycelium.wallet.activity.main.address

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.MutableLiveData
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.support.v4.app.FragmentActivity
import android.widget.Toast
import com.mrd.bitlib.model.AddressType
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import com.mycelium.wallet.coinapult.CoinapultAccount

abstract class AddressFragmentViewModel(val context: Application) : AndroidViewModel(context) {
    protected val mbwManager = MbwManager.getInstance(context)!!
    protected lateinit var model: AddressFragmentModel
    protected val showBip44Path: Boolean = mbwManager.metadataStorage.showBip44Path

    open fun init() {
        if (::model.isInitialized) {
            throw IllegalStateException("This method should be called only once.")
        }
        model = AddressFragmentModel(context, mbwManager.selectedAccount, showBip44Path)
    }

    fun getAccountLabel() = model.accountLabel
    fun getAccountAddress() = model.accountAddress
    fun getAddressPath() = model.addressPath
    fun isCompressedKey() = model.isCompressedKey
    fun getAccountAddressType() = model.accountAddressType

    fun getDrawableForAccount(resources: Resources): Drawable? =
            Utils.getDrawableForAccount(model.account, true, resources)

    override fun onCleared() {
        model.onCleared()
    }

    fun addressClick() {
        Utils.setClipboardString(getAddressString(), context)
        Toast.makeText(context, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
    }

    fun getAddressString(): String {
        val addressString = getAccountAddress().value!!.toString()
        return if(mbwManager.selectedAccount is CoinapultAccount) {
            "Coinapult stopped working! Handle with care: " + addressString.chunked(5).joinToString(" ")
        } else { addressString }
    }

    fun isLabelNullOrEmpty() = (getAccountLabel().value == null || getAccountLabel().value!!.toString().equals(""))

    fun isCoinapult() = mbwManager.selectedAccount is CoinapultAccount

    abstract fun qrClickReaction(activity: FragmentActivity)

    fun isInitialized() = ::model.isInitialized
}