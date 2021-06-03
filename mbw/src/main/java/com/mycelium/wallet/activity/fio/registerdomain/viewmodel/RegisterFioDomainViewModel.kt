package com.mycelium.wallet.activity.fio.registerdomain.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.fio.FioAccount
import com.mycelium.wapi.wallet.fio.FioModule
import fiofoundation.io.fiosdk.errors.FIOError
import fiofoundation.io.fiosdk.errors.fionetworkprovider.PushTransactionError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.logging.Level
import java.util.logging.Logger

class RegisterFioDomainViewModel : ViewModel() {
    val fioAccountToRegisterName = MutableLiveData<FioAccount>()
    val accountToPayFeeFrom = MutableLiveData<WalletAccount<*>>()
    val registrationFee = MutableLiveData<Value>()
    val domain = MutableLiveData<String>("")
    val isFioDomainAvailable = MutableLiveData<Boolean>(true)
    val isFioDomainValid = MutableLiveData<Boolean>(true)
    val isFioServiceAvailable = MutableLiveData<Boolean>(true)
    val isRenew = MutableLiveData(false)

    fun registerDomain(fioModule: FioModule, doOnSuccess: (String) -> Unit, doOnError: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val expiration = fioAccountToRegisterName.value!!.registerFIOAddress(domain.value!!)!!
                doOnSuccess(expiration)
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Logger.getLogger(RegisterFioDomainViewModel::class.simpleName).log(Level.WARNING, "failed to register fio domain: ${e.localizedMessage}")
                    if (e is FIOError) {
                        fioModule.addFioServerLog(e.toJson())
                    }
                    doOnError("${(e.cause?.cause?.cause as? PushTransactionError)?.responseError?.fields?.firstOrNull()?.error ?: e.message}")
                }
            }
        }
    }
}