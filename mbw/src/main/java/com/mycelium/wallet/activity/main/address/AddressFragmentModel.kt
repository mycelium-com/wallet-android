package com.mycelium.wallet.activity.main.address

import android.app.Application
import android.arch.lifecycle.MutableLiveData
import android.text.Html
import android.text.Spanned
import com.mrd.bitlib.model.Address
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.event.AccountChanged
import com.mycelium.wallet.event.ReceivingAddressChanged
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.bip44.Bip44BCHAccount
import com.mycelium.wapi.wallet.single.SingleAddressAccount
import com.mycelium.wapi.wallet.single.SingleAddressBCHAccount
import com.squareup.otto.Subscribe
import asStringRes

class AddressFragmentModel(
        val context: Application,
        var account: WalletAccount,
        val showBip44Path: Boolean
) {
    private var mbwManager: MbwManager = MbwManager.getInstance(context)
    val accountLabel: MutableLiveData<Spanned> = MutableLiveData()
    val accountAddress: MutableLiveData<Address> = MutableLiveData()
    val addressPath: MutableLiveData<String> = MutableLiveData()
    var isCompressedKey: Boolean = true
    val accountAddressType: MutableLiveData<String> = MutableLiveData()

    init {
        updateLabel()
        onAddressChange()

        MbwManager.getEventBus().register(this)
    }

    private fun updateAddressPath(showBip44Path: Boolean) {
        addressPath.value =
                if (showBip44Path && accountAddress.value!!.bip32Path != null) {
                    accountAddress.value!!.bip32Path.toString()
                } else {
                    ""
                }
    }

    private fun updateLabel() {
        val label = mbwManager.metadataStorage.getLabelByAccount(account.id)
        val acc = account
        isCompressedKey = !(acc is SingleAddressAccount && acc.publicKey?.isCompressed == false)
        accountLabel.value =
                Html.fromHtml(if (account is Bip44BCHAccount || account is SingleAddressBCHAccount) {
                    context.getString(R.string.bitcoin_cash) + " - " + label
                } else {
                    label
                })
    }

    private fun updateAddress(account: WalletAccount) {
        account.receivingAddress.orNull()?.let { address ->
            accountAddress.value = address
            accountAddressType.value = context.getString(address.type.asStringRes())
        }
    }

    fun onCleared() = MbwManager.getEventBus().unregister(this)

    /**
     * We got a new Receiving Address, either because the selected Account changed,
     * or because our HD Account received Coins and changed the Address
     */
    @Subscribe
    fun receivingAddressChanged(event: ReceivingAddressChanged) {
        onAddressChange()
    }

    @Subscribe
    fun accountChanged(event: AccountChanged) {
        account = mbwManager.selectedAccount
        updateLabel()
        onAddressChange()
    }

    fun onAddressChange() {
        updateAddress(account)
        updateAddressPath(showBip44Path)
    }
}