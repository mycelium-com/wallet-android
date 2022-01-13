package com.mycelium.wallet.activity.fio.requests

import android.app.Activity
import android.content.Intent
import android.graphics.Point
import android.os.AsyncTask
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.google.gson.Gson
import com.mrd.bitlib.util.HexUtils
import com.mycelium.bequant.common.loader
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.fio.requests.viewmodels.FioSendRequestViewModel
import com.mycelium.wallet.activity.modern.Toaster
import com.mycelium.wallet.activity.send.BroadcastDialog
import com.mycelium.wallet.activity.send.SendCoinsActivity
import com.mycelium.wallet.activity.send.adapter.FeeLvlViewAdapter
import com.mycelium.wallet.activity.send.adapter.FeeViewAdapter
import com.mycelium.wallet.activity.send.event.BroadcastResultListener
import com.mycelium.wallet.activity.send.model.*
import com.mycelium.wallet.activity.util.toStringWithUnit
import com.mycelium.wallet.databinding.*
import com.mycelium.wallet.fio.FioRequestNotificator
import com.mycelium.wallet.fio.event.FioRequestStatusChanged
import com.mycelium.wapi.wallet.*
import com.mycelium.wapi.wallet.Util.getCoinByChain
import com.mycelium.wapi.wallet.Util.strToBigInteger
import com.mycelium.wapi.wallet.btc.bip44.HDAccount
import com.mycelium.wapi.wallet.btc.single.SingleAddressAccount
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.erc20.ERC20Account
import com.mycelium.wapi.wallet.eth.EthAccount
import com.mycelium.wapi.wallet.fio.*
import fiofoundation.io.fiosdk.errors.FIOError
import fiofoundation.io.fiosdk.models.fionetworkprovider.FIORequestContent
import fiofoundation.io.fiosdk.models.fionetworkprovider.response.PushTransactionResponse
import kotlinx.android.synthetic.main.fio_send_request_buttons.*
import kotlinx.android.synthetic.main.fio_send_request_info.*
import kotlinx.android.synthetic.main.send_coins_activity.*
import kotlinx.android.synthetic.main.send_coins_advanced_eth.*
import kotlinx.android.synthetic.main.send_coins_fee_selector.*
import java.math.BigInteger
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger

class ApproveFioRequestActivity : AppCompatActivity(), BroadcastResultListener {

    private lateinit var fioRequestViewModel: FioSendRequestViewModel
    private lateinit var sendViewModel: SendCoinsViewModel
    private lateinit var binding: Any
    private lateinit var signedTransaction: Transaction
    private lateinit var mbwManager: MbwManager
    private lateinit var fioModule: FioModule
    private var payerInited = false
    private var spinnerInited = false
    private var activityResultDialog: DialogFragment? = null

    companion object {
        const val CONTENT = "content"
        const val CONVERTED_AMOUNT = "converted"
        const val FEE = "fee"
        const val DATE = "date"
        const val FROM = "from"
        const val MEMO = "memo"
        const val TO = "to"
        const val TXID = "txid"
        const val AMOUNT = "amount"
        const val ACCOUNT = "account"
        fun start(activity: Activity, item: FIORequestContent) {
            with(Intent(activity, ApproveFioRequestActivity::class.java)) {
                putExtra(CONTENT, item.toJson())
                activity.startActivity(this)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val viewModelProvider = ViewModelProviders.of(this)
        fioRequestViewModel = viewModelProvider.get(FioSendRequestViewModel::class.java)
        val fioRequestContent = Gson().fromJson(intent.getStringExtra(CONTENT), FIORequestContent::class.java)

        supportActionBar?.run {
            setHomeAsUpIndicator(R.drawable.ic_back_arrow)
            setDisplayHomeAsUpEnabled(true)
            title = "Send ${fioRequestContent.deserializedContent!!.tokenCode}"
        }

        // request data population
        fioRequestViewModel.request.value = fioRequestContent
        fioRequestViewModel.memoFrom.value = fioRequestContent.deserializedContent?.memo ?: ""
        fioRequestViewModel.payeeName.value = fioRequestContent.payeeFioAddress
        fioRequestViewModel.payerName.value = fioRequestContent.payerFioAddress
        fioRequestViewModel.requestDate.value = Util.transformExpirationDate(fioRequestContent.timeStamp)

        mbwManager = MbwManager.getInstance(this)
        val walletManager = mbwManager.getWalletManager(false)
        fioModule = walletManager.getModuleById(FioModule.ID) as FioModule
        val uuid = fioModule.getFioAccountByFioName(fioRequestContent.payerFioAddress)!!
        fioRequestViewModel.payerNameOwnerAccount.value = walletManager.getAccount(uuid) as FioAccount
        val requestedCurrency = getCoinByChain(mbwManager.network, fioRequestContent.deserializedContent!!.tokenCode)
                ?: mbwManager.getWalletManager(false).getAssetTypes()
                        .find { it.symbol.equals(fioRequestContent.deserializedContent!!.tokenCode, true) }
        if (requestedCurrency == null) {
            Toaster(this).toast("Impossible to pay request with the ${fioRequestContent.deserializedContent!!.tokenCode} currency", true)
            finish()
            return
        }

        val spendingAccounts = mbwManager.getWalletManager(false)
                .getActiveSpendingAccounts().filter { it.coinType.id == requestedCurrency.id }.sortedBy { it.label }
        fioRequestViewModel.amount.value = Value.valueOf(requestedCurrency, strToBigInteger(requestedCurrency,
                fioRequestContent.deserializedContent!!.amount))
        if (spendingAccounts.isNotEmpty()) {
            val account = spendingAccounts.first()
            fioRequestViewModel.payerAccount.value = account
            setUpSendViewModel(account, viewModelProvider)
            initSpinners(spendingAccounts)
        } else {
            Toaster(this).toast("Impossible to pay request with the ${fioRequestContent.deserializedContent!!.tokenCode} currency", true)
            finish()
        }

        fioRequestViewModel.payerAccount.observe(this, Observer {
            if (!payerInited) {
                payerInited = true
            } else {
                sendViewModel.init(it, intent)
                when (binding) {
                    is FioSendRequestActivityBindingImpl ->
                        (binding as FioSendRequestActivityBinding).sendViewModel = sendViewModel
                    is FioSendRequestActivityEthBindingImpl ->
                        (binding as FioSendRequestActivityEthBinding).sendViewModel = sendViewModel as SendEthViewModel
                    is FioSendRequestActivityFioBindingImpl ->
                        (binding as FioSendRequestActivityFioBinding).sendViewModel = sendViewModel as SendFioViewModel
                }
                initFeeView()
                initFeeLvlView()
                sendViewModel.getAmount().value = fioRequestViewModel.amount.value
                initReceivingAddress()
                sendViewModel.getTransactionStatus().observe(this, Observer { status ->
                    btSend.isEnabled = status == SendCoinsModel.TransactionStatus.OK
                })
            }
        })

        sendViewModel.getAmount().value = fioRequestViewModel.amount.value
        val fioEndpoints = mbwManager.fioEndpoints
        GetPublicAddressTask(fioEndpoints, fioRequestViewModel.payeeName.value!!,
                fioRequestContent.deserializedContent!!.chainCode,
                fioRequestContent.deserializedContent!!.tokenCode, fioModule) { response ->
            if (response.message != null) {
                Toaster(this).toast(response.message!!, false)
            } else {
                fioRequestViewModel.payeeTokenPublicAddress.value = response.publicAddress!!
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)

        GetPublicAddressTask(fioEndpoints, fioRequestViewModel.payerName.value!!,
                fioRequestContent.deserializedContent!!.chainCode,
                fioRequestContent.deserializedContent!!.tokenCode, fioModule) { response ->
            if (response.message != null) {
                Toaster(this).toast(response.message!!, false)
            } else {
                fioRequestViewModel.payerTokenPublicAddress.value = response.publicAddress!!
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)

        fioRequestViewModel.alternativeAmountFormatted.value = mbwManager.exchangeRateManager.get(fioRequestViewModel.amount.value,
                mbwManager.getFiatCurrency(fioRequestViewModel.amount.value?.type))?.toStringWithUnit()

        sendViewModel.getTransactionStatus().observe(this, Observer {
            btSend.isEnabled = it == SendCoinsModel.TransactionStatus.OK
        })
        fioRequestViewModel.payeeTokenPublicAddress.observe(this, Observer {
            initReceivingAddress()
        })
        fioRequestViewModel.amount.observe(this, Observer {
            sendViewModel.getAmount().value = it
        })
    }

    private fun initReceivingAddress() {
        if (!fioRequestViewModel.payeeTokenPublicAddress.value.isNullOrEmpty()) {
            val parsedAddresses = mbwManager.getWalletManager(false).parseAddress(fioRequestViewModel.payeeTokenPublicAddress.value!!)
            sendViewModel.getReceivingAddress().value = parsedAddresses.first()
        }
    }

    private fun setUpSendViewModel(account: WalletAccount<*>, viewModelProvider: ViewModelProvider) {
        sendViewModel = provideSendViewModel(account, viewModelProvider)
        sendViewModel.init(account, intent)
        initDatabinding(account)
        initFeeView()
        initFeeLvlView()
    }

    private fun provideSendViewModel(account: WalletAccount<*>, viewModelProvider: ViewModelProvider): SendCoinsViewModel {
        return when (account) {
            is SingleAddressAccount, is HDAccount -> viewModelProvider.get(SendBtcViewModel::class.java)
            is EthAccount, is ERC20Account -> viewModelProvider.get(SendEthViewModel::class.java)
            is FioAccount -> viewModelProvider.get(SendFioViewModel::class.java)
            else -> throw NotImplementedError()
        }
    }

    private fun initSpinners(spendingAccounts: List<WalletAccount<*>>) {
        val fiatCurrencies = mbwManager.currencyList
        val spinnerItems = fiatCurrencies.mapNotNull {
            mbwManager.exchangeRateManager.get(fioRequestViewModel.amount.value, it)?.toStringWithUnit()
        }
        if (spinnerItems.size < 2) {
            spinnerFiat?.background = null
            spinnerFiat?.setPadding(0,0,0,0)
        }
        spinnerFiat?.adapter = ArrayAdapter(this, R.layout.layout_fio_fiat_spinner, R.id.text,
                spinnerItems).apply {
            setDropDownViewResource(R.layout.layout_send_coin_transaction_replace_dropdown)
        }
        spinnerFiat?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) {}
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                fioRequestViewModel.alternativeAmountFormatted.value = spinnerFiat.adapter.getItem(p2) as String
            }
        }
        spinnerFiat?.setSelection(spinnerItems.indexOfFirst {
            it.contains(mbwManager.getFiatCurrency(fioRequestViewModel.amount.value?.type).symbol, true)
        })
        val spendingFrom = spendingAccounts.map { "${it.label} - ${it.accountBalance.spendable}" }
        if (spendingFrom.size < 2) {
            spinnerSpendingFromAccount?.background = null
            spinnerSpendingFromAccount?.setPadding(0,0,0,0)
        }
        spinnerSpendingFromAccount?.adapter = ArrayAdapter(this, R.layout.layout_spending_from_account,
                R.id.text, spendingFrom).apply {
            setDropDownViewResource(R.layout.layout_send_coin_transaction_replace_dropdown)
        }
        spinnerSpendingFromAccount?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) {}
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                if (!spinnerInited) {
                    spinnerInited = true
                } else {
                    fioRequestViewModel.payerAccount.value = spendingAccounts[p2]
                }
            }
        }
    }

    fun onClickSend() {
        sendViewModel.sendTransaction(this)
    }

    fun onClickDecline() {
        val dialog = AlertDialog.Builder(this, R.style.MyceliumModern_Dialog_BlueButtons)
                .setTitle(getString(R.string.delete_received_fio_request))
                .setMessage(getString(R.string.delete_received_fio_request_msg))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete) { _, _ ->
                    loader(true)
                    btSend.isEnabled = false
                    RejectRequestTask(fioRequestViewModel.payerNameOwnerAccount.value!! as FioAccount, fioRequestViewModel.payerName.value!!,
                            fioRequestViewModel.request.value!!.fioRequestId, fioModule) {
                        loader(false)
                        FioRequestNotificator.cancel(fioRequestViewModel.request.value!!)
                        MbwManager.getEventBus().post(FioRequestStatusChanged(fioRequestViewModel.request.value!!));
                        finish()
                    }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
                }.create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getColor(R.color.fio_red))
        }
        dialog.show()
    }

    override fun onResume() {
        super.onResume()
        activityResultDialog?.show(supportFragmentManager, "ActivityResultDialog")
        activityResultDialog = null
    }

    private fun initDatabinding(account: WalletAccount<*>) {
        val sendCoinsActivityBinding = when (account) {
            is EthAccount, is ERC20Account -> {
                DataBindingUtil.setContentView<FioSendRequestActivityEthBinding>(this, R.layout.fio_send_request_activity_eth)
                        .also {
                            it.fioRequestViewModel = fioRequestViewModel
                            it.sendViewModel = (sendViewModel as SendEthViewModel).apply {
                                spinner?.adapter = ArrayAdapter(context,
                                        R.layout.layout_send_coin_transaction_replace, R.id.text, getTxItems()).apply {
                                    this.setDropDownViewResource(R.layout.layout_send_coin_transaction_replace_dropdown)
                                }
                            }
                            it.activity = this
                        }
            }
            is FioAccount -> {
                DataBindingUtil.setContentView<FioSendRequestActivityFioBinding>(this, R.layout.fio_send_request_activity_fio)
                        .also {
                            it.fioRequestViewModel = fioRequestViewModel
                            it.sendViewModel = sendViewModel as SendFioViewModel
                            it.activity = this
                        }
            }
            else -> getDefaultBinding()
        }
        sendCoinsActivityBinding.lifecycleOwner = this
        binding = sendCoinsActivityBinding
    }

    private fun getDefaultBinding(): FioSendRequestActivityBinding =
            DataBindingUtil.setContentView<FioSendRequestActivityBinding>(this, R.layout.fio_send_request_activity)
                    .also {
                        it.fioRequestViewModel = fioRequestViewModel
                        it.sendViewModel = sendViewModel
                        it.activity = this
                    }

    private fun initFeeView() {
        feeValueList?.setHasFixedSize(true)

        val displaySize = Point()
        windowManager.defaultDisplay.getSize(displaySize)
        val feeFirstItemWidth = (displaySize.x - resources.getDimensionPixelSize(R.dimen.item_dob_width)) / 2

        val feeViewAdapter = FeeViewAdapter(feeFirstItemWidth)
        feeViewAdapter.setFormatter(sendViewModel.getFeeFormatter())

        feeValueList?.adapter = feeViewAdapter
        feeViewAdapter.setDataset(sendViewModel.getFeeDataset().value)
        sendViewModel.getFeeDataset().observe(this, Observer { feeItems ->
            feeViewAdapter.setDataset(feeItems)
            val selectedFee = sendViewModel.getSelectedFee().value!!
            if (feeViewAdapter.selectedItem >= feeViewAdapter.itemCount ||
                    feeViewAdapter.getItem(feeViewAdapter.selectedItem).feePerKb != selectedFee.valueAsLong) {
                feeValueList?.setSelectedItem(selectedFee)
            }
        })

        feeValueList?.setSelectListener { adapter, position ->
            val item = (adapter as FeeViewAdapter).getItem(position)
            sendViewModel.getSelectedFee().value = Value.valueOf(item.value.type, item.feePerKb)

            if (sendViewModel.isSendScrollDefault() && root.maxScrollAmount - root.scaleY > 0) {
                root.smoothScrollBy(0, root.maxScrollAmount)
                sendViewModel.setSendScrollDefault(false)
            }
        }
    }

    private fun initFeeLvlView() {
        feeLvlList?.setHasFixedSize(true)
        val feeLvlItems = sendViewModel.getFeeLvlItems()

        val displaySize = Point()
        windowManager.defaultDisplay.getSize(displaySize)
        val feeFirstItemWidth = (displaySize.x - resources.getDimensionPixelSize(R.dimen.item_dob_width)) / 2
        feeLvlList?.adapter = FeeLvlViewAdapter(feeLvlItems, feeFirstItemWidth)
        feeLvlList?.setSelectListener { adapter, position ->
            val item = (adapter as FeeLvlViewAdapter).getItem(position)
            sendViewModel.getFeeLvl().value = item.minerFee
            feeValueList?.setSelectedItem(sendViewModel.getSelectedFee().value)
        }
        feeLvlList?.setSelectedItem(sendViewModel.getFeeLvl().value)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == SendCoinsActivity.SIGN_TRANSACTION_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            signedTransaction = data!!.getSerializableExtra(SendCoinsActivity.SIGNED_TRANSACTION) as Transaction
            activityResultDialog = BroadcastDialog.create(sendViewModel.getAccount(), isColdStorage = false, transaction = signedTransaction)
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
            when (item.itemId) {
                android.R.id.home -> {
                    onBackPressed()
                    true
                }
                else -> super.onOptionsItemSelected(item)
            }

    override fun broadcastResult(broadcastResult: BroadcastResult) {
        if (broadcastResult.resultType == BroadcastResultType.SUCCESS) {
            val txid = HexUtils.toHex(signedTransaction.id)
            loader(true)
            RecordObtTask(txid, fioRequestViewModel, fioModule) { success ->
                loader(false)
                if (!success) {
                    Toaster(this).toast("Failed to write obt", false)
                }
                mbwManager.getWalletManager(false)
                        .startSynchronization(fioRequestViewModel.payerNameOwnerAccount.value?.id)
                FioRequestNotificator.cancel(fioRequestViewModel.request.value!!)
                MbwManager.getEventBus().post(FioRequestStatusChanged(fioRequestViewModel.request.value!!));

                ApproveFioRequestSuccessActivity.start(this, fioRequestViewModel.amount.value!!,
                        fioRequestViewModel.alternativeAmountFormatted.value!!,
                        sendViewModel.getSelectedFee().value!!, Date().time, fioRequestViewModel.payerName.value!!,
                        fioRequestViewModel.payeeName.value!!, fioRequestViewModel.memoTo.value!!,
                        signedTransaction.id, fioRequestViewModel.payerAccount.value!!.id)
                finish()
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        }
    }

    class GetPublicAddressTask(
            private val fioEndpoints: FioEndpoints,
            private val fioName: String,
            private val chainCode: String,
            private val tokenCode: String,
            private val fioModule: FioModule,
            val listener: ((GetPubAddressResponse) -> Unit)) : AsyncTask<Void, Void, GetPubAddressResponse>() {
        override fun doInBackground(vararg args: Void): GetPubAddressResponse {
            return try {
                FioBlockchainService.getPubkeyByFioAddress(fioEndpoints, fioName, chainCode, tokenCode)
            } catch (e: Exception) {
                if (e is FIOError) {
                    fioModule.addFioServerLog(e.toJson())
                }
                GetPubAddressResponse().also {
                    it.message = e.localizedMessage
                }
            }
        }

        override fun onPostExecute(result: GetPubAddressResponse) {
            listener(result)
        }
    }

    class RecordObtTask(
            private val txid: String,
            private val fioRequestViewModel: FioSendRequestViewModel,
            private val fioModule: FioModule,
            val listener: ((Boolean) -> Unit)) : AsyncTask<Void, Void, Boolean>() {
        override fun doInBackground(vararg args: Void): Boolean {
            return try {
                val request = fioRequestViewModel.request.value!!
                val amountInDouble = fioRequestViewModel.amount.value!!.toPlainString().toDouble()
                (fioRequestViewModel.payerNameOwnerAccount.value!! as FioAccount).recordObtData(request.fioRequestId,
                        request.payerFioAddress, request.payeeFioAddress, fioRequestViewModel.payerTokenPublicAddress.value!!,
                        fioRequestViewModel.payeeTokenPublicAddress.value!!, amountInDouble, request.deserializedContent!!.chainCode,
                        request.deserializedContent!!.tokenCode, txid, fioRequestViewModel.memoTo.value ?: "")
            } catch (e: Exception) {
                if (e is FIOError) {
                    fioModule.addFioServerLog(e.toJson())
                }
                Logger.getLogger(RecordObtTask::class.simpleName).log(Level.WARNING, "failed to write obt: ${e.localizedMessage}")
                false
            }
        }

        override fun onPostExecute(result: Boolean) {
            listener(result)
        }
    }

    class RejectRequestTask(
            private val fioAccount: FioAccount,
            private val fioName: String,
            private val requestId: BigInteger,
            private val fioModule: FioModule,
            val listener: ((PushTransactionResponse.ActionTraceResponse?) -> Unit)) : AsyncTask<Void, Void, PushTransactionResponse.ActionTraceResponse?>() {
        override fun doInBackground(vararg args: Void): PushTransactionResponse.ActionTraceResponse? {
            return try {
                fioAccount.rejectFundsRequest(requestId, fioName)
            } catch (e: Exception) {
                if (e is FIOError) {
                    fioModule.addFioServerLog(e.toJson())
                }
                Logger.getLogger(RejectRequestTask::class.simpleName).log(Level.WARNING, "failed to reject request: ${e.localizedMessage}")
                null
            }
        }

        override fun onPostExecute(result: PushTransactionResponse.ActionTraceResponse?) {
            listener(result)
        }
    }
}
