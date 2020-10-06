package com.mycelium.wallet.activity.fio.requests.viewmodels

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.WalletApplication
import com.mycelium.wallet.activity.GetAmountActivity
import com.mycelium.wallet.activity.receive.ReceiveCoinsActivity
import com.mycelium.wallet.activity.receive.ReceiveCoinsViewModel
import com.mycelium.wallet.activity.send.ManualAddressEntry
import com.mycelium.wapi.wallet.Address
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.btc.bip44.getBTCBip44Accounts
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.fio.FioAccount
import com.mycelium.wapi.wallet.fio.FioModule
import com.mycelium.wapi.wallet.fio.RegisteredFIOName
import fiofoundation.io.fiosdk.models.fionetworkprovider.response.PushTransactionResponse

open class FioRequestCreateViewModel() : ViewModel() {


    private var fioModule: FioModule

    val mbwManager = MbwManager.getInstance(WalletApplication.getInstance())
    val payerFioAddress = MutableLiveData<String>()
    val payeeFioAddress = MutableLiveData<String>()
    val payerTokenPublicAddress = MutableLiveData<String>()
    val payeeTokenPublicAddress = MutableLiveData<String>()
    val payeeFioAddreses = MutableLiveData<List<RegisteredFIOName>>()
    val payeeAccount = MutableLiveData<WalletAccount<*>>()
    val amount = MutableLiveData<Value>()
    val memo = MutableLiveData<String>()

    init {
        val walletManager = mbwManager.getWalletManager(false)
        fioModule = walletManager.getModuleById(FioModule.ID) as FioModule
        val account = mbwManager.selectedAccount
        val fioNames = fioModule.getFIONames(account)
        if (fioNames.isEmpty()) {
            TODO("Handle case when account to registered")
        }
        payeeAccount.value = account
        payeeFioAddreses.value = fioNames
        payeeFioAddress.value = fioNames[0].name
    }


    fun sendRequest(context: Context): PushTransactionResponse {
        val fioAccount = MbwManager.getInstance(context).selectedAccount as FioAccount
        val transferTokensFee = fioAccount.getTransferTokensFee()
        return fioAccount.requestFunds(
                payerTokenPublicAddress.value!!,
                payeeFioAddress.value!!,
                payeeTokenPublicAddress.value!!,
                amount.value?.value?.toDouble()!!,
                "",
                "",
                transferTokensFee)
    }

    fun getPayeeFioAddreses(): List<RegisteredFIOName>? {
        return payeeFioAddreses.value
    }

    open fun processReceivedResults(requestCode: Int, resultCode: Int, data: Intent?, activity: Activity) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == ReceiveCoinsViewModel.GET_AMOUNT_RESULT_CODE) {
                // Get result from address chooser (may be null)
                amount.value = data?.getSerializableExtra(GetAmountActivity.AMOUNT) as Value?
            } else if (requestCode == ReceiveCoinsActivity.MANUAL_ENTRY_RESULT_CODE && !data?.getStringExtra(ManualAddressEntry.ADDRESS_RESULT_FIO).isNullOrBlank()) {
                val fioAddress = data?.getStringExtra(ManualAddressEntry.ADDRESS_RESULT_FIO)!!
                val addressResult = data?.getSerializableExtra(ManualAddressEntry.ADDRESS_RESULT_NAME) as Address
                payerFioAddress.value = fioAddress
                payerTokenPublicAddress.value = addressResult.toString()
            }
        }
    }
}