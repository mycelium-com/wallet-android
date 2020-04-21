package com.mycelium.wallet.activity.main.address

import android.app.Application
import androidx.lifecycle.MutableLiveData
import android.text.Html
import android.text.Spanned
import asStringRes
import com.mrd.bitlib.model.AddressType
import com.mrd.bitlib.model.hdpath.HdKeyPath
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.event.AccountChanged
import com.mycelium.wallet.event.ReceivingAddressChanged
import com.mycelium.wapi.wallet.Address
import com.mycelium.wapi.wallet.SyncMode
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.bch.bip44.Bip44BCHAccount
import com.mycelium.wapi.wallet.bch.single.SingleAddressBCHAccount
import com.mycelium.wapi.wallet.btc.WalletBtcAccount
import com.mycelium.wapi.wallet.btc.single.SingleAddressAccount
import com.mycelium.wapi.wallet.eth.EthAccount
import com.mycelium.wapi.wallet.eth.EthereumModule
import com.squareup.otto.Subscribe

class AddressFragmentModel(
        val context: Application,
        var account: WalletAccount<*>,
        private val showBip44Path: Boolean
) {
    private var mbwManager: MbwManager = MbwManager.getInstance(context)
    val accountLabel: MutableLiveData<Spanned> = MutableLiveData()
    val accountAddress: MutableLiveData<Address> = MutableLiveData()
    val addressPath: MutableLiveData<String> = MutableLiveData()
    val type: MutableLiveData<AddressType> = MutableLiveData()
    private val bip32Path: MutableLiveData<HdKeyPath> = MutableLiveData()
    var isCompressedKey: Boolean = true
    val accountAddressType: MutableLiveData<String> = MutableLiveData()

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
        val acc = account
        isCompressedKey = !(acc is SingleAddressAccount && acc.publicKey?.isCompressed == false)
        // Deprecated but not resolvable until we stop supporting API <24
        accountLabel.value = Html.fromHtml(when (account) {
            is Bip44BCHAccount,
            is SingleAddressBCHAccount ->
                context.getString(R.string.bitcoin_cash) + " - " + label
            else -> label
        })
    }

    private fun updateAddress(account: WalletAccount<*>) {
        if (account is WalletBtcAccount) {
            account.receivingAddress.orNull()?.let { address ->
                bip32Path.value = address.bip32Path
                type.value = address.type
                accountAddressType.value = context.getString(address.type.asStringRes())
            }
        } else if (account is EthAccount) {
            val module = mbwManager.getWalletManager(false).getModuleById(EthereumModule.ID) as EthereumModule
            bip32Path.value = module.getBip44Path(account.accountIndex)
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
        mbwManager.getWalletManager(false).run {
            if (hasAccount(event.account)) {
                startSynchronization(
                        SyncMode.FULL_SYNC_CURRENT_ACCOUNT_FORCED, listOf(getAccount(event.account) ?: return@run))
            }
        }
    }

    fun onAddressChange() {
        updateAddress(account)
        updateAddressPath(showBip44Path)
    }
}