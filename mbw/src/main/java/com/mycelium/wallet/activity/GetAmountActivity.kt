package com.mycelium.wallet.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.Html
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.Window
import android.widget.PopupMenu
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import butterknife.ButterKnife
import butterknife.OnClick
import com.google.common.base.Preconditions
import com.mrd.bitlib.TransactionUtils.MINIMUM_OUTPUT_VALUE
import com.mycelium.view.Denomination
import com.mycelium.wallet.*
import com.mycelium.wallet.NumberEntry.NumberEntryListener
import com.mycelium.wallet.activity.getamount.AmountValidation
import com.mycelium.wallet.activity.getamount.GetAmountViewModel
import com.mycelium.wallet.activity.modern.Toaster
import com.mycelium.wallet.activity.util.toString
import com.mycelium.wallet.activity.util.toStringFriendlyWithUnit
import com.mycelium.wallet.activity.util.toStringWithUnit
import com.mycelium.wallet.databinding.GetAmountActivityBinding
import com.mycelium.wallet.event.ExchangeRatesRefreshed
import com.mycelium.wallet.event.SelectedCurrencyChanged
import com.mycelium.wapi.wallet.Address
import com.mycelium.wapi.wallet.TransactionData
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.btc.FeePerKbFee
import com.mycelium.wapi.wallet.coins.AssetInfo
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.coins.Value.Companion.isNullOrZero
import com.mycelium.wapi.wallet.coins.Value.Companion.valueOf
import com.mycelium.wapi.wallet.erc20.ERC20Account
import com.mycelium.wapi.wallet.erc20.ERC20Account.Companion.TOKEN_TRANSFER_GAS_LIMIT
import com.mycelium.wapi.wallet.eth.EthTransactionData
import com.mycelium.wapi.wallet.exceptions.BuildTransactionException
import com.mycelium.wapi.wallet.exceptions.InsufficientFundsException
import com.mycelium.wapi.wallet.exceptions.OutputTooSmallException
import com.mycelium.wapi.wallet.fiat.coins.FiatType
import com.squareup.otto.Subscribe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.math.BigInteger
import java.util.*

class GetAmountActivity : AppCompatActivity(), NumberEntryListener {

    private lateinit var binding: GetAmountActivityBinding
    private val viewModel: GetAmountViewModel by viewModels()
    private var isSendMode = false
    private var _numberEntry: NumberEntry? = null
    private var _mbwManager: MbwManager? = null
    private var destinationAddress: Address? = null
    private var _kbMinerFee: Value? = null
    private var txData: TransactionData? = null
    private lateinit var mainCurrencyType: AssetInfo

    @SuppressLint("ShowToast")
    public override fun onCreate(savedInstanceState: Bundle?) {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        super.onCreate(savedInstanceState)
        setContentView(GetAmountActivityBinding.inflate(layoutInflater).apply {
            binding = this
            lifecycleOwner = this@GetAmountActivity
            viewModel = this@GetAmountActivity.viewModel
        }.root)
        ButterKnife.bind(this)
        _mbwManager = MbwManager.getInstance(application)
        isSendMode = intent.getBooleanExtra(SEND_MODE, false)
        if (isSendMode) {
            initSendModeAccount()
        } else {
            viewModel.account = _mbwManager!!.selectedAccount
        }
        mainCurrencyType = viewModel.account!!.coinType
        _mbwManager!!.currencySwitcher.defaultCurrency = mainCurrencyType
        viewModel.currentCurrency.value = mainCurrencyType
        initNumberEntry(savedInstanceState)
        if (isSendMode) {
            initSendMode()
        }
        updateUI()
        if (isSendMode) {
            updateERC20RelatedUI()
        }
        checkEntry()
        setupActionBar()
    }

    private fun setupActionBar() {
        supportActionBar?.run {
            title = getString(R.string.amount_title)
            setHomeAsUpIndicator(R.drawable.ic_back_arrow)
            setHomeButtonEnabled(true)
            setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun getMaxDecimal(assetInfo: AssetInfo?): Int =
            (assetInfo as? FiatType)?.unitExponent
                    ?: assetInfo!!.unitExponent - _mbwManager!!.getDenomination(viewModel.account!!.coinType).scale

    private fun initSendMode() {
        // Calculate the maximum amount that can be spent where we send everything we got to another address
        _kbMinerFee = Preconditions.checkNotNull(intent.getSerializableExtra(KB_MINER_FEE) as Value)
        txData = intent.getSerializableExtra(TX_DATA) as TransactionData?
        destinationAddress = (intent.getSerializableExtra(DESTINATION_ADDRESS) as Address?)?.takeIf {
            it.coinType == viewModel.account!!.coinType
        } ?: viewModel.account!!.dummyAddress
        lifecycleScope.launch(Dispatchers.Default) {
            viewModel.maxSpendableAmount.postValue(
                viewModel.account!!.calculateMaxSpendableAmount(_kbMinerFee!!, destinationAddress, txData)
            )
        }

        // if no amount is set, create an null amount with the correct currency
        if (viewModel.amount.value == null) {
            viewModel.amount.value = Value.zeroValue(viewModel.account!!.coinType)
        }
    }

    private fun initSendModeAccount() {
        //we need to have an account, fee, etc to be able to calculate sending related stuff
        val isColdStorage = intent.getBooleanExtra(IS_COLD_STORAGE, false)
        val accountId = Preconditions.checkNotNull(intent.getSerializableExtra(ACCOUNT) as UUID)
        //TODO check all WalletAccount return type in walletmanager, should be some like WalletAccount<Address>
        viewModel.account = _mbwManager!!.getWalletManager(isColdStorage).getAccount(accountId) as WalletAccount<Address>
    }

    private fun initNumberEntry(savedInstanceState: Bundle?) {
        // Load saved state
        viewModel.amount.value = if (savedInstanceState != null) {
            savedInstanceState.getSerializable(ENTERED_AMOUNT) as Value?
        } else {
            intent.getSerializableExtra(ENTERED_AMOUNT) as Value?
        }

        // Init the number pad
        val amountString = if (!isNullOrZero(viewModel.amount.value))
            viewModel.amount.value!!.toString(_mbwManager!!.getDenomination(viewModel.account!!.coinType)) else ""
        viewModel.currentCurrency.value = viewModel.amount.value?.type ?: viewModel.account!!.coinType
        _numberEntry = NumberEntry(getMaxDecimal(viewModel.currentCurrency.value), this, this, amountString)
    }

    @OnClick(R.id.btOk)
    fun onOkClick() {
        if (isNullOrZero(viewModel.amount.value) && isSendMode) {
            return
        }

        // Return the entered value and set a positive result code
        setResult(RESULT_OK, Intent().putExtra(AMOUNT, viewModel.amount.value))
        finish()
    }

    @OnClick(R.id.btMax)
    fun onMaxButtonClick() {
        if (isNullOrZero(viewModel.maxSpendableAmount.value)) {
            Toaster(this).toast(R.string.insufficient_funds, true)
        } else {
            viewModel.amount.value = viewModel.convert(viewModel.maxSpendableAmount.value!!, viewModel.currentCurrency.value!!)
                    ?: viewModel.maxSpendableAmount.value!!
            // set the current shown currency to the amount's currency
            viewModel.currentCurrency.value = viewModel.amount.value!!.type
            updateUI()
            checkEntry()
        }
    }

    @OnClick(R.id.btCurrency)
    fun onSwitchCurrencyClick(view: View) {
        val currencyList = availableCurrencyList()
        if (currencyList.size > 1) {
            val currencyListMenu = PopupMenu(this, view)
            val cryptocurrencies = _mbwManager!!.getWalletManager(false).getCryptocurrenciesSymbols()
            currencyList.forEach { asset ->
                var itemTitle = asset.symbol
                // we want to display cryptocurrency items as "Symbol (denomination if it differs from UNIT)", e.g. "BTC (bits)"
                val denomination = _mbwManager!!.getDenomination(viewModel.account!!.coinType)
                if (cryptocurrencies.contains(asset.symbol) && denomination !== Denomination.UNIT) {
                    itemTitle += " (" + denomination.getUnicodeString(asset.symbol) + ")"
                }
                currencyListMenu.menu.add(Menu.NONE, asset.hashCode(), Menu.NONE, itemTitle)
            }
            currencyListMenu.setOnMenuItemClickListener(PopupMenu.OnMenuItemClickListener { menuItem ->
                currencyList.find { it.hashCode() == menuItem.itemId }?.let { assetInfo ->
                    viewModel.currentCurrency.value = assetInfo
                    viewModel.amount.value?.let {
                        viewModel.amount.value = viewModel.convert(it, assetInfo)
                    }
                    updateUI()
                    return@OnMenuItemClickListener true
                }
                false
            })
            currencyListMenu.show()
        }
    }

    private fun availableCurrencyList(): List<AssetInfo> = mutableListOf<AssetInfo>().apply {
        _mbwManager!!.currencySwitcher.getCurrencyList(mainCurrencyType).forEach { asset ->
            if (viewModel.convert(asset.oneCoin(), mainCurrencyType) != null) {
                add(asset)
            }
        }
    }

    @OnClick(R.id.btPaste)
    fun onPasteButtonClick() {
        amountFromClipboard()?.let {
            setEnteredAmount(it)
            _numberEntry!!.setEntry(viewModel.amount.value!!.valueAsBigDecimal,
                    getMaxDecimal(viewModel.currentCurrency.value))
        }
    }

    @OnClick(R.id.tvHowIsItCalculated)
    fun howIsItCalculatedClick() {
        AlertDialog.Builder(this)
                .setMessage(getString(R.string.how_is_it_calculated_text))
                .setPositiveButton(R.string.button_ok, null)
                .create()
                .show()
    }

    private fun enablePaste(): Boolean = amountFromClipboard() != null

    private fun amountFromClipboard(): String? {
        val content = Utils.getClipboardString(this)
        if (content.isEmpty()) {
            return null
        }
        val number = Utils.truncateAndConvertDecimalString(content.trim { it <= ' ' }, getMaxDecimal(mainCurrencyType))
                ?: return null
        return if (BigDecimal(number).compareTo(BigDecimal.ZERO) < 1) null else number
    }

    private fun updateUI() {
        //update buttons and views

        binding.currencyDropdownImageView.visibility = if (availableCurrencyList().size > 1) View.VISIBLE else View.GONE
        if (viewModel.amount.value != null) {
            // Set current currency name button
            //update amount
            val newAmount = if (viewModel.currentCurrency.value is FiatType) {
                viewModel.amount.value!!.valueAsBigDecimal
            } else {
                val toTargetUnit = _mbwManager!!.getDenomination(viewModel.account!!.coinType).scale
                viewModel.amount.value!!.valueAsBigDecimal.multiply(BigDecimal.TEN.pow(toTargetUnit))
            }
            _numberEntry!!.setEntry(newAmount, getMaxDecimal(viewModel.amount.value!!.type))
        } else {
            binding.tvAmount.text = ""
        }

        // Check whether we can show the paste button
        binding.btPaste.visibility = if (enablePaste()) View.VISIBLE else View.GONE
    }

    private fun updateERC20RelatedUI() {
        (viewModel.account as? ERC20Account)?.ethAcc?.let { parentEthAccount ->
            val gasLimit = (txData as? EthTransactionData)?.gasLimit
                ?: BigInteger.valueOf(TOKEN_TRANSFER_GAS_LIMIT)
            val fee = Value.valueOf(_kbMinerFee!!.type, gasLimit * _kbMinerFee!!.value)
            val isNotEnoughEth = parentEthAccount.accountBalance.spendable.lessThan(fee)

            if (isNotEnoughEth) {
                val denomination = _mbwManager!!.getDenomination(parentEthAccount.coinType)
                val convertedFee = " ~${
                    _mbwManager!!.exchangeRateManager.get(fee, _mbwManager!!.getFiatCurrency(viewModel.account!!.coinType))
                        ?.toStringFriendlyWithUnit() ?: ""
                }"

                binding.tvPleaseTopUp.apply {
                    text =
                        Html.fromHtml(getString(R.string.please_top_up_your_eth_account, parentEthAccount.label, fee.toStringFriendlyWithUnit(denomination), convertedFee))
                    setOnClickListener {
                        _mbwManager!!.setSelectedAccount(parentEthAccount.id)
                        setResult(RESULT_OK, Intent().putExtra(EXIT_TO_MAIN_SCREEN, true))
                        finish()
                    }
                    visibility = View.VISIBLE
                }
                binding.tvParentEthAccountBalanceLabel.text = getString(R.string.parent_eth_account, parentEthAccount.label)
                binding.tvParentEthAccountBalance.text = " ${parentEthAccount.accountBalance.spendable.toStringFriendlyWithUnit(denomination)}"
                binding.llParentEthAccountBalance.visibility = View.VISIBLE
                binding.tvEthRequiredInfo.visibility = View.VISIBLE
                binding.divider.setBackgroundColor(resources.getColor(R.color.fio_red))
            }
        }
    }

    public override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)
        savedInstanceState.putSerializable(ENTERED_AMOUNT, viewModel.amount.value)
    }

    override fun onResume() {
        MbwManager.getEventBus().register(this)
        _mbwManager!!.exchangeRateManager.requestOptionalRefresh()
        binding.btPaste.visibility = if (enablePaste()) View.VISIBLE else View.GONE
        super.onResume()
    }

    override fun onPause() {
        MbwManager.getEventBus().unregister(this)
        viewModel.currentCurrency.value = _mbwManager!!.currencySwitcher.currentCurrencyMap[viewModel.account!!.coinType]
        super.onPause()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
            when (item.itemId) {
                android.R.id.home -> {
                    onBackPressed()
                    true
                }
                else -> super.onOptionsItemSelected(item)
            }

    override fun onEntryChanged(entry: String, wasSet: Boolean) {
        if (!wasSet) {
            // if it was change by the user pressing buttons (show it unformatted)
            setEnteredAmount(entry)
        }
        updateAmountsDisplay(entry)
        checkEntry()
    }

    private fun setEnteredAmount(value: String) {
        viewModel.amount.value = try {
            val value1 = viewModel.currentCurrency.value!!.value(value)
            if (viewModel.currentCurrency.value is FiatType) {
                value1
            } else {
                valueOf(value1.type, _mbwManager!!.getDenomination(viewModel.account!!.coinType).getAmount(value1.value))
            }
        } catch (e: NumberFormatException) {
            Value.zeroValue(viewModel.currentCurrency.value!!)
        }
        if (isSendMode) {
            // enable/disable Max button
            binding.btMax.isEnabled = viewModel.maxSpendableAmount.value?.notEqualsTo(viewModel.amount.value!!)
                    ?: false
        }
    }

    private fun updateAmountsDisplay(amountText: String) {
        // update main-currency display
        binding.tvAmount.text = amountText
        // Set alternate amount if we can
        if (!_mbwManager!!.hasFiatCurrency()
                || !_mbwManager!!.currencySwitcher.isFiatExchangeRateAvailable(viewModel.account!!.coinType)) {
            binding.tvAlternateAmount.text = ""
        } else {
            binding.tvAlternateAmount.text = if (mainCurrencyType == viewModel.currentCurrency.value) {
                // Show Fiat as alternate amount
                val currency = _mbwManager!!.getFiatCurrency(viewModel.account!!.coinType)
                viewModel.convert(viewModel.amount.value!!, currency)
            } else {
                try {
                    viewModel.convert(viewModel.amount.value!!, mainCurrencyType)
                } catch (ex: IllegalArgumentException) {
                    // something failed while calculating the amount
                    null
                }
            }?.toStringWithUnit(_mbwManager!!.getDenomination(viewModel.account!!.coinType))
        }
    }

    private fun checkEntry() {
        if (isSendMode) {
            if (isNullOrZero(viewModel.amount.value)) {
                // Nothing entered
                binding.tvAmount.setTextColor(resources.getColor(R.color.white))
                binding.btOk.isEnabled = false
            } else {
                checkTransaction()
            }
        } else {
            binding.btOk.isEnabled = checkAmount()
        }
    }

    /**
     * Check that the amount is large enough for the network to accept it, and
     * that we have enough funds to send it.
     */
    private fun checkSendAmount(value: Value?, listener: (value: Value?, result: AmountValidation) -> Unit) {
        lifecycleScope.launch(Dispatchers.Default) {
            val result = validateAmount(value)
            withContext(Dispatchers.Main) {
                listener(value, result)
            }
        }
    }

    private fun validateAmount(value: Value?): AmountValidation {
        if (value == null) {
            return AmountValidation.ExchangeRateNotAvailable
        } else if (value.equalZero()) {
            return AmountValidation.Ok //entering a fiat value + exchange is not availible
        }
        try {
            viewModel.account!!.createTx(destinationAddress!!, value, FeePerKbFee(_kbMinerFee!!), txData)
        } catch (e: OutputTooSmallException) {
            return AmountValidation.ValueTooSmall
        } catch (e: InsufficientFundsException) {
            return AmountValidation.NotEnoughFunds
        } catch (e: BuildTransactionException) {
            // under certain conditions the max-miner-fee check fails - report it back to the server, so we can better
            // debug it
            _mbwManager!!.reportIgnoredException("MinerFeeException", e)
            return AmountValidation.Invalid
        } catch (e: Exception) {
            Log.e("GetAmountActivity", "validateAmount", e)
            return AmountValidation.Invalid
        }
        return AmountValidation.Ok
    }

    private fun checkTransaction() {
        // Check whether we have sufficient funds, and whether the output is too small
        var amount = viewModel.amount.value
        // if _amount is not in account's currency then convert to account's currency before checking amount
        if (mainCurrencyType != viewModel.currentCurrency.value) {
            amount = viewModel.convert(viewModel.amount.value!!, mainCurrencyType)
        }
        checkSendAmount(amount) { amount, result ->
            if (result == AmountValidation.Ok) {
                binding.tvAmount.setTextColor(resources.getColor(R.color.white))
            } else {
                binding.tvAmount.setTextColor(resources.getColor(R.color.red))
                if (result == AmountValidation.NotEnoughFunds) {
                    // We do not have enough funds
                    if (amount!!.equalZero() || viewModel.account!!.accountBalance.spendable.lessThan(amount)) {
                        // We do not have enough funds for sending the requested amount
                        Toaster(this@GetAmountActivity).toast(R.string.insufficient_funds, true)
                    } else {
                        // We do have enough funds for sending the requested amount, but
                        // not for the required fee
                        Toaster(this@GetAmountActivity).toast(R.string.insufficient_funds_for_fee, true)
                    }
                } else if (result == AmountValidation.ExchangeRateNotAvailable) {
                    Toaster(this@GetAmountActivity).toast(com.mycelium.wallet.R.string.exchange_rate_unavailable, true)
                }
                // else {
                // The amount we want to send is not large enough for the network to
                // accept it. Don't Toast about it, it's just annoying
                // }
            }
            // Enable/disable Ok button
            binding.btOk.isEnabled = result == AmountValidation.Ok && !viewModel.amount.value!!.isZero()
        }
    }

    private fun checkAmount(): Boolean {
        var amount = viewModel.amount.value
        // if _amount is not in account's currency then convert to account's currency before checking amount
        if (mainCurrencyType != viewModel.currentCurrency.value) {
            amount = viewModel.convert(viewModel.amount.value!!, mainCurrencyType)
        }
        when(viewModel.account?.coinType){
            Utils.getBtcCoinType() -> {
                if (amount != null && BigInteger.ZERO < amount.value && amount.value < MINIMUM_OUTPUT_VALUE.toBigInteger()) {
                    val minAmount = valueOf(viewModel.account?.coinType!!, MINIMUM_OUTPUT_VALUE).toStringWithUnit()
                    binding.error.text = getString(R.string.amount_too_small_short, minAmount)
                    binding.error.isVisible = true
                    return false
                } else {
                    binding.error.isVisible = false
                    return true
                }
            }
            else -> return true
        }
    }


    @Subscribe
    fun exchangeRatesRefreshed(event: ExchangeRatesRefreshed) {
        updateExchangeRateDisplay()
    }

    @Subscribe
    fun selectedCurrencyChanged(event: SelectedCurrencyChanged) {
        updateExchangeRateDisplay()
    }

    private fun updateExchangeRateDisplay() {
        if (viewModel.amount.value != null && _mbwManager!!.currencySwitcher.getExchangeRatePrice(viewModel.account!!.coinType) != null) {
            updateAmountsDisplay(_numberEntry!!.entry)
        }
    }

    companion object {
        const val AMOUNT = "amount"
        const val ENTERED_AMOUNT = "enteredamount"
        const val ACCOUNT = "account"
        const val KB_MINER_FEE = "kbMinerFee"
        const val IS_COLD_STORAGE = "isColdStorage"
        const val DESTINATION_ADDRESS = "destinationAddress"
        const val SEND_MODE = "sendmode"
        const val TX_DATA = "txData"
        const val EXIT_TO_MAIN_SCREEN = "exitToMain"

        /**
         * Get Amount for spending
         */
        fun callMeToSend(currentActivity: Activity, requestCode: Int, account: UUID?, amountToSend: Value?, kbMinerFee: Value?,
                         isColdStorage: Boolean, destinationAddress: Address?, txData: TransactionData?) {
            val intent = Intent(currentActivity, GetAmountActivity::class.java)
                    .putExtra(ACCOUNT, account)
                    .putExtra(ENTERED_AMOUNT, amountToSend)
                    .putExtra(KB_MINER_FEE, kbMinerFee)
                    .putExtra(IS_COLD_STORAGE, isColdStorage)
                    .putExtra(SEND_MODE, true)
                    .putExtra(TX_DATA, txData)
            if (destinationAddress != null) {
                intent.putExtra(DESTINATION_ADDRESS, destinationAddress)
            }
            currentActivity.startActivityForResult(intent, requestCode)
        }

        /**
         * Get Amount for receiving
         */
        fun callMeToReceive(currentActivity: Activity, amountToReceive: Value?, requestCode: Int, currencyType: CryptoCurrency?) {
            val intent = Intent(currentActivity, GetAmountActivity::class.java)
                    .putExtra(ENTERED_AMOUNT, amountToReceive)
                    .putExtra(SEND_MODE, false)
            currentActivity.startActivityForResult(intent, requestCode)
        }
    }
}