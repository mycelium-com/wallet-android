package com.mycelium.wallet.activity.send.model

import android.app.Activity
import android.app.Application
import android.content.Intent
import com.mycelium.view.Denomination
import com.mycelium.wallet.activity.send.adapter.FeeViewAdapter
import com.mycelium.wallet.activity.util.toStringWithUnit
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.btc.bip44.HDAccountExternalSignature
import com.mycelium.wapi.wallet.coins.Value
import org.web3j.utils.Convert
import java.math.RoundingMode
import java.util.regex.Pattern
import kotlin.math.roundToLong

open class SendEthViewModel(context: Application) : SendCoinsViewModel(context) {
    override val uriPattern = Pattern.compile("0x[a-zA-Z0-9]+")

    override fun init(account: WalletAccount<*>, intent: Intent) {
        super.init(account, intent)
        model = SendEthModel(context, account, intent)
    }

    override fun sendTransaction(activity: Activity) {
        if (isColdStorage() || model.account is HDAccountExternalSignature) {
            // We do not ask for pin when the key is from cold storage or from a external device (trezor,...)
            model.signTransaction(activity)
        } else {
            mbwManager.runPinProtectedFunction(activity) { model.signTransaction(activity) }
        }
    }

    override fun getFeeFormatter() = object : FeeViewAdapter.FeeItemFormatter {
        override fun getFeeAbsValue(value: Value) = value.toStringWithUnit(Denomination.MILLI)

        override fun getAltValue(value: Value) = "~${value.toStringWithUnit()}"

        override fun getFeePerUnit(value: Long) = "${Convert.fromWei(value.toBigDecimal(),
                Convert.Unit.GWEI).setScale(2, RoundingMode.HALF_UP)} Gwei/gas"
    }
}