package com.mycelium.wallet.activity.receive

import android.app.Application
import android.arch.lifecycle.MutableLiveData
import android.os.Bundle
import com.mrd.bitlib.model.AddressType
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import com.mycelium.wapi.wallet.AbstractAccount
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.btc.WalletBtcAccount
import com.mycelium.wapi.wallet.btc.bip44.HDAccount
import com.mycelium.wapi.wallet.btc.single.SingleAddressAccount
import com.mycelium.wapi.wallet.currency.CurrencyValue

class ReceiveBtcViewModel(application: Application) : ReceiveCoinsViewModel(application) {
    val addressType: MutableLiveData<AddressType> = MutableLiveData()

    override fun init(account: WalletAccount<*,*>, hasPrivateKey: Boolean, showIncomingUtxo: Boolean) {
        super.init(account, hasPrivateKey, showIncomingUtxo)
        model = ReceiveCoinsModel(getApplication(), account, ACCOUNT_LABEL, hasPrivateKey, showIncomingUtxo)
        addressType.value = (account as WalletBtcAccount).receivingAddress.get().type
    }

    fun setAddressType(addressType: AddressType) {
        this.addressType.value = addressType
        model.receivingAddress.value = when (account) {
            is HDAccount -> (account as HDAccount).getReceivingAddress(addressType)!!
            is SingleAddressAccount -> (account as SingleAddressAccount).getAddress(addressType)
            else -> throw IllegalStateException()
        }
        model.updateObservingAddress()
    }

    fun getAccountDefaultAddressType(): AddressType {
        return when (account) {
            is HDAccount -> (account as HDAccount).receivingAddress.get().type
            is SingleAddressAccount -> (account as SingleAddressAccount).address.type
            else -> throw IllegalStateException()
        }
    }

    fun setCurrentAddressTypeAsDefault() {
        (account as AbstractAccount).setDefaultAddressType(addressType.value)
        this.addressType.value = addressType.value // this is required to update UI
    }

    override fun getHint() = context.getString(R.string.amount_hint_denomination,
                mbwManager.bitcoinDenomination.toString())

    override fun getCurrencyName() = context.getString(R.string.bitcoin_name)

    override fun getFormattedValue(sum: CurrencyValue) = Utils.getFormattedValueWithUnit(sum, mbwManager.bitcoinDenomination)

    override fun loadInstance(savedInstanceState: Bundle) {
        setAddressType(savedInstanceState.getSerializable(ADDRESS_TYPE) as AddressType)
        super.loadInstance(savedInstanceState)
    }

    override fun saveInstance(outState: Bundle) {
        outState.putSerializable(ADDRESS_TYPE, addressType.value)
        super.saveInstance(outState)
    }

    override fun getTitle(): String {
        return if (CurrencyValue.isNullOrZero(model.amountData.value)) {
            context.getString(R.string.address_title, context.getString(R.string.bitcoin_name))
        } else {
            context.getString(R.string.payment_request)
        }
    }

    companion object {
        private const val ACCOUNT_LABEL = "bitcoin"
        private const val ADDRESS_TYPE = "addressType"
    }
}