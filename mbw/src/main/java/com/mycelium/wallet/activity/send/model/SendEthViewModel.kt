package com.mycelium.wallet.activity.send.model

import android.app.Activity
import android.app.Application
import android.content.Intent
import androidx.databinding.InverseMethod
import androidx.lifecycle.MutableLiveData
import com.mycelium.wallet.activity.util.EthFeeFormatter
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.btc.bip44.HDAccountExternalSignature
import java.math.BigInteger
import java.util.regex.Pattern

open class SendEthViewModel(application: Application) : SendCoinsViewModel(application) {
    override val uriPattern = Pattern.compile("0x[a-zA-Z0-9]+")

    override fun init(account: WalletAccount<*>, intent: Intent) {
        super.init(account, intent)
        model = SendEthModel(context, account, intent)
    }

    var isAdvancedBlockExpanded: MutableLiveData<Boolean> = MutableLiveData()

    fun expandCollapseAdvancedBlock() {
        isAdvancedBlockExpanded.value = isAdvancedBlockExpanded.value != true
    }

    fun getGasLimit() = (model as SendEthModel).gasLimit

    fun getInputData() = (model as SendEthModel).inputData

    fun getTxItems() = (model as SendEthModel).txItems

    fun getSelectedTxItem() = (model as SendEthModel).selectedTxItem

    override fun sendTransaction(activity: Activity) {
        if (isColdStorage() || model.account is HDAccountExternalSignature) {
            // We do not ask for pin when the key is from cold storage or from a external device (trezor,...)
            model.signTransaction(activity)
        } else {
            mbwManager.runPinProtectedFunction(activity) { model.signTransaction(activity) }
        }
    }

    override fun getFeeFormatter() = EthFeeFormatter()
}

object Converter {
    @InverseMethod("stringToBigInt")
    @JvmStatic
    fun bigIntToString(value: BigInteger?): String {
        return value?.toString() ?: ""
    }

    @JvmStatic
    fun stringToBigInt(value: String): BigInteger? {
        return if (value.isNotEmpty()) BigInteger(value) else null
    }
}