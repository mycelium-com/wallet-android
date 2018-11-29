package com.mycelium.wallet.activity.receive

import android.app.Application
import android.arch.lifecycle.MutableLiveData
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import com.mrd.bitlib.model.AddressType
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import com.mycelium.wapi.wallet.btc.AbstractBtcAccount
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.btc.WalletBtcAccount
import com.mycelium.wapi.wallet.btc.bip44.HDAccount
import com.mycelium.wapi.wallet.btc.single.SingleAddressAccount
import com.mycelium.wapi.wallet.coins.Value

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
            is HDAccount, is SingleAddressAccount -> account.receiveAddress
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
        (account as AbstractBtcAccount).setDefaultAddressType(addressType.value)
        this.addressType.value = addressType.value // this is required to update UI
    }

    override fun getHint() = context.getString(R.string.amount_hint_denomination,
                mbwManager.bitcoinDenomination.toString())

    override fun getCurrencyName() = context.getString(R.string.bitcoin_name)

    override fun getFormattedValue(sum: Value) = Utils.getFormattedValue(sum, mbwManager.bitcoinDenomination)

    fun showAddressTypesInfo(activity: AppCompatActivity) {
        // building message based on networking preferences
        val dialogMessage = if (mbwManager.network.isProdnet) {
            activity.getString(R.string.what_is_address_type_description, "1", "3", "bc1")
        } else {
            activity.getString(R.string.what_is_address_type_description, "m or n", "2", "tb1")
        }

        val dialog = AlertDialog.Builder(activity, R.style.MyceliumModern_Dialog)
                .setTitle(activity.resources.getString(R.string.what_is_address_type))
                .setMessage(dialogMessage)
                .setPositiveButton(R.string.button_ok, null)
                .create()

        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(activity.resources.getColor(R.color.mycelium_midblue))
    }

    override fun loadInstance(savedInstanceState: Bundle) {
        setAddressType(savedInstanceState.getSerializable(ADDRESS_TYPE) as AddressType)
        super.loadInstance(savedInstanceState)
    }

    override fun saveInstance(outState: Bundle) {
        outState.putSerializable(ADDRESS_TYPE, addressType.value)
        super.saveInstance(outState)
    }

    override fun getTitle(): String {
        return if (Value.isNullOrZero(model.amountData.value)) {
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