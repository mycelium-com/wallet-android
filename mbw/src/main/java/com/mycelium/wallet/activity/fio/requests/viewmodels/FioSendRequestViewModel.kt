package com.mycelium.wallet.activity.fio.requests.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mycelium.wallet.Utils
import com.mycelium.wapi.wallet.Util
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.coins.Value
import fiofoundation.io.fiosdk.models.fionetworkprovider.FIORequestContent

class FioSendRequestViewModel : ViewModel() {
    val TX_ID_SIZE = 64
    val OBT_STATUS_SIZE = 18
    val MAX_FIO_ODT_CONTENT_SIZE = 260 - TX_ID_SIZE - OBT_STATUS_SIZE
    val request = MutableLiveData<FIORequestContent>()
    val payeeName = MutableLiveData<String>("newfriend@hisdomain")
    val memoFrom = MutableLiveData<String>("Please give me money to party - and come on!!!!")
    val alternativeAmountFormatted = MutableLiveData<String>("55.02 USD")
    val payerName = MutableLiveData<String>("myfiowallet@mycelium")
    val payerNameOwnerAccount = MutableLiveData<WalletAccount<*>>()
    val payerAccount = MutableLiveData<WalletAccount<*>>()
    val memoTo = MutableLiveData<String>("")
    val requestDate = MutableLiveData<String>("")
    val amount = MutableLiveData<Value>(Value.valueOf(Utils.getBtcCoinType(), 12000))
    val payeeTokenPublicAddress = MutableLiveData<String>("")
    val payerTokenPublicAddress = MutableLiveData<String>("")
    val memoMaxLength = MutableLiveData<Int>()

    init {
        payeeTokenPublicAddress.observeForever {
            memoMaxLength.postValue(calculateMemoMaxLength())
        }
        payerTokenPublicAddress.observeForever {
            memoMaxLength.postValue(calculateMemoMaxLength())
        }
        amount.observeForever {
            memoMaxLength.postValue(calculateMemoMaxLength())
        }
        request.observeForever {
            memoMaxLength.postValue(calculateMemoMaxLength())
        }
    }

    /**
     * https://developers.fioprotocol.io/wallet-integration-guide/encrypting-fio-data
     * FIO memo has dynamic limit depends on amount, address, chain code, token code
     */
    fun calculateMemoMaxLength(): Int = MAX_FIO_ODT_CONTENT_SIZE -
            (payeeTokenPublicAddress.value?.length ?: 0) -
            (payerTokenPublicAddress.value?.length ?: 0) -
            Util.valueToDouble(amount.value!!).toString().length -
            request.value!!.deserializedContent!!.chainCode.length -
            request.value!!.deserializedContent!!.tokenCode.length
}