package com.mycelium.wallet.activity.main.address

import android.app.Application
import android.arch.lifecycle.MutableLiveData
import com.mrd.bitlib.model.AddressType
import com.mrd.bitlib.model.hdpath.HdKeyPath
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.event.AccountChanged
import com.mycelium.wallet.event.ReceivingAddressChanged
import com.mycelium.wapi.wallet.GenericAddress
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.bch.bip44.Bip44BCHAccount
import com.mycelium.wapi.wallet.bch.single.SingleAddressBCHAccount
import com.mycelium.wapi.wallet.btc.WalletBtcAccount
import com.squareup.otto.Subscribe

class AddressFragmentModel(
        val context: Application,
        var account: WalletAccount<*,*>,
        val showBip44Path: Boolean
) {
    private var mbwManager: MbwManager = MbwManager.getInstance(context)
    val accountLabel: MutableLiveData<String> = MutableLiveData()
    val accountAddress: MutableLiveData<GenericAddress> = MutableLiveData()
    val addressPath: MutableLiveData<String> = MutableLiveData()
    var isUncompressedKey: Boolean = false
    val type: MutableLiveData<AddressType> = MutableLiveData()
    val bip32Path: MutableLiveData<HdKeyPath> = MutableLiveData()

    init {
        updateLabel()
        onAddressChange()

        MbwManager.getEventBus().register(this)
    }

    private fun updateAddressPath(showBip44Path: Boolean) {
        addressPath.value = if (showBip44Path && bip32Path.value != null) {
            bip32Path.value.toString()
        } else {
            ""
        }
    }

    private fun updateLabel() {
        val label = mbwManager.metadataStorage.getLabelByAccount(account.id)
        isUncompressedKey = !((account as SingleAddressBCHAccount?)?.publicKey?.isCompressed ?: true)
        accountLabel.value = when (account) {
            is Bip44BCHAccount,
            is SingleAddressBCHAccount ->
                context.getString(R.string.bitcoin_cash) + " - " + label
            else -> label
        }
    }

    private fun updateAddress(account: WalletAccount<*,*>) {
        if(account is WalletBtcAccount && account.receivingAddress.isPresent) {
            bip32Path.value = account.receivingAddress.get().bip32Path
            type.value = account.receivingAddress.get().type
        }
        accountAddress.value = account.receiveAddress
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