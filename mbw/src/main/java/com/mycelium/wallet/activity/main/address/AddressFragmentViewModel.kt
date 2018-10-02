package com.mycelium.wallet.activity.main.address

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.support.v4.app.FragmentActivity
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import com.mycelium.wapi.wallet.WalletAccount

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
        model = AddressFragmentModel(context, account, showBip44Path)
    }

    fun getAccountLabel() = model.accountLabel
    fun getAccountAddress() = model.accountAddress
    fun getAddressPath() = model.addressPath

    fun getDrawableForAccount(resources: Resources): Drawable {
        return Utils.getDrawableForAccount(account, true, resources)
    }

    fun addressClick() {
        Utils.setClipboardString(getAccountAddress().value!!.toString(), context)
        Toast.makeText(context, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
    }

    fun isLabelNullOrEmpty() = (getAccountLabel().value == null || getAccountLabel().value.equals(""))

    abstract fun qrClickReaction(activity: FragmentActivity)

    fun isInitialized() = ::model.isInitialized
}