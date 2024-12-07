package com.mycelium.wallet.activity.send

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Point
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Html
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.children
import androidx.core.widget.doOnTextChanged
import androidx.databinding.BindingAdapter
import androidx.databinding.InverseBindingAdapter
import androidx.databinding.InverseBindingListener
import androidx.lifecycle.Observer
import com.google.common.base.Strings
import com.mrd.bitlib.crypto.HdKeyNode
import com.mrd.bitlib.util.HexUtils
import com.mycelium.wallet.*
import com.mycelium.wallet.activity.GetAmountActivity
import com.mycelium.wallet.activity.ScanActivity
import com.mycelium.wallet.activity.modern.GetFromAddressBookActivity
import com.mycelium.wallet.activity.send.adapter.BatchAdapter
import com.mycelium.wallet.activity.send.adapter.FeeLvlViewAdapter
import com.mycelium.wallet.activity.send.adapter.FeeViewAdapter
import com.mycelium.wallet.activity.send.event.AmountListener
import com.mycelium.wallet.activity.send.event.BroadcastResultListener
import com.mycelium.wallet.activity.send.model.*
import com.mycelium.wallet.activity.util.collapse
import com.mycelium.wallet.activity.util.expand
import com.mycelium.wallet.activity.util.toStringFriendlyWithUnit
import com.mycelium.wallet.activity.view.VerticalSpaceItemDecoration
import com.mycelium.wallet.content.HandleConfigFactory
import com.mycelium.wallet.databinding.*
import com.mycelium.wapi.content.AssetUri
import com.mycelium.wapi.content.WithCallback
import com.mycelium.wapi.content.btc.BitcoinUri
import com.mycelium.wapi.wallet.*
import com.mycelium.wapi.wallet.btc.bip44.HDAccount
import com.mycelium.wapi.wallet.btc.single.SingleAddressAccount
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.coins.Value.Companion.zeroValue
import com.mycelium.wapi.wallet.erc20.ERC20Account
import com.mycelium.wapi.wallet.erc20.ERC20Account.Companion.AVG_TOKEN_TRANSFER_GAS
import com.mycelium.wapi.wallet.eth.EthAccount
import com.mycelium.wapi.wallet.fio.FioAccount
import com.mycelium.wapi.wallet.fio.FioModule
import org.web3j.utils.Convert
import java.math.BigInteger
import java.util.*
import java.util.concurrent.TimeUnit
import androidx.activity.viewModels
import androidx.core.widget.NestedScrollView

class SendCoinsActivity : AppCompatActivity(), BroadcastResultListener, AmountListener {
    private val viewModel: SendCoinsViewModel by viewModels { SendCoinsFactory(account) }
    private lateinit var mbwManager: MbwManager
    private lateinit var senderFioNamesMenu: PopupMenu
    private var bindingFioMemo: FioMemoInputBinding? = null
    private var bindingFeeSelector: SendCoinsFeeSelectorBinding? = null
    private var bindingAdvEth: SendCoinsAdvancedEthBinding? = null
    private var bindingFeeTitleEth: SendCoinsFeeTitleEthBinding? = null
    private var bindingSenderFio: SendCoinsSenderFioBinding? = null
    private var bindingAdvBlock: SendCoinsAdvancedBlockBinding? = null
    private lateinit var account: WalletAccount<*>

    private val batchAdapter = BatchAdapter().apply {
        clipboardListener = { position, item ->
            viewModel.onClickClipboard(item)
        }
        contactListener = { position, item ->
            startActivityForResult(
                Intent(this@SendCoinsActivity, GetFromAddressBookActivity::class.java)
                    .putExtra(ACCOUNT, viewModel.getAccount().id)
                    .putExtra(IS_COLD_STORAGE, viewModel.isColdStorage()),
                (position + 1).shl(10) or ADDRESS_BOOK_RESULT_CODE
            )
        }
        qrScanListener = { position, item ->
            val config = HandleConfigFactory.returnKeyOrAddressOrUriOrKeynode()
            ScanActivity.callMe(
                this@SendCoinsActivity,
                (position + 1).shl(10) or SCAN_RESULT_CODE, config
            )
        }
        amountListener = { position, item ->
            val account = viewModel.getAccount()
            GetAmountActivity.callMeToSend(
                this@SendCoinsActivity,
                (position + 1).shl(10) or GET_AMOUNT_RESULT_CODE, account.id,
                item.crypto, viewModel.getSelectedFee().value,
                viewModel.isColdStorage(), item.address, viewModel.getTransactionData().value
            )
        }
        closeListener = { position, item ->
            viewModel.removeOutput(item)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mbwManager = MbwManager.getInstance(application)
        val accountId = checkNotNull(intent.getSerializableExtra(ACCOUNT) as UUID)
        val rawPaymentRequest = intent.getByteArrayExtra(RAW_PAYMENT_REQUEST)
        val crashHint = intent.extras!!.keySet().joinToString() + " (account id was $accountId)"
        val isColdStorage = intent.getBooleanExtra(IS_COLD_STORAGE, false)
        account = mbwManager.getWalletManager(isColdStorage).getAccount(accountId)
                ?: throw IllegalStateException(crashHint)

        viewModel.activity = this
        if (!viewModel.isInitialized()) {
            viewModel.init(account, intent)
        }

        if (savedInstanceState != null) {
            viewModel.loadInstance(savedInstanceState)
        }
        //if we do not have a stored receiving address, and got a keynode, we need to figure out the address
        if (viewModel.getReceivingAddress().value == null) {
            val hdKey = intent.getSerializableExtra(HD_KEY) as HdKeyNode?
            if (hdKey != null) {
                viewModel.setReceivingAddressFromKeynode(hdKey, this)
            }
        }

        if (!account.canSpend()) {
            chooseSpendingAccount(rawPaymentRequest)
            return
        }

        // lets see if we got a raw Payment request (probably by downloading a file with MIME application/bitcoin-paymentrequest)
        if (rawPaymentRequest != null && viewModel.hasPaymentRequestHandler()) {
            viewModel.verifyPaymentRequest(rawPaymentRequest, this)
        }

        // lets check whether we got a payment request uri and need to fetch payment data
        val genericUri = viewModel.getGenericUri().value
        if (genericUri is WithCallback && !Strings.isNullOrEmpty((genericUri as WithCallback).callbackURL)
                && !viewModel.hasPaymentRequestHandler()) {
            viewModel.verifyPaymentRequest(genericUri, this)
        }

        initDatabinding(account)

        initFeeView()
        initFeeLvlView()
        supportActionBar?.run {
            title = getString(R.string.send_cointype, viewModel.getAccount().coinType.symbol) + " (${viewModel.getAccount().label})"
            setHomeAsUpIndicator(R.drawable.ic_back_arrow)
            setHomeButtonEnabled(true)
            setDisplayHomeAsUpEnabled(true)
        }
        createSenderFioNamesMenu()
        viewModel.payerFioName.observe(this, Observer {
            updateMemoVisibility()
        })
        viewModel.payeeFioName.observe(this, Observer {
            updateMemoVisibility()
        })
        updateMemoVisibility()
        bindingFioMemo?.etFioMemo?.setOnFocusChangeListener { view, b ->
            if(b) {
                val root = findViewById<ScrollView>(R.id.root)
                root.postDelayed({ root.smoothScrollBy(0, root.maxScrollAmount) }, 500)
            }
        }
        viewModel.outputList.observe(this) {
            batchAdapter.submitList(it)
        }
    }

    private fun updateMemoVisibility() {
        bindingFioMemo?.llFioMemo?.visibility = if (viewModel.payeeFioName.value?.isNotEmpty() == true
                && viewModel.payerFioName.value?.isNotEmpty() == true)
            View.VISIBLE
        else
            View.GONE
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
            when (item.itemId) {
                android.R.id.home -> {
                    onBackPressed()
                    true
                }
                else -> super.onOptionsItemSelected(item)
            }

    override fun onResume() {
        super.onResume()

        // If we don't have a fresh exchange rate, now is a good time to request one, as we will need it in a minute
        if (!mbwManager.currencySwitcher.isFiatExchangeRateAvailable(viewModel.getAccount().coinType)) {
            mbwManager.exchangeRateManager.requestRefresh()
        }

        Handler(Looper.getMainLooper()).postDelayed({ viewModel.updateClipboardUri() }, 300)
        viewModel.activityResultDialog?.show(supportFragmentManager, "ActivityResultDialog")
        viewModel.activityResultDialog = null
    }

    private fun chooseSpendingAccount(rawPaymentRequest: ByteArray?) {
        //we need the user to pick a spending account - the activity will then init sendmain correctly
        val uri: AssetUri = intent.getSerializableExtra(ASSET_URI) as AssetUri?
                ?: BitcoinUri.from(viewModel.getReceivingAddress().value, viewModel.getAmount().value,
                        viewModel.getTransactionLabel().value, null)

        if (rawPaymentRequest != null) {
            GetSpendingRecordActivity.callMeWithResult(this, rawPaymentRequest, REQUEST_PICK_ACCOUNT)
        } else {
            GetSpendingRecordActivity.callMeWithResult(this, uri, REQUEST_PICK_ACCOUNT)
        }
        //no matter whether the user did successfully send or tapped back - we do not want to stay here with a wrong account selected
        finish()
        return
    }

    override fun onPause() {
        mbwManager.versionManager.closeDialog()
        super.onPause()
    }

    private fun initDatabinding(account: WalletAccount<*>) {
        //Data binding, should be called after everything else
        val sendCoinsActivityBinding = when (account) {
            is HDAccount, is SingleAddressAccount -> {
                SendCoinsActivityBtcBinding.inflate(layoutInflater)
                        .also {
                            bindingFioMemo = it.layoutFioMemo
                            bindingFeeSelector = it.layoutFeeBlock.layoutFeeSelector
                            bindingSenderFio = it.layoutSenderFio
                            it.viewModel = viewModel as SendBtcViewModel
                            it.activity = this
                            it.batch.adapter = batchAdapter
                            it.batch.addItemDecoration(VerticalSpaceItemDecoration(resources.getDimensionPixelOffset(R.dimen.size_x4)))
                            it.addBatchAddress.setOnClickListener {
                                viewModel.addEmptyOutput()
                            }
                        }
            }
            is EthAccount, is ERC20Account -> {
                SendCoinsActivityEthBinding.inflate(layoutInflater)
                        .also { binding ->
                            bindingFioMemo = binding.layoutFioMemo
                            bindingFeeSelector = binding.layoutFeeBlock.layoutFeeSelector
                            bindingSenderFio = binding.layoutSenderFio
                            bindingAdvEth = binding.layoutSendAdvBlock.layoutAdvEth
                            bindingFeeTitleEth = binding.layoutFeeBlock.layoutFeeHeap.layoutFeeTitle
                            bindingAdvBlock = binding.layoutSendAdvBlock

                            binding.viewModel = (viewModel as SendEthViewModel).apply {
                                getGasLimitStatus().observe(this@SendCoinsActivity, Observer { status ->
                                    bindingAdvEth?.etGasLimit?.setTextColor(resources.getColor(R.color.white))
                                    when (status) {
                                        SendEthModel.GasLimitStatus.ERROR -> {
                                            bindingAdvEth?.tvGasLimitHelper?.visibility = View.GONE
                                            bindingAdvEth?.tvGasLimitWarning?.visibility = View.GONE
                                            bindingAdvEth?.tvGasLimitError?.visibility = View.VISIBLE
                                            bindingAdvEth?.etGasLimit?.setTextColor(resources.getColor(R.color.fio_red))
                                        }
                                        SendEthModel.GasLimitStatus.WARNING -> {
                                            bindingAdvEth?.tvGasLimitHelper?.visibility = View.GONE
                                            bindingAdvEth?.tvGasLimitWarning?.visibility = View.VISIBLE
                                            bindingAdvEth?.tvGasLimitError?.visibility = View.GONE
                                        }
                                        else -> {
                                            bindingAdvEth?.tvGasLimitHelper?.visibility = View.VISIBLE
                                            bindingAdvEth?.tvGasLimitWarning?.visibility = View.GONE
                                            bindingAdvEth?.tvGasLimitError?.visibility = View.GONE
                                        }
                                    }
                                    bindingAdvEth?.advancedBlock?.layoutParams?.height = ViewGroup.LayoutParams.WRAP_CONTENT
                                    bindingAdvEth?.advancedBlock?.requestLayout()
                                })
                                bindingAdvEth?.etGasLimit?.doOnTextChanged { _, _, _, _ ->
                                        // reset gas limit and therefore UI state
                                        getGasLimit().value = null
                                        getTransactionDataStatus().value = SendCoinsModel.TransactionDataStatus.TYPING
                                }
                                bindingAdvEth?.etGasLimit?.setOnEditorActionListener { textView, actionId, keyEvent ->
                                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                                        textView.clearFocus()
                                    }
                                    false
                                }
                                bindingAdvEth?.etGasLimit?.setOnFocusChangeListener { _, hasFocus ->
                                    if (!hasFocus) {
                                        val gasLimit = bindingAdvEth?.etGasLimit?.text.toString()
                                        getGasLimit().value = if (gasLimit.isEmpty()) null else BigInteger(gasLimit)
                                        getTransactionDataStatus().value = SendCoinsModel.TransactionDataStatus.READY
                                    }
                                }
                                bindingAdvEth?.gasPrice?.doOnTextChanged { _, _, _, _ ->
                                    getGasPrice().value = null
                                    getTransactionDataStatus().value = SendCoinsModel.TransactionDataStatus.TYPING
                                }
                                bindingAdvEth?.gasPrice?.setOnEditorActionListener { textView, actionId, keyEvent ->
                                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                                        textView.clearFocus()
                                    }
                                    false
                                }
                                bindingAdvEth?.gasPrice?.setOnFocusChangeListener { _, hasFocus ->
                                    if (!hasFocus) {
                                        val gasPrice = bindingAdvEth?.gasPrice?.text.toString()
                                        getGasPrice().value = if (gasPrice.isEmpty()) null else Convert.toWei(gasPrice, Convert.Unit.GWEI).toBigInteger()
                                        if (getGasLimit().value == null) {
                                            val limit = getDefaultGasLimit()
                                            bindingAdvEth?.etGasLimit?.setText(limit.toString())
                                            getGasLimit().value = limit
                                        }
                                        getTransactionDataStatus().value = SendCoinsModel.TransactionDataStatus.READY
                                    }
                                }
                                val tvSatFeeValue = findViewById<TextView>(R.id.tvSatFeeValue)
                                getGasPrice().observe(this@SendCoinsActivity, Observer { gp ->
                                    if (gp == null) {
                                        tvSatFeeValue.visibility = View.GONE
                                        bindingFeeSelector?.feeLvlList?.visibility = View.VISIBLE
                                        bindingFeeSelector?.feeValueList?.visibility = View.VISIBLE
                                        bindingFeeTitleEth?.tvFeeUpdatesTimer?.visibility = View.VISIBLE
                                    } else {
                                        val gasLimit =
                                            if (getGasLimitStatus().value != SendEthModel.GasLimitStatus.ERROR) {
                                                getGasLimit().value ?: getDefaultGasLimit()
                                            } else {
                                                getDefaultGasLimit()
                                            }

                                        val totalFee = Value.valueOf(account.basedOnCoinType, gasLimit * gp)
                                        tvSatFeeValue.text =
                                            "${totalFee.toStringFriendlyWithUnit(getDenomination())} ${convert(totalFee)}"

                                        tvSatFeeValue.visibility = View.VISIBLE
                                        bindingFeeSelector?.feeLvlList?.visibility = View.GONE
                                        bindingFeeSelector?.feeValueList?.visibility = View.GONE
                                        bindingFeeTitleEth?.tvFeeUpdatesTimer?.visibility = View.GONE
                                    }
                                })
                                getGasLimit().observe(this@SendCoinsActivity, Observer { gl ->
                                    if (account is ERC20Account) {
                                        var gasLimit = gl
                                        if (gl == null || getGasLimitStatus().value == SendEthModel.GasLimitStatus.ERROR) {
                                            binding.tvThisIsUpdatedFee.visibility = View.GONE
                                            binding.tvHighestPossibleFeeInfo.visibility = View.VISIBLE
                                            binding.llNotEnoughEth.visibility = View.GONE
                                            gasLimit = BigInteger.valueOf(ERC20Account.TOKEN_TRANSFER_GAS_LIMIT)
                                        } else {
                                            binding.tvThisIsUpdatedFee.visibility = View.VISIBLE
                                            binding.tvHighestPossibleFeeInfo.visibility = View.GONE
                                            binding.llNotEnoughEth.visibility = View.GONE
                                        }
                                        val selectedFee = getSelectedFee().value!!
                                        getTotalFee().value = Value.valueOf(selectedFee.type, gasLimit!! * selectedFee.value)
                                    }
                                    getGasPrice().value?.let { gp ->
                                        val gasLimit =
                                            if (getGasLimitStatus().value != SendEthModel.GasLimitStatus.ERROR) {
                                                gl ?: getDefaultGasLimit()
                                            } else {
                                                getDefaultGasLimit()
                                            }
                                        val totalFee = Value.valueOf(account.basedOnCoinType, gasLimit * gp)
                                        tvSatFeeValue.text =
                                            "${totalFee.toStringFriendlyWithUnit(getDenomination())} ${convert(totalFee)}"
                                    }
                                })
                                if (account is ERC20Account) {
                                    getSelectedFee().observe(this@SendCoinsActivity, Observer { selectedFee ->
                                        getEstimatedFee().value = Value.valueOf(selectedFee.type, BigInteger.valueOf(AVG_TOKEN_TRANSFER_GAS) * selectedFee.value)

                                        val gasLimit = getGasLimit().value ?: BigInteger.valueOf(ERC20Account.TOKEN_TRANSFER_GAS_LIMIT)
                                        getTotalFee().value = Value.valueOf(selectedFee.type, gasLimit * selectedFee.value)
                                    })
                                    getEstimatedFee().observe(this@SendCoinsActivity, Observer { estimatedFee ->
                                        binding.tvHighestPossibleFeeInfo.text =
                                            Html.fromHtml(getString(R.string.erc20_highest_possible_fee_info, getParentAccount()!!.label,
                                                                    estimatedFee.toStringFriendlyWithUnit(getDenomination()), convert(estimatedFee)))
                                    })
                                    getTotalFee().observe(this@SendCoinsActivity, Observer { totalFee ->
                                        val parentAccountBalance = getParentAccount()!!.accountBalance.spendable
                                        if (parentAccountBalance.lessThan(totalFee)) {
                                            val diff = totalFee - parentAccountBalance
                                            binding.tvPleaseTopUp.text =
                                                Html.fromHtml(getString(R.string.please_top_up_your_eth_account,
                                                                        getParentAccount()!!.label, diff.toStringFriendlyWithUnit(getDenomination()), convert(diff)))
                                            binding.tvThisIsUpdatedFee.visibility = View.GONE
                                            binding.tvHighestPossibleFeeInfo.visibility = View.GONE
                                            binding.llNotEnoughEth.visibility = View.VISIBLE
                                        }
                                    })
                                    binding.tvParentEthAccountBalanceLabel.text = getString(R.string.parent_eth_account, getParentAccount()!!.label)
                                    binding.tvParentEthAccountBalance.text = " ${getParentAccount()!!.accountBalance.spendable.toStringFriendlyWithUnit(getDenomination())}"
                                    binding.tvPleaseTopUp.setOnClickListener {
                                        mbwManager.setSelectedAccount(getParentAccount()!!.id)
                                        setResult(RESULT_CANCELED)
                                        finish()
                                    }
                                    bindingAdvBlock?.tvTxOptionsLabel?.text = Html.fromHtml(getString(R.string.edit_gas_limit_advanced_users))
                                    bindingAdvBlock?.icEditGasInfo?.visibility = View.VISIBLE
                                } else {
                                    bindingAdvBlock?.tvTxOptionsLabel?.text = Html.fromHtml(getString(R.string.transaction_options_advanced_users))
                                    bindingAdvEth?.icInfoGasLimit?.visibility = View.VISIBLE
                                }
                                isAdvancedBlockExpanded.observe(this@SendCoinsActivity, Observer { isExpanded ->
                                    if (!isExpanded) {
                                        bindingAdvEth?.etGasLimit?.setText("")
                                        getGasLimit().value = null
                                        bindingAdvEth?.gasPrice?.setText("")
                                        getGasPrice().value = null
                                        bindingAdvEth?.spinner?.setSelection(0)
                                        getTransactionDataStatus().value = SendCoinsModel.TransactionDataStatus.READY
                                    }
                                })
                                bindingAdvEth?.spinner?.adapter = ArrayAdapter(context,
                                                                R.layout.layout_send_coin_transaction_replace, R.id.text, getTxItems()).apply {
                                    this.setDropDownViewResource(R.layout.layout_send_coin_transaction_replace_dropdown)
                                }
                            }
//                            it.activity = this
                        }
            }
            is FioAccount -> {
                SendCoinsActivityFioBinding.inflate(layoutInflater)
                        .also {
                            bindingFioMemo = it.layoutFioMemo
                            bindingSenderFio = it.layoutSenderFio

                            it.viewModel = viewModel as SendFioViewModel
                            it.activity = this
                        }
            }
            else -> getDefaultBinding()
        }
        sendCoinsActivityBinding.lifecycleOwner = this
        setContentView(sendCoinsActivityBinding.root)
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val v = currentFocus
            if (v is EditText && v.id == R.id.etGasLimit) {
                val outRect = Rect()
                v.getGlobalVisibleRect(outRect)
                if (!outRect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                    v.clearFocus()
                    val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0)
                }
            }
        }
        return super.dispatchTouchEvent(event)
    }

    private fun getDefaultBinding(): SendCoinsActivityBinding =
            SendCoinsActivityBinding.inflate(layoutInflater)
                    .also {
                        bindingFioMemo = it.layoutFioMemo
                        bindingFeeSelector = it.layoutFeeBlock.layoutFeeSelector
                        bindingSenderFio = it.layoutSenderFio

                        it.viewModel = viewModel
                        it.activity = this
                    }

    private fun initFeeView() {
        bindingFeeSelector?.feeValueList?.setHasFixedSize(true)

        val displaySize = Point()
        windowManager.defaultDisplay.getSize(displaySize)
        val feeFirstItemWidth = (displaySize.x - resources.getDimensionPixelSize(R.dimen.item_dob_width)) / 2

        val feeViewAdapter = FeeViewAdapter(feeFirstItemWidth)
        feeViewAdapter.setFormatter(viewModel.getFeeFormatter())

        bindingFeeSelector?.feeValueList?.adapter = feeViewAdapter
        feeViewAdapter.setDataset(viewModel.getFeeDataset().value)
        viewModel.getFeeDataset().observe(this, Observer { feeItems ->
            feeViewAdapter.setDataset(feeItems)
            val selectedFee = viewModel.getSelectedFee().value!!
            if (feeViewAdapter.selectedItem >= feeViewAdapter.itemCount ||
                    feeViewAdapter.getItem(feeViewAdapter.selectedItem).feePerKb != selectedFee.valueAsLong) {
                bindingFeeSelector?.feeValueList?.setSelectedItem(selectedFee)
            }
        })

        bindingFeeSelector?.feeValueList?.setSelectListener { adapter, position ->
            val item = (adapter as FeeViewAdapter).getItem(position)
            viewModel.getSelectedFee().value = Value.valueOf(item.value.type, item.feePerKb)
            val root = findViewById<NestedScrollView>(R.id.root)
            if (viewModel.isSendScrollDefault() && root.maxScrollAmount - root.scaleY > 0) {
                root.smoothScrollBy(0, root.maxScrollAmount)
                viewModel.setSendScrollDefault(false)
            }
        }
    }

    private fun initFeeLvlView() {
        bindingFeeSelector?.feeLvlList?.setHasFixedSize(true)
        val feeLvlItems = viewModel.getFeeLvlItems()

        val displaySize = Point()
        windowManager.defaultDisplay.getSize(displaySize)
        val feeFirstItemWidth = (displaySize.x - resources.getDimensionPixelSize(R.dimen.item_dob_width)) / 2
        bindingFeeSelector?.feeLvlList?.adapter = FeeLvlViewAdapter(feeLvlItems, feeFirstItemWidth)
        bindingFeeSelector?.feeLvlList?.setSelectListener { adapter, position ->
            val item = (adapter as FeeLvlViewAdapter).getItem(position)
            viewModel.getFeeLvl().value = item.minerFee
            bindingFeeSelector?.feeValueList?.setSelectedItem(viewModel.getSelectedFee().value)
        }
        bindingFeeSelector?.feeLvlList?.setSelectedItem(viewModel.getFeeLvl().value)
    }

    fun onClickUnconfirmedWarning() {
        AlertDialog.Builder(this)
                .setTitle(R.string.spending_unconfirmed_title)
                .setMessage(R.string.spending_unconfirmed_description)
                .setPositiveButton(android.R.string.ok, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show()
    }

    fun onColuTipClick() {
        AlertDialog.Builder(this)
                .setMessage(R.string.tips_rmc_check_address)
                .setPositiveButton(R.string.button_ok, null)
                .create()
                .show()
    }

    override fun onClickAmount() {
        val account = viewModel.getAccount()
        GetAmountActivity.callMeToSend(this, GET_AMOUNT_RESULT_CODE, account.id,
                viewModel.getAmount().value, viewModel.getSelectedFee().value,
                viewModel.isColdStorage(), viewModel.getReceivingAddress().value, viewModel.getTransactionData().value)
    }

    fun onClickScan() {
        val config = HandleConfigFactory.returnKeyOrAddressOrUriOrKeynode()
        ScanActivity.callMe(this, SCAN_RESULT_CODE, config)
    }

    fun onClickAddressBook() {
        startActivityForResult(Intent(this, GetFromAddressBookActivity::class.java)
                .putExtra(ACCOUNT, viewModel.getAccount().id)
                .putExtra(IS_COLD_STORAGE, viewModel.isColdStorage()),
                ADDRESS_BOOK_RESULT_CODE)
    }

    fun onClickManualEntry() {
        val intent = Intent(this, ManualAddressEntry::class.java)
                .putExtra(ACCOUNT, viewModel.getAccount().id)
                .putExtra(IS_COLD_STORAGE, viewModel.isColdStorage())
        startActivityForResult(intent, MANUAL_ENTRY_RESULT_CODE)
    }

    fun onClickBatch() {
        (viewModel as? SendBtcViewModel)?.run {
            isBatch.value = true
            addEmptyOutput()
        }
    }

    fun onClickSenderFioNames() {
        senderFioNamesMenu.show()
    }

    private fun createSenderFioNamesMenu() {
        val fioModule = mbwManager.getWalletManager(false).getModuleById(FioModule.ID) as FioModule
        val now = Date()
        val fioNames = fioModule.getFIONames(viewModel.getAccount()).filter { it.expireDate.after(now) }
        if (fioNames.isEmpty()) {
            bindingSenderFio?.sender?.visibility = View.GONE
        } else {
            senderFioNamesMenu = PopupMenu(this, bindingSenderFio?.ivFromFioName).apply {
                fioNames.forEach {
                    menu.add(it.name)
                }
                setOnMenuItemClickListener { item ->
                    // btcViewModel.setAddressType(AddressType.values()[item.itemId])
                    bindingSenderFio?.tvFrom?.text = item.title
                    getSharedPreferences(Constants.SETTINGS_NAME, MODE_PRIVATE)
                            .edit()
                            .putString(Constants.LAST_FIO_SENDER, "${item.title}")
                            .apply()
                    false
                }
                val fioSender = getSharedPreferences(Constants.SETTINGS_NAME, MODE_PRIVATE)
                        .getString(Constants.LAST_FIO_SENDER, fioNames.first().name)
                if (menu.children.any { it.title == fioSender }) {
                    viewModel.payerFioName.postValue(fioSender)
                }
            }
        }
    }

    fun onClickSend() {
        viewModel.fioMemo.value = bindingFioMemo?.etFioMemo?.text.toString()
        if (isPossibleDuplicateSending()) {
            AlertDialog.Builder(this)
                    .setTitle(R.string.possible_duplicate_warning_title)
                    .setMessage(R.string.possible_duplicate_warning_desc)
                    .setPositiveButton(android.R.string.yes) { _: DialogInterface?, _: Int -> viewModel.sendTransaction(this) }
                    .setNegativeButton(android.R.string.no) { _: DialogInterface?, _: Int -> finish() }
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show()
        } else {
            viewModel.sendTransaction(this)
        }
    }

    fun showInputDataInfo() {
        AlertDialog.Builder(this, R.style.MyceliumModern_Dialog)
                .setTitle(R.string.input_data_format)
                .setMessage(R.string.input_data_format_desc)
                .setPositiveButton(R.string.button_ok, null)
                .create()
                .show()
    }

    fun showGasLimitInfo() {
        AlertDialog.Builder(this, R.style.MyceliumModern_Dialog)
                .setTitle(R.string.gas_limit_info_title)
                .setMessage(R.string.gas_limit_info_desc)
                .setPositiveButton(R.string.button_ok, null)
                .create()
                .show()
    }

    fun showTxReplaceInfo() {
        AlertDialog.Builder(this, R.style.MyceliumModern_Dialog)
                .setTitle(R.string.tx_replace_info_title)
                .setMessage(R.string.tx_replacae_info_desc)
                .setPositiveButton(R.string.button_ok, null)
                .create()
                .show()
    }

    /**
     * Checks whether the last outgoing transaction that was sent recently (within 10 minutes)
     * has the same amount and receiving address to warn a user about possible duplicate sending.
     */
    private fun isPossibleDuplicateSending(): Boolean {
        // we could have used getTransactionsSince here instead of getTransactionSummaries
        // but for accounts with large number of transactions (>500) it would introduce quite delay
        // so we take last 25 transactions as a sort of heuristic
        val summaries: List<TransactionSummary> = viewModel.getAccount().getTransactionSummaries(0, 25)
        if (summaries.isEmpty()) {
            return false // user has no transactions
        }
        if (summaries[0].timestamp * 1000 < System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(10)) {
            return false // latest transaction is too old
        }
        // find latest outgoing transaction
        var outgoingTx: TransactionSummary? = null
        for (summary in summaries) {
            if (!summary.isIncoming) {
                outgoingTx = summary
                break
            }
        }
        if (outgoingTx == null) {
            return false // no outgoing transactions
        }
        // extract sent amount from the transaction
        var outgoingTxAmount = zeroValue(viewModel.getAccount().coinType)
        for (output in outgoingTx.outputs) {
            if (output.address == viewModel.getReceivingAddress().value) {
                outgoingTxAmount = output.value
            }
        }
        return outgoingTx.destinationAddresses.size > 0 && outgoingTx.destinationAddresses[0] == viewModel.getReceivingAddress().value &&
                outgoingTxAmount == viewModel.getAmount().value
    }

    override fun onSaveInstanceState(outState: Bundle) {
        viewModel.saveInstance(outState)
        super.onSaveInstanceState(outState)
    }

    override fun broadcastResult(broadcastResult: BroadcastResult) {
        val result = Intent()
        if (broadcastResult.resultType == BroadcastResultType.SUCCESS) {
            val signedTransaction = viewModel.getSignedTransaction()!!
            viewModel.getTransactionLabel().value?.run {
                mbwManager.metadataStorage.storeTransactionLabel(HexUtils.toHex(signedTransaction.id), this)
            }
            val hash = HexUtils.toHex(signedTransaction.id)
            val fiat = viewModel.getFiatValue()
            fiat?.run {
                val pref = getSharedPreferences(TRANSACTION_FIAT_VALUE, Context.MODE_PRIVATE).edit()
                if (viewModel.isBatch.value == true) {
                    pref.putString(BATCH_HASH_PREFIX + hash, fiat)
                } else {
                    pref.putString(hash, fiat)
                }
                pref.apply()
            }
            result.putExtra(Constants.TRANSACTION_FIAT_VALUE_KEY, fiat)
                    .putExtra(Constants.TRANSACTION_ID_INTENT_KEY, hash)
        }
        val resultType = if (broadcastResult.resultType == BroadcastResultType.SUCCESS) {
            Activity.RESULT_OK
        } else {
            Activity.RESULT_CANCELED
        }
        setResult(resultType, result)
        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        viewModel.processReceivedResults(requestCode, resultCode, data, this)
        super.onActivityResult(requestCode, resultCode, data)
    }

    companion object {
        const val GET_AMOUNT_RESULT_CODE = 1
        const val SCAN_RESULT_CODE = 2
        const val ADDRESS_BOOK_RESULT_CODE = 3
        const val MANUAL_ENTRY_RESULT_CODE = 4
        const val REQUEST_PICK_ACCOUNT = 5
        const val SIGN_TRANSACTION_REQUEST_CODE = 6
        const val REQUEST_PAYMENT_HANDLER = 8
        const val RAW_PAYMENT_REQUEST = "rawPaymentRequest"

        internal const val ACCOUNT = "account"
        internal const val IS_COLD_STORAGE = "isColdStorage"
        internal const val AMOUNT = "amount"
        internal const val RECEIVING_ADDRESS = "receivingAddress"
        internal const val HD_KEY = "hdKey"
        internal const val TRANSACTION_LABEL = "transactionLabel"
        internal const val ASSET_URI = "assetUri"
        const val SIGNED_TRANSACTION = "signedTransaction"
        const val TRANSACTION_FIAT_VALUE = "transaction_fiat_value"
        const val BATCH_HASH_PREFIX = "batch_"

        @JvmStatic
        fun getIntent(currentActivity: Activity, account: UUID, isColdStorage: Boolean): Intent =
                Intent(currentActivity, SendCoinsActivity::class.java)
                        .putExtra(ACCOUNT, account)
                        .putExtra(IS_COLD_STORAGE, isColdStorage)

        @JvmStatic
        fun getIntent(currentActivity: Activity, account: UUID,
                      amountToSend: Value, receivingAddress: Address, isColdStorage: Boolean): Intent =
                getIntent(currentActivity, account, isColdStorage)
                        .putExtra(AMOUNT, amountToSend)
                        .putExtra(RECEIVING_ADDRESS, receivingAddress)

        @JvmStatic
        fun getIntent(currentActivity: Activity, account: UUID, rawPaymentRequest: ByteArray,
                      isColdStorage: Boolean): Intent =
                getIntent(currentActivity, account, isColdStorage)
                        .putExtra(RAW_PAYMENT_REQUEST, rawPaymentRequest)

        @JvmStatic
        fun getIntent(currentActivity: Activity, account: UUID, uri: AssetUri, isColdStorage: Boolean): Intent =
                getIntent(currentActivity, account, isColdStorage)
                        .putExtra(AMOUNT, uri.value)
                        .putExtra(RECEIVING_ADDRESS, uri.address)
                        .putExtra(TRANSACTION_LABEL, uri.label)
                        .putExtra(ASSET_URI, uri)

        @JvmStatic
        fun getIntent(currentActivity: Activity, account: UUID, hdKey: HdKeyNode): Intent =
                getIntent(currentActivity, account, false)
                        .putExtra(HD_KEY, hdKey)
    }
}

@BindingAdapter("errorAnimatedText")
fun setVisibilityAnimated(target: TextView, error: CharSequence) {
    if (error.isNotEmpty()) {
        target.text = error
        target.expand()
    } else {
        target.collapse()
    }
}

@BindingAdapter(value = ["animatedVisibility", "activity"], requireAll = false)
fun setVisibilityAnimated(target: View, visible: Boolean, activity: SendCoinsActivity?) {
    if (visible) {
        target.expand {
            activity?.findViewById<ScrollView>(R.id.root)?.let {
                it.smoothScrollTo(0, it.measuredHeight)
            }
        }
    } else {
        target.collapse()
    }
}

@BindingAdapter(value = ["selectedItem", "selectedItemAttrChanged"], requireAll = false)
fun setSpinnerListener(spinner: Spinner, spinnerItem: SpinnerItem, listener: InverseBindingListener) {
    spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) = listener.onChange()
        override fun onNothingSelected(adapterView: AdapterView<*>) = listener.onChange()
    }
}

@InverseBindingAdapter(attribute = "selectedItem")
fun getSelectedItem(spinner: Spinner): SpinnerItem {
    return spinner.selectedItem as SpinnerItem
}

interface SpinnerItem

class TransactionItem(val tx: TransactionSummary, private val dateString: String,
                      private val amountString: String) : SpinnerItem {
    override fun toString(): String {
        val idHex = HexUtils.toHex(tx.id)
        val idString = "${idHex.substring(0, 6)}…${idHex.substring(idHex.length - 2)}"
        return "$idString - $dateString, $amountString"
    }
}

class NoneItem : SpinnerItem {
    override fun toString(): String = WalletApplication.getInstance().getString(R.string.none)
    override fun equals(other: Any?) = this.toString() == other.toString()
}
