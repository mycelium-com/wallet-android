package com.mycelium.wallet.activity.fio.requests.viewmodels

import android.app.Activity
import android.app.Application
import android.view.View
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mycelium.wallet.activity.send.model.SendCoinsViewModel
import com.mycelium.wallet.activity.util.FeeFormatter
import com.mycelium.wapi.wallet.fio.FioAccount
import fiofoundation.io.fiosdk.models.fionetworkprovider.response.PushTransactionResponse
import java.util.regex.Pattern

open class FioRequestBtcViewModel() : ViewModel() {


    val payeeFioAddress = MutableLiveData<String>()
    val payerTokenPublicAddress = MutableLiveData<String>()
    val payeeTokenPublicAddress = MutableLiveData<String>()
    val amount = MutableLiveData<String>()

    fun sendRequest(fioAccount: FioAccount): PushTransactionResponse {
        val transferTokensFee = fioAccount.getTransferTokensFee()
        return fioAccount.requestFunds(
                payerTokenPublicAddress.value!!,
                payeeFioAddress.value!!,
                payeeTokenPublicAddress.value!!,
                amount.value?.toDouble()!!,
                "",
                "",
                transferTokensFee)
    }

}