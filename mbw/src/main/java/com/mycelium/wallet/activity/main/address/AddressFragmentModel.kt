package com.mycelium.wallet.activity.main.address

import android.app.Application
import android.arch.lifecycle.MutableLiveData
import android.os.Bundle
import android.view.View
import android.widget.Toast
import butterknife.BindView
import com.google.common.base.Optional
import com.mrd.bitlib.model.Address
import com.mrd.bitlib.model.AddressType
import com.mrd.bitlib.util.CoinUtil
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.receive.ReceiveCoinsModel
import com.mycelium.wallet.activity.util.AccountDisplayType
import com.mycelium.wallet.activity.util.QrImageView
import com.mycelium.wallet.event.HdAccountCreated
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.bip44.Bip44BCHAccount
import com.mycelium.wapi.wallet.bip44.HDAccount
import com.mycelium.wapi.wallet.currency.CurrencyValue
import com.mycelium.wapi.wallet.currency.ExchangeBasedCurrencyValue
import com.mycelium.wapi.wallet.single.SingleAddressAccount
import com.mycelium.wapi.wallet.single.SingleAddressBCHAccount
import com.squareup.otto.Bus

class AddressFragmentModel(
        val context: Application,
        val account: WalletAccount,
        val showBip44Path: Boolean
) {
    private var mbwManager: MbwManager = MbwManager.getInstance(context)
    val accountLabel: MutableLiveData<String> = MutableLiveData()
    val accountAddress: MutableLiveData<String> = MutableLiveData()
    val addressPath : MutableLiveData<String> = MutableLiveData()
    var position = 0

    init {
        val label = mbwManager.metadataStorage.getLabelByAccount(account.id)
        accountLabel.value =
                when (account) {
                    is Bip44BCHAccount, is SingleAddressBCHAccount ->
                        context.getString(R.string.bitcoin_cash) + " - " + label
                    else -> label
                }

        accountAddress.value =
                when (account) {
                    is SingleAddressAccount ->
                        account.getAddress(AddressType.P2WPKH).toString()
                    is HDAccount ->
                        account.getReceivingAddress(AddressType.P2WPKH).toString()
                    else -> account.receivingAddress.get().toString()
                }
        addressPath.value =
                when(showBip44Path){
                    true -> Address.fromString(accountAddress.value).bip32Path.toString()
                    false -> ""
                }
    }
}