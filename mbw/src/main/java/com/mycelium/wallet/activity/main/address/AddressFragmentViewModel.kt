package com.mycelium.wallet.activity.main.address

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

    open fun init() {
        if (::model.isInitialized) {
            throw IllegalStateException("This method should be called only once.")
        }
        this.account = mbwManager.selectedAccount
    }

    open fun getAccountLabel(): String {
        var name = mbwManager!!.metadataStorage.getLabelByAccount(account.id)
        if (account is SingleAddressBCHAccount || account is Bip44BCHAccount) {
            name = context.getString(R.string.bitcoin_cash) + " - " + name
        }
        return name
    }

    open fun getAccountAddress(): String {
        return account.receivingAddress.get().toString()
    }

    open fun getAccountType(): WalletAccount.Type {
        return account.type
    }

    open fun getAddressPath(): String {
        if (mbwManager.getMetadataStorage().getShowBip44Path() && Address.fromString(getAccountAddress()).bip32Path != null) {
            val path = Address.fromString(getAccountAddress()).bip32Path
            return path.toString()
        } else {
            return ""
        }
    }

    fun getAddressUri() : String{
        val receivingAddress = Address.fromString(getAccountAddress())
        return BitcoinUriWithAddress.fromAddress(receivingAddress).toString()
    }

    fun getDrawableForAccount(resources: Resources) : Drawable {
        return Utils.getDrawableForAccount(account, true, resources)
    }

    open fun qrClickReaction() {}

    fun isInitialized() = ::model.isInitialized
}