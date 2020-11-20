package com.mycelium.wallet.activity.fio.mapaccount.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mycelium.wapi.wallet.fio.FioAccount
import com.mycelium.wapi.wallet.fio.RegisteredFIOName
import java.text.SimpleDateFormat
import java.util.*


class AccountMappingViewModel : ViewModel() {
    val fioAccount = MutableLiveData<FioAccount>()
    val fioName = MutableLiveData<RegisteredFIOName>()
    val acknowledge = MutableLiveData<Boolean>(false)
    val fewTransactionsLeft = MutableLiveData<Boolean>()
    private val shouldRenew = MutableLiveData<Boolean>()

    fun dateToString(date: Date) = DATE_FORMAT.format(date)!!

    fun intToString(int: Int) = int.toString()

    fun soonExpiring() = EXPIRATION_WARN_DATE.after(fioName.value?.expireDate)

    fun update() {
        val fewTransactions = fioName.value?.bundledTxsNum ?: 0 < 10
        fewTransactionsLeft.postValue(fewTransactions)
        shouldRenew.postValue(fewTransactions || soonExpiring())
    }

    companion object {
        private val DATE_FORMAT = SimpleDateFormat("MMMM dd, yyyy\nK:mm a")
        private val EXPIRATION_WARN_DATE = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, 30) }
    }
}