package com.mycelium.wallet.activity.main.address

import android.app.Application
import android.arch.lifecycle.MutableLiveData
import com.mrd.bitlib.model.Address
import com.mrd.bitlib.model.AddressType
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.bip44.Bip44BCHAccount
import com.mycelium.wapi.wallet.bip44.HDAccount
import com.mycelium.wapi.wallet.single.SingleAddressAccount
import com.mycelium.wapi.wallet.single.SingleAddressBCHAccount

class AddressFragmentModel(
        val context: Application,
        val account: WalletAccount,
        val showBip44Path: Boolean
) {
    private var mbwManager: MbwManager = MbwManager.getInstance(context)
    val accountLabel: MutableLiveData<String> = MutableLiveData()
    val accountAddress: MutableLiveData<Address> = MutableLiveData()
    val addressPath: MutableLiveData<String> = MutableLiveData()

    init {
        val label = mbwManager.metadataStorage.getLabelByAccount(account.id)
        accountLabel.value =
                when (account) {
                    is Bip44BCHAccount, is SingleAddressBCHAccount ->
                        context.getString(R.string.bitcoin_cash) + " - " + label
                    else -> label
                }

        val defaultAddressType = mbwManager.defaultAddressType
        accountAddress.value =
                when (account) {
                    is SingleAddressAccount ->
                        account.getAddress(defaultAddressType)
                    is HDAccount ->
                        account.getReceivingAddress(defaultAddressType)
                    else -> account.receivingAddress.get()
                }

        addressPath.value =
                when (showBip44Path && accountAddress.value!!.bip32Path != null) {
                    true -> accountAddress.value!!.bip32Path.toString()
                    false -> ""
                }
    }
}