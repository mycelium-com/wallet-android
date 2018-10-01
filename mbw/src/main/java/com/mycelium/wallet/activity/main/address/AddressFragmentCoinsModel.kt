package com.mycelium.wallet.activity.main.address

import android.app.Activity
import android.app.Application
import android.content.res.Resources
import android.graphics.drawable.Drawable
import com.mrd.bitlib.model.Address
import com.mycelium.wallet.BitcoinUriWithAddress
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import com.mycelium.wallet.activity.receive.ReceiveCoinsActivity
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.bip44.Bip44BCHAccount
import com.mycelium.wapi.wallet.single.SingleAddressBCHAccount

class AddressFragmentCoinsModel(app: Application) : AddressFragmentViewModel(app) {


    override fun init() {
        super.init()
        model = AddressFragmentModel(context, account, false)
    }

    override fun getAccountLabel(): String {
        var name = mbwManager!!.metadataStorage.getLabelByAccount(account.id)
        if (account is SingleAddressBCHAccount || account is Bip44BCHAccount) {
            name = context.getString(R.string.bitcoin_cash) + " - " + name
        }
        return name
    }
    override fun getAccountAddress(): String {
        return account.receivingAddress.get().toString()
    }

    override fun getAddressPath(): String {
        if (showBip44Path && Address.fromString(getAccountAddress()).bip32Path != null) {
            val path = Address.fromString(getAccountAddress()).bip32Path
            return path.toString()
        } else {
            return ""
        }
    }
    override fun qrClickReaction(activity: Activity) {
        if (account.receivingAddress.isPresent) {
            ReceiveCoinsActivity.callMe(activity, account, account.canSpend())
        }
    }
}