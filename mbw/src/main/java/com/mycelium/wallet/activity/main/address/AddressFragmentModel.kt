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
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.bip44.HDAccount
import com.mycelium.wapi.wallet.currency.CurrencyValue
import com.mycelium.wapi.wallet.currency.ExchangeBasedCurrencyValue
import com.mycelium.wapi.wallet.single.SingleAddressAccount
import com.squareup.otto.Bus

class AddressFragmentModel(
        val context: Application,
        val account: WalletAccount,
        val isBtc: Boolean
) {
    private var mbwManager: MbwManager = MbwManager.getInstance(context)
    val accountLabel: MutableLiveData<String> = MutableLiveData()
    val accountAddress: MutableLiveData<Address> = MutableLiveData()
    val addressPath : MutableLiveData<String> = MutableLiveData()
    private var position = 0

    init {
        accountLabel.value = mbwManager.metadataStorage.getLabelByAccount(account.id)
        accountAddress.value = account.receivingAddress.get()
        addressPath.value = accountAddress.value!!.bip32Path.toString()
    }

    fun changePosition() {
        if(isBtc) {
            val addresses = mutableListOf<AddressType>()
            addresses.add(AddressType.P2WPKH)
            addresses.add(AddressType.P2SH_P2WPKH)

            position = (position + 1) % addresses.size

            if(account is SingleAddressAccount){
                accountAddress.value = account.getAddress(addresses[position])
                addressPath.value = accountAddress.value!!.bip32Path.toString()
            }
        }

    }

}