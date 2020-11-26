package com.mycelium.wallet.activity.fio.requests

import android.app.Activity
import android.content.Intent
import android.graphics.Point
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
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
import com.mycelium.wapi.wallet.*
import com.mycelium.wapi.wallet.Util.getCoinByChain
import com.mycelium.wapi.wallet.Util.strToBigInteger
import com.mycelium.wapi.wallet.btc.bip44.HDAccount
import com.mycelium.wapi.wallet.btc.single.SingleAddressAccount
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.erc20.ERC20Account
import com.mycelium.wapi.wallet.eth.EthAccount
import com.mycelium.wapi.wallet.fio.FioAccount
import com.mycelium.wapi.wallet.fio.FioBlockchainService
import com.mycelium.wapi.wallet.fio.FioModule
import com.mycelium.wapi.wallet.fio.GetPubAddressResponse
import fiofoundation.io.fiosdk.models.fionetworkprovider.FIORequestContent
import fiofoundation.io.fiosdk.models.fionetworkprovider.response.PushTransactionResponse
import kotlinx.android.synthetic.main.fio_send_request_buttons.*
import kotlinx.android.synthetic.main.fio_send_request_info.*
import kotlinx.android.synthetic.main.send_coins_activity.*
import kotlinx.android.synthetic.main.send_coins_advanced_eth.*
import kotlinx.android.synthetic.main.send_coins_fee_selector.*
import java.io.IOException
import java.math.BigInteger
import java.util.*

class ApproveFioRequestActivity : AppCompatActivity(), BroadcastResultListener {

    private lateinit var fioRequestViewModel: FioSendRequestViewModel
    private lateinit var sendViewModel: SendCoinsViewModel
    private lateinit var binding: Any
    private lateinit var signedTransaction: Transaction
    private lateinit var mbwManager: MbwManager
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
            title = "Send ${fioRequestContent.deserializedContent!!.chainCode}"
        }

        // request data population
        fioRequestViewModel.request.value = fioRequestContent
        fioRequestViewModel.memoFrom.value = fioRequestContent.deserializedContent?.memo ?: ""
        fioRequestViewModel.payeeName.value = fioRequestContent.payeeFioAddress
        fioRequestViewModel.payerName.value = fioRequestContent.payerFioAddress
        fioRequestViewModel.requestDate.value = fioRequestContent.timeStamp
        Log.i("asdaf", "asdaf payeeFioAddress: ${fioRequestContent.payeeFioAddress} payerFioAddress: ${fioRequestContent.payerFioAddress}")

        mbwManager = MbwManager.getInstance(this)
        val walletManager = mbwManager.getWalletManager(false)
        val fioModule = walletManager.getModuleById(FioModule.ID) as FioModule
        val uuid = fioModule.getFioAccountByFioName(fioRequestContent.payerFioAddress)!!
        fioRequestViewModel.payerNameOwnerAccount.value = walletManager.getAccount(uuid) as FioAccount
        val requestedCurrency = getCoinByChain(mbwManager.network, fioRequestContent.deserializedContent!!.chainCode)
        if (requestedCurrency == null) {
            Toaster(this).toast("Impossible to pay request with the ${fioRequestContent.deserializedContent!!.chainCode} currency", true)
            finish()
            return
        }

        val spendingAccounts = mbwManager.getWalletManager(false)
                .getAllActiveAccounts().filter { it.coinType.id == requestedCurrency.id }.sortedBy { it.label }

        if (spendingAccounts.isNotEmpty()) {
            val account = spendingAccounts.first()
            fioRequestViewModel.payerAccount.value = account
            setUpSendViewModel(account, viewModelProvider)
            initSpinners(spendingAccounts)
        } else {
            Toaster(this).toast("Impossible to pay request with the ${fioRequestContent.deserializedContent!!.chainCode} currency", true)
            finish()
        }

        fioRequestViewModel.payerAccount.observe(this, Observer {
            if (!payerInited) {
                payerInited = true
            } else {
                sendViewModel.init(it, intent)
                when (binding) {
                    is FioSendRequestActivityBindingImpl ->
                        (binding as FioSendRequestActivityBindingImpl).sendViewModel = sendViewModel
                    is FioSendRequestActivityEthBindingImpl ->
                        (binding as FioSendRequestActivityEthBindingImpl).sendViewModel = sendViewModel as SendEthViewModel
                    is FioSendRequestActivityFioBindingImpl ->
                        (binding as FioSendRequestActivityFioBindingImpl).sendViewModel = sendViewModel as SendFioViewModel
                }
                initFeeView()
                initFeeLvlView()
                sendViewModel.getAmount().value = fioRequestViewModel.amount.value
                initReceivingAddress()
                sendViewModel.getTransactionStatus().observe(this, Observer {
                    Log.i("ApproveFioRequest", "sendViewModel.observer TransactionStatus: $it")
                    btSend.isEnabled = it == SendCoinsModel.TransactionStatus.OK
                })
            }
        })

        fioRequestViewModel.amount.value = Value.valueOf(requestedCurrency, strToBigInteger(requestedCurrency,
                fioRequestContent.deserializedContent!!.amount))
        sendViewModel.getAmount().value = fioRequestViewModel.amount.value
        GetPublicAddressTask(fioRequestViewModel.payeeName.value!!,
                fioRequestContent.deserializedContent!!.chainCode,
                fioRequestContent.deserializedContent!!.tokenCode) { response ->
            if (response.message != null) {
                Toaster(this).toast(response.message, false)
            } else {
                fioRequestViewModel.payeeTokenPublicAddress.value = response.publicAddress!!
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)

        GetPublicAddressTask(fioRequestViewModel.payerName.value!!,
                fioRequestContent.deserializedContent!!.chainCode,
                fioRequestContent.deserializedContent!!.tokenCode) { response ->
            if (response.message != null) {
                Toaster(this).toast(response.message, false)
            } else {
                fioRequestViewModel.payerTokenPublicAddress.value = response.publicAddress!!
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)

        fioRequestViewModel.alternativeAmountFormatted.value = mbwManager.exchangeRateManager.get(fioRequestViewModel.amount.value,
                mbwManager.getFiatCurrency(fioRequestViewModel.amount.value?.type))?.toStringWithUnit()

        sendViewModel.getTransactionStatus().observe(this, Observer { // TODO Why
            Log.i("ApproveFioRequest", "sendViewModel.observer TransactionStatus: $it")
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
        Log.i("asdaf", "asdaf sendViewModel inited")
        initDatabinding(account)
        Log.i("asdaf", "asdaf databinding inited")
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
                .setTitle("Delete received FIO Request")
                .setMessage("Are you sure you want to delete this FIO Request?")
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete) { _, _ ->
                    RejectRequestTask(fioRequestViewModel.payerNameOwnerAccount.value!! as FioAccount, fioRequestViewModel.payerName.value!!,
                            fioRequestViewModel.request.value!!.fioRequestId) {
                        Log.i("asdaf", "asdaf rejection status: ${it?.status}")
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

    override fun onOptionsItemSelected(item: MenuItem?): Boolean =
            when (item?.itemId) {
                android.R.id.home -> {
                    onBackPressed()
                    true
                }
                else -> super.onOptionsItemSelected(item)
            }

    override fun broadcastResult(broadcastResult: BroadcastResult) {
        if (broadcastResult.resultType == BroadcastResultType.SUCCESS) {
            val txid = HexUtils.toHex(signedTransaction.id)
            Log.i("asdaf", "asdaf ihdi: $txid")

            if (fioRequestViewModel.memoTo.value?.isNotEmpty() == true) {
                RecordObtTask(txid, fioRequestViewModel) { success ->
                    if (!success) {
                        Toaster(this).toast("Failed to write memo", false)
                    }
                    val walletManager = mbwManager.getWalletManager(false)
                    val fioModule = walletManager.getModuleById(FioModule.ID) as FioModule

                    walletManager.startSynchronization(SyncMode.NORMAL, fioModule.getAccounts())

                    ApproveFioRequestSuccessActivity.start(this, fioRequestViewModel.amount.value!!,
                            fioRequestViewModel.alternativeAmountFormatted.value!!,
                            sendViewModel.getSelectedFee().value!!, Date().time, fioRequestViewModel.payerName.value!!,
                            fioRequestViewModel.payeeName.value!!, fioRequestViewModel.memoTo.value!!,
                            signedTransaction.id, fioRequestViewModel.payerAccount.value!!.id)
                    finish()
                }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
            } else {
                ApproveFioRequestSuccessActivity.start(this, fioRequestViewModel.amount.value!!,
                        fioRequestViewModel.alternativeAmountFormatted.value!!,
                        sendViewModel.getSelectedFee().value!!, Date().time, fioRequestViewModel.payerName.value!!,
                        fioRequestViewModel.payeeName.value!!, fioRequestViewModel.memoTo.value!!,
                        signedTransaction.id, fioRequestViewModel.payerAccount.value!!.id)
                finish()
            }
        }
    }

//    private fun getDateString(timestamp: Long): String {
//        val date = Date(timestamp * 1000L)
//        val locale = resources.configuration.locale
//
//        val dayFormat = DateFormat.getDateInstance(DateFormat.LONG, locale)
//        val dateString = dayFormat.format(date)
//
//        val hourFormat = DateFormat.getTimeInstance(DateFormat.SHORT, locale)
//        val timeString = hourFormat.format(date)
//
//        return "$dateString at $timeString"
//    }

    class GetPublicAddressTask(
            private val fioName: String,
            private val chainCode: String,
            private val tokenCode: String,
            val listener: ((GetPubAddressResponse) -> Unit)) : AsyncTask<Void, Void, GetPubAddressResponse>() {
        override fun doInBackground(vararg args: Void): GetPubAddressResponse {
            return try {
                FioBlockchainService.getPubkeyByFioAddress(fioName, chainCode, tokenCode)
            } catch (e: IOException) {
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
            val listener: ((Boolean) -> Unit)) : AsyncTask<Void, Void, Boolean>() {
        override fun doInBackground(vararg args: Void): Boolean {
            return try {
                val request = fioRequestViewModel.request.value!!
                val amountInDouble = fioRequestViewModel.amount.value!!.toPlainString().toDouble()
                (fioRequestViewModel.payerNameOwnerAccount.value!! as FioAccount).recordObtData(request.fioRequestId,
                        request.payerFioAddress, request.payeeFioAddress, fioRequestViewModel.payerTokenPublicAddress.value!!,
                        fioRequestViewModel.payeeTokenPublicAddress.value!!, amountInDouble, request.deserializedContent!!.chainCode,
                        request.deserializedContent!!.tokenCode, txid, fioRequestViewModel.memoTo.value!!)
            } catch (e: Exception) {
                Log.i("asdaf", "asdaf failed to write memo: ${e.localizedMessage}")
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
            val listener: ((PushTransactionResponse.ActionTraceResponse?) -> Unit)) : AsyncTask<Void, Void, PushTransactionResponse.ActionTraceResponse?>() {
        override fun doInBackground(vararg args: Void): PushTransactionResponse.ActionTraceResponse? {
            return try {
                fioAccount.rejectFundsRequest(requestId, fioName)
            } catch (e: Exception) {
                Log.i("asdaf", "asdaf failed to reject: ${e.localizedMessage}")
                null
            }
        }

        override fun onPostExecute(result: PushTransactionResponse.ActionTraceResponse?) {
            listener(result)
        }
    }
}