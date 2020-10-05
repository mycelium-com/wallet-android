package com.mycelium.wallet.activity.send.model

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.util.EthFeeFormatter
import com.mycelium.wapi.content.AssetUri
import com.mycelium.wapi.content.eth.EthUri
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.btc.bip44.HDAccountExternalSignature
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.erc20.ERC20Account
import com.mycelium.wapi.wallet.eth.EthAccount
import java.util.regex.Pattern

open class SendEthViewModel(application: Application) : SendCoinsViewModel(application) {
    override val uriPattern = Pattern.compile("[a-zA-Z0-9]+")!!

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
                    Toast.makeText(activity, context.getString(R.string.payment_uri_wrong_account_erc20, token.name), Toast.LENGTH_LONG).show()
                } else {
                    setParams(uri)
                }
            } else {
                Toast.makeText(activity, R.string.payment_uri_unsupported_token, Toast.LENGTH_LONG).show()
            }
        } else {
            if (getAccount() !is EthAccount) {
                Toast.makeText(activity, R.string.payment_uri_wrong_account_eth, Toast.LENGTH_LONG).show()
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
                Toast.makeText(activity, R.string.amount_changed, Toast.LENGTH_LONG).show()
            }
            model.amount.value = uri.value
        }
    }

    var isAdvancedBlockExpanded: MutableLiveData<Boolean> = MutableLiveData()

    fun expandCollapseAdvancedBlock() {
        isAdvancedBlockExpanded.value = isAdvancedBlockExpanded.value != true
    }

    fun getGasLimit() = (model as SendEthModel).gasLimit

    fun getInputData() = (model as SendEthModel).inputData

    fun getTxItems() = (model as SendEthModel).txItems

    fun getSelectedTxItem() = (model as SendEthModel).selectedTxItem

    fun showGasLimitError() = (model as SendEthModel).showGasLimitError

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
