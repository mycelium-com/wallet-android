package com.mycelium.wallet.activity.fio.requests.viewmodels

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.WalletApplication
import com.mycelium.wallet.activity.receive.ReceiveCoinsActivity
import com.mycelium.wallet.activity.send.ManualAddressEntry
import com.mycelium.wapi.wallet.btc.bip44.getActiveHDAccounts
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.fio.FioAccount
import com.mycelium.wapi.wallet.fio.FioModule
import com.mycelium.wapi.wallet.fio.RegisteredFIOName
import fiofoundation.io.fiosdk.models.fionetworkprovider.response.PushTransactionResponse

open class FioRequestBtcViewModel() : ViewModel() {


    private var fioModule: FioModule

    val payerFioAddress = MutableLiveData<String>()
    val payeeFioAddress = MutableLiveData<String>()
    val payerTokenPublicAddress = MutableLiveData<String>()
    val payeeTokenPublicAddress = MutableLiveData<String>()
    val payeeFioAddreses = MutableLiveData<List<RegisteredFIOName>>()
    val amount = MutableLiveData<Value>()
    val memo = MutableLiveData<String>()

    init {
        val mbwManager = MbwManager.getInstance(WalletApplication.getInstance())
        fioModule = mbwManager.getWalletManager(false).getModuleById(FioModule.ID) as FioModule
        payeeFioAddreses.value = fioModule.getFIONames(mbwManager.selectedAccount)
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

    open fun processReceivedResults(requestCode: Int, resultCode: Int, data: Intent?, activity: Activity) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == ReceiveCoinsActivity.MANUAL_ENTRY_RESULT_CODE && !data?.getStringExtra(ManualAddressEntry.ADDRESS_RESULT_FIO).isNullOrBlank()) {
                val fioAddress = data?.getStringExtra(ManualAddressEntry.ADDRESS_RESULT_FIO)!!
                val addressResult = data?.getStringExtra(ManualAddressEntry.ADDRESS_RESULT_NAME)!!
                payeeFioAddress.value = fioAddress
                payeeTokenPublicAddress.value = addressResult
            }
        }
    }
}