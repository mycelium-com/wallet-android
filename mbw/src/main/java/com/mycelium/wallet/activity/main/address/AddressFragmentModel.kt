package com.mycelium.wallet.activity.main.address

import android.app.Application
import android.arch.lifecycle.MutableLiveData
import com.mrd.bitlib.model.Address
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.event.AccountChanged
import com.mycelium.wallet.event.ReceivingAddressChanged
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.bip44.Bip44BCHAccount
import com.mycelium.wapi.wallet.bip44.HDAccount
import com.mycelium.wapi.wallet.single.SingleAddressAccount
import com.mycelium.wapi.wallet.single.SingleAddressBCHAccount
import com.squareup.otto.Subscribe

class AddressFragmentModel(
        val context: Application,
        var account: WalletAccount,
        val showBip44Path: Boolean
) {
    private var mbwManager: MbwManager = MbwManager.getInstance(context)
    val accountLabel: MutableLiveData<String> = MutableLiveData()
    val accountAddress: MutableLiveData<Address> = MutableLiveData()
    val addressPath: MutableLiveData<String> = MutableLiveData()

    init {
        updateLabel()
        onAddressChange()

        mbwManager.eventBus.register(this)
    }

    private fun updateAddressPath(showBip44Path: Boolean) {
        addressPath.value =
                when (showBip44Path && accountAddress.value!!.bip32Path != null) {
                    true -> accountAddress.value!!.bip32Path.toString()
                    false -> ""
                }
    }

    private fun updateLabel() {
        val label = mbwManager.metadataStorage.getLabelByAccount(account.id)
        accountLabel.value =
                when (account) {
                    is Bip44BCHAccount, is SingleAddressBCHAccount ->
                        context.getString(R.string.bitcoin_cash) + " - " + label
                    else -> label
                }
    }

    private fun updateAddress(account: WalletAccount) {
        val defaultAddressType = mbwManager.defaultAddressType
        accountAddress.value =
                when (account) {
                    is SingleAddressAccount -> account.getAddress(defaultAddressType)
                    is HDAccount -> account.getReceivingAddress(defaultAddressType)
                    else -> account.receivingAddress.get()
                }
    }

    fun onCleared() {
        mbwManager.eventBus.unregister(this)
    }

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

    private fun onAddressChange() {
        updateAddress(account)
        updateAddressPath(showBip44Path)
    }
}