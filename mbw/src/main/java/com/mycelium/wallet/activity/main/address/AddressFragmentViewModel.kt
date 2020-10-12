package com.mycelium.wallet.activity.main.address

import android.app.Application
import android.content.Intent
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.AndroidViewModel
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import com.mycelium.wallet.activity.fio.registername.RegisterFioNameActivity

abstract class AddressFragmentViewModel(val context: Application) : AndroidViewModel(context) {
    protected val mbwManager = MbwManager.getInstance(context)
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
    fun getType() = model.type
    fun getAccountAddressType() = model.accountAddressType
    fun getAccount() = model.account
    fun getRegisteredFIONames() = model.registeredFIONames

    fun getDrawableForAccount(resources: Resources): Drawable? =
            Utils.getDrawableForAccount(model.account, true, resources)

    override fun onCleared() {
        model.onCleared()
    }

    fun addressClick() {
        Utils.setClipboardString(getAddressString(), context)
        Toast.makeText(context, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
    }

    fun getAddressString(): String = getAccountAddress().value!!.toString()

    fun isLabelNullOrEmpty() = (getAccountLabel().value == null || getAccountLabel().value!!.toString().equals(""))

    abstract fun qrClickReaction(activity: FragmentActivity)

    fun isInitialized() = ::model.isInitialized

    fun startRegisterFIONameActivity(activity: FragmentActivity) {
        val intent: Intent = Intent(activity, RegisterFioNameActivity::class.java)
                .putExtra("account", model.account.id)
        activity.startActivity(intent)
    }
}