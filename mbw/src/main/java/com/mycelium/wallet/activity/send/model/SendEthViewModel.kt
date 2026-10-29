package com.mycelium.wallet.activity.send.model

import android.app.Activity
import android.app.AlertDialog
import android.app.Application
import android.content.Intent
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
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
import com.mycelium.wapi.wallet.eth.AbstractEthERC20Account
import com.mycelium.wapi.wallet.eth.EthAccount
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import android.app.ProgressDialog
import com.mrd.bitlib.util.HexUtils
import com.mycelium.wallet.activity.send.SendCoinsActivity
import com.mycelium.wallet.activity.util.toStringWithUnit
import com.mycelium.wapi.wallet.BroadcastResult
import com.mycelium.wapi.wallet.BroadcastResultType
import com.mycelium.wapi.wallet.SyncMode
import com.mycelium.wapi.wallet.eth.EthTransaction
import com.mycelium.wapi.wallet.eth.aa.AAError
import com.mycelium.wapi.wallet.eth.aa.AATransactionManager
import com.mycelium.wapi.wallet.eth.aa.AATransactionResult
import java.math.BigInteger
import java.util.regex.Pattern

open class SendEthViewModel(application: Application) : SendCoinsViewModel(application) {
    override val uriPattern = Pattern.compile("[a-zA-Z0-9]+")!!
    val toaster = Toaster(application)

    override fun init(account: WalletAccount<*>, intent: Intent) {
        super.init(account, intent)
        model = SendEthModel(getApplication(), account, intent)
        val ethModel = model as SendEthModel
        if (isAdvancedBlockExpanded.value == null) {
            isAdvancedBlockExpanded.value = false
        }
        if (ethModel.nonce.value != null) {
            isAdvancedBlockExpanded.value = true
        } else {
            checkStuckQueue(account)
        }
    }

    /** Nonce of an earlier pending tx that cannot be mined at the current base fee, if any. */
    val stuckQueueNonce: MutableLiveData<String?> = MutableLiveData()

    /**
     * A new send takes the next nonce and cannot confirm before an earlier stuck one — 2026-10-26
     * a user retried a USDT send four times behind a nonce priced under the base fee, each retry
     * only lengthening the queue. A replacement (preset nonce) reuses its own slot and is skipped.
     */
    private fun checkStuckQueue(account: WalletAccount<*>) {
        val ethAccount = account as? AbstractEthERC20Account ?: return
        viewModelScope.launch {
            stuckQueueNonce.value = ethAccount.findStuckQueueTx()?.nonce?.toString()
        }
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

    fun getNonce() = (model as SendEthModel).nonce

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

    // ---- gas paid in the token (ERC-4337 paymaster)
    private val ethModel get() = model as SendEthModel
    private val tokenFeeScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    fun getFeeMode() = ethModel.feeMode
    fun getTokenFeeAvailable() = ethModel.tokenFeeAvailable
    fun getTokenFeeText() = ethModel.tokenFeeText
    fun getTokenFeeMaxText() = ethModel.tokenFeeMaxText
    fun getTokenFeeLoading() = ethModel.tokenFeeLoading
    fun getTokenFeeError() = ethModel.tokenFeeError
    fun tokenSymbol(): String = model.account.coinType.symbol
    fun selectEthFee() = ethModel.setFeeMode(EthFeeMode.ETH)
    fun selectTokenFee() = ethModel.setFeeMode(EthFeeMode.TOKEN)

    fun tokenFeeFiat(): String {
        val fee = ethModel.tokenFeeValue() ?: return ""
        val rate = mbwManager.exchangeRateManager.get(fee, mbwManager.getFiatCurrency(model.account.coinType))?.toStringWithUnit()
        return if (rate != null) "~$rate" else ""
    }

    fun tokenFeeInfoClickListener() {
        val symbol = tokenSymbol()
        AlertDialog.Builder(activity, R.style.MyceliumModern_Dialog)
            .setTitle(context.getString(R.string.eth_token_fee_info_title, symbol))
            .setMessage(context.getString(R.string.eth_token_fee_info_message, symbol, symbol,
                ethModel.tokenFeeMaxText.value.orEmpty().ifEmpty { symbol }))
            .setPositiveButton(R.string.button_ok, null)
            .create()
            .show()
    }

    override fun sendTransaction(activity: Activity) {
        if (ethModel.isTokenFeeMode()) {
            if (isColdStorage()) {
                sendWithTokenFee(activity)
            } else {
                mbwManager.runPinProtectedFunction(activity) { sendWithTokenFee(activity) }
            }
            return
        }
        if (isColdStorage() || model.account is HDAccountExternalSignature) {
            // We do not ask for pin when the key is from cold storage or from a external device (trezor,...)
            model.signTransaction(activity)
        } else {
            mbwManager.runPinProtectedFunction(activity) {
                model.signTransaction(activity)
            }
        }
    }

    /**
     * The transfer goes out as a UserOperation: `[approve(paymaster), transfer(recipient)]`,
     * signed with the account key together with the EIP-7702 delegation when the address is not
     * delegated yet. The bundler mines it and the paymaster takes the gas in the token.
     */
    /** A token-paid send is in flight: a second tap must not start another one. */
    @Volatile
    private var tokenFeeSending = false

    private fun sendWithTokenFee(activity: Activity) {
        val account = model.account as? ERC20Account ?: return
        val manager = mbwManager.aaTransactionManager ?: return
        if (tokenFeeSending) return
        tokenFeeSending = true
        val progress = ProgressDialog(activity).apply {
            setCancelable(false)
            setMessage(context.getString(R.string.eth_token_fee_processing_quote))
            show()
        }
        val shownMax = ethModel.shownMaxTokenFee()
        tokenFeeScope.launch {
            try {
                sendWithTokenFee(activity, account, manager, progress, shownMax)
            } finally {
                tokenFeeSending = false
                if (progress.isShowing && !activity.isDestroyed) progress.dismiss()
            }
        }
    }

    private suspend fun sendWithTokenFee(activity: Activity, account: ERC20Account, manager: AATransactionManager,
                                         progress: ProgressDialog, shownMax: BigInteger?) {
        val send = withContext(Dispatchers.IO) { ethModel.quoteForSend() }
        if (send == null) {
            progress.dismiss()
            showTokenFeeError(activity, context.getString(
                if (ethModel.sendQuoteInsufficient) R.string.eth_token_fee_insufficient else R.string.eth_token_fee_unavailable,
                tokenSymbol()))
            return
        }
        // The quote is fetched again for the send, and Sepolia/mainnet gas can jump within
        // seconds (2026-10-29: shown "up to 0.22", re-quoted up to 0.94, charged 0.42 USDC).
        // A fee above what the user saw must be agreed to, not silently paid.
        if (shownMax == null || send.quote.maxTokenAmount > shownMax) {
            progress.dismiss()
            val symbol = model.account.coinType
            val confirmed = confirmNewFee(activity,
                Value.valueOf(symbol, send.quote.tokenAmount).toStringWithUnit(),
                Value.valueOf(symbol, send.quote.maxTokenAmount).toStringWithUnit())
            if (!confirmed) return
            progress.show()
        }
        progress.setMessage(context.getString(R.string.eth_token_fee_processing_sending))
        val result = withContext(Dispatchers.IO) {
            manager.execute(account.receivingAddress.addressString, send.token, send.recipient, send.amount.value,
                send.quote, account::signHash)
        }
        progress.dismiss()
        if (result is AATransactionResult.Success) {
            // Book it now: blockbook may take a while to index the bundle, and until then a
            // second send would be priced against the pre-send balance.
            withContext(Dispatchers.IO) {
                account.recordTokenPaidTransfer(result.transactionHash, result.blockNumber, send.recipient,
                    result.tokenDebited.takeIf { it.signum() > 0 } ?: (send.amount.value + send.quote.tokenAmount))
            }
        }
        // The EIP-7702 authorization consumed an EOA nonce and the token balance moved: resync
        // both accounts so the next ETH-paid send does not reuse a stale nonce.
        mbwManager.getWalletManager(false).startSynchronization(SyncMode.NORMAL_FORCED, listOf(account, account.ethAcc))
        if (activity.isDestroyed) return
        when (result) {
            is AATransactionResult.Success -> finishAsSent(activity, result.transactionHash, send.amount)
            is AATransactionResult.Failure -> showTokenFeeError(activity, when (result.error) {
                AAError.TIMEOUT -> context.getString(R.string.eth_token_fee_timeout)
                AAError.PENDING_EOA_TX -> context.getString(R.string.eth_token_fee_pending_eth_tx)
                else -> context.getString(R.string.eth_token_fee_error, result.message)
            })
        }
    }

    private suspend fun confirmNewFee(activity: Activity, fee: String, maxFee: String): Boolean =
        suspendCancellableCoroutine { cont ->
            AlertDialog.Builder(activity, R.style.MyceliumModern_Dialog)
                .setTitle(R.string.eth_token_fee_changed_title)
                .setMessage(context.getString(R.string.eth_token_fee_changed_message, fee, maxFee))
                .setPositiveButton(R.string.send) { _, _ -> if (cont.isActive) cont.resume(true) }
                .setNegativeButton(R.string.cancel) { _, _ -> if (cont.isActive) cont.resume(false) }
                .setOnCancelListener { if (cont.isActive) cont.resume(false) }
                .create().show()
        }

    private fun showTokenFeeError(activity: Activity, message: String) {
        AlertDialog.Builder(activity, R.style.MyceliumModern_Dialog)
            .setTitle(R.string.eth_token_fee_title)
            .setMessage(message)
            .setPositiveButton(R.string.button_ok, null)
            .create().show()
    }

    /** Hands over to the activity's normal "sent" path so labels/results are handled as for any send. */
    private fun finishAsSent(activity: Activity, txHash: String, amount: Value) {
        val account = model.account
        val done = EthTransaction(account.basedOnCoinType, model.receivingAddress.value.toString(),
            Value.zeroValue(account.basedOnCoinType), BigInteger.ZERO, BigInteger.ZERO, BigInteger.ZERO, "",
            tokenValue = amount)
        done.txHash = HexUtils.toBytes(txHash.removePrefix("0x"))
        model.signedTransaction = done
        (activity as? SendCoinsActivity)?.broadcastResult(BroadcastResult(BroadcastResultType.SUCCESS))
    }

    override fun getFeeFormatter() = EthFeeFormatter()

    override fun onCleared() {
        (model as? SendEthModel)?.release()
        super.onCleared()
    }
}
