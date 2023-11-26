package com.mycelium.wallet.activity.send.model

import android.app.Activity
import android.app.AlertDialog
import android.app.Application
import android.content.Intent
import androidx.lifecycle.MutableLiveData
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.modern.Toaster
import com.mycelium.wallet.activity.util.EthFeeFormatter
import com.mycelium.wallet.activity.util.toStringFriendlyWithUnit
import com.mycelium.wapi.content.AssetUri
import com.mycelium.wapi.content.eth.EthUri
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.btc.bip44.HDAccountExternalSignature
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.erc20.ERC20Account
import com.mycelium.wapi.wallet.eth.EthAccount
import java.math.BigInteger
import java.util.regex.Pattern

open class SendEthViewModel(application: Application) : SendCoinsViewModel(application) {
    override val uriPattern = Pattern.compile("[a-zA-Z0-9]+")!!
    val toaster = Toaster(application)

    override fun init(account: WalletAccount<*>, intent: Intent) {
        super.init(account, intent)
        model = SendEthModel(getApplication(), account, intent)
    }

    override fun processAssetUri(uri: AssetUri) {
        val ethUri = uri as EthUri

        if (ethUri.asset != null) {
            val token = mbwManager.supportedERC20Tokens.values.firstOrNull { it.contractAddress.equals(ethUri.asset, true) }
            if (token != null) {
                if (getAccount() !is ERC20Account || token.id != getAccount().coinType.id) {
                    toaster.toast(context.getString(R.string.payment_uri_wrong_account_erc20, token.name), false)
                } else {
                    setParams(uri)
                }
            } else {
                toaster.toast(R.string.payment_uri_unsupported_token, false)
            }
        } else {
            if (getAccount() !is EthAccount) {
                toaster.toast(R.string.payment_uri_wrong_account_eth, false)
            } else {
                setParams(uri)
            }
        }
    }

    private fun setParams(uri: AssetUri) {
        model.receivingAddress.value = uri.address
        model.transactionLabel.value = uri.label
        if (uri.value?.isPositive() == true) {
            //we set the amount to the one contained in the qr code, even if another one was entered previously
            if (!Value.isNullOrZero(model.amount.value)) {
                toaster.toast(R.string.amount_changed, false)
            }
            model.amount.value = uri.value
        }
    }

    val isAdvancedBlockExpanded: MutableLiveData<Boolean> = MutableLiveData()

    fun getParentAccount() = (model as SendEthModel).parentAccount

    fun getGasLimit() = (model as SendEthModel).gasLimit

    fun getGasPrice() = (model as SendEthModel).gasPrice

    fun getInputData() = (model as SendEthModel).inputData

    fun getTxItems() = (model as SendEthModel).txItems

    fun getSelectedTxItem() = (model as SendEthModel).selectedTxItem

    fun getEstimatedFee() = (model as SendEthModel).estimatedFee

    fun getTotalFee() = (model as SendEthModel).totalFee

    fun getGasLimitStatus() = (model as SendEthModel).gasLimitStatus

    fun getDenomination() = (model as SendEthModel).denomination

    fun getDefaultGasLimit(): BigInteger = (model as SendEthModel).account.let {
        if (it is ERC20Account) {
            BigInteger.valueOf(ERC20Account.TOKEN_TRANSFER_GAS_LIMIT)
        } else {
            BigInteger.valueOf(it.typicalEstimatedTransactionSize.toLong())
        }
    }

    fun getSecondsUntilNextUpdate() = (model as SendEthModel).feeUpdater.secondsUntilNextUpdate

    fun convert(value: Value): String =
        " ~${mbwManager.exchangeRateManager.get(value, mbwManager.getFiatCurrency(value.type))?.toStringFriendlyWithUnit() ?: ""}"

    fun minerFeeInfoClickListener() {
        AlertDialog.Builder(activity, R.style.MyceliumModern_Dialog)
            .setMessage(R.string.miner_fee_erc20_info)
            .setTitle(R.string.miner_fee_erc20_info_title)
            .setPositiveButton(R.string.button_ok, null)
            .create()
            .show()
    }

    fun gasLimitErc20InfoClickListener(activity: Activity) {
        AlertDialog.Builder(activity, R.style.MyceliumModern_Dialog)
            .setMessage(R.string.gas_limit_erc20_info)
            .setTitle(R.string.gas_limit_erc20_info_title)
            .setPositiveButton(R.string.button_ok, null)
            .create()
            .show()
    }

    override fun sendTransaction(activity: Activity) {
        if (isColdStorage() || model.account is HDAccountExternalSignature) {
            // We do not ask for pin when the key is from cold storage or from a external device (trezor,...)
            model.signTransaction(activity)
            sendFioObtData()
        } else {
            mbwManager.runPinProtectedFunction(activity) {
                model.signTransaction(activity)
                sendFioObtData()
            }
        }
    }

    override fun getFeeFormatter() = EthFeeFormatter()
}
