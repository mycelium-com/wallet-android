package com.mycelium.wallet.activity.main

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.common.base.Preconditions
import com.mycelium.wallet.Constants
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import com.mycelium.wallet.activity.BipSsImportActivity.Companion.callMe
import com.mycelium.wallet.activity.HandleUrlActivity
import com.mycelium.wallet.activity.ScanActivity
import com.mycelium.wallet.activity.StringHandlerActivity
import com.mycelium.wallet.activity.modern.Toaster
import com.mycelium.wallet.activity.pop.PopActivity
import com.mycelium.wallet.activity.receive.ReceiveCoinsActivity.Companion.callMe
import com.mycelium.wallet.activity.send.SendCoinsActivity.Companion.getIntent
import com.mycelium.wallet.activity.send.SendInitializationActivity.Companion.callMe
import com.mycelium.wallet.activity.send.SendInitializationActivity.Companion.callMeWithResult
import com.mycelium.wallet.activity.util.getAddress
import com.mycelium.wallet.activity.util.getAssetUri
import com.mycelium.wallet.activity.util.getBitIdRequest
import com.mycelium.wallet.activity.util.getHdKeyNode
import com.mycelium.wallet.activity.util.getPopRequest
import com.mycelium.wallet.activity.util.getPrivateKey
import com.mycelium.wallet.activity.util.getShare
import com.mycelium.wallet.activity.util.getUri
import com.mycelium.wallet.activity.util.toStringWithUnit
import com.mycelium.wallet.bitid.BitIDAuthenticationActivity
import com.mycelium.wallet.content.HandleConfigFactory.genericScanRequest
import com.mycelium.wallet.content.ResultType
import com.mycelium.wallet.databinding.MainBalanceViewBinding
import com.mycelium.wallet.event.AccountChanged
import com.mycelium.wallet.event.BalanceChanged
import com.mycelium.wallet.event.ExchangeRatesRefreshed
import com.mycelium.wallet.event.ExchangeSourceChanged
import com.mycelium.wallet.event.RefreshingExchangeRatesFailed
import com.mycelium.wallet.event.SelectedAccountChanged
import com.mycelium.wallet.event.SelectedCurrencyChanged
import com.mycelium.wallet.event.SyncStopped
import com.mycelium.wapi.wallet.SyncMode
import com.mycelium.wapi.wallet.bch.bip44.Bip44BCHAccount
import com.mycelium.wapi.wallet.bch.single.SingleAddressBCHAccount
import com.mycelium.wapi.wallet.btc.bip44.UnrelatedHDAccountConfig
import com.mycelium.wapi.wallet.coins.AssetInfo
import com.mycelium.wapi.wallet.coins.Balance
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.coins.Value.Companion.zeroValue
import com.mycelium.wapi.wallet.colu.ColuAccount
import com.squareup.otto.Subscribe
import java.math.BigDecimal

class BalanceFragment : Fragment() {
    private var _mbwManager: MbwManager? = null
    private var _exchangeRatePrice: Double? = null
    private var _toaster: Toaster? = null

    var binding: MainBalanceViewBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = MainBalanceViewBinding.inflate(inflater).apply {
        binding = this
    }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.llBalance?.setOnClickListener {
            val account = _mbwManager!!.selectedAccount
            _mbwManager!!.getWalletManager(false)
                .startSynchronization(
                    SyncMode.NORMAL_FORCED,
                    listOf(account)
                )
        }
        updateExchangeSourceMenu()
        binding?.layoutActoins?.btSend?.setOnClickListener {
            onClickSend()
        }
        binding?.layoutActoins?.btReceive?.setOnClickListener {
            onClickReceive()
        }
        binding?.layoutActoins?.btScan?.setOnClickListener {
            onClickScan()
        }
    }

    private fun updateExchangeSourceMenu() {
        val exchangeMenu = PopupMenu(activity, binding?.exchangeSourceLayout)
        binding?.exchangeSourceLayout!!.setOnClickListener { exchangeMenu.show() }

        val exchangeRateManager = _mbwManager!!.exchangeRateManager
        val selectedAccount = _mbwManager!!.selectedAccount
        val sources = exchangeRateManager.getExchangeSourceNames(selectedAccount.coinType.symbol)
        val sourcesAndValues: MutableMap<String, String> = HashMap() // Needed for popup menu

        sources.sortWith { rate1, rate2 ->
            rate1.compareTo(
                rate2,
                ignoreCase = true
            )
        }

        sources.forEach { source ->
            val exchangeRate = exchangeRateManager.getExchangeRate(
                selectedAccount.coinType.symbol,
                _mbwManager!!.getFiatCurrency(selectedAccount.coinType).symbol, source
            )
            val price = if (exchangeRate?.price == null)
                "not available"
            else
                (BigDecimal(exchangeRate.price).setScale(2, BigDecimal.ROUND_DOWN).toPlainString() +
                        " " + _mbwManager!!.getFiatCurrency(selectedAccount.coinType).symbol)
            var item = if (selectedAccount is ColuAccount) {
                COINMARKETCAP + "/" + source
            } else {
                "$source ($price)"
            }
            sourcesAndValues[item] = source
            exchangeMenu.menu.add(item)
        }

        // if we ended up with not existent source name for current cryptocurrency (CC)
        // after we have switched accounts for different CC
        // then use the default exchange
        if (sources.size != 0 && !sources.contains(
                exchangeRateManager.getCurrentExchangeSourceName(
                    selectedAccount.coinType.symbol
                )
            )
        ) {
            exchangeRateManager.setCurrentExchangeSourceName(
                selectedAccount.coinType.symbol,
                Constants.DEFAULT_EXCHANGE
            )
        }

        exchangeMenu.setOnMenuItemClickListener { item ->
            exchangeRateManager.setCurrentExchangeSourceName(
                selectedAccount.coinType.symbol,
                sourcesAndValues[item.title.toString()]
            )
            false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setHasOptionsMenu(true)
        super.onCreate(savedInstanceState)
    }

    override fun onAttach(context: Context) {
        _mbwManager = MbwManager.getInstance(context)
        _toaster = Toaster(this)
        super.onAttach(context)
    }

    override fun onStart() {
        MbwManager.getEventBus().register(this)
        _exchangeRatePrice =
            _mbwManager!!.currencySwitcher.getExchangeRatePrice(_mbwManager!!.selectedAccount.coinType)
        if (_exchangeRatePrice == null) {
            _mbwManager!!.exchangeRateManager.requestRefresh()
        }

        updateUi()
        super.onStart()
    }

    override fun onStop() {
        MbwManager.getEventBus().unregister(this)
        super.onStop()
    }

    fun onClickSend() {
        if (isBCH) {
            return
        }
        val account = Preconditions.checkNotNull(
            _mbwManager!!.selectedAccount
        )
        if (account.canSpend()) {
            if (account is ColuAccount && account.accountBalance.spendable.equalZero()) {
                AlertDialog.Builder(activity)
                    .setMessage(getString(R.string.rmc_send_warning, account.coinType.name))
                    .setPositiveButton(
                        R.string.button_ok
                    ) { dialogInterface, i ->
                        callMe(
                            requireActivity(),
                            _mbwManager!!.selectedAccount.id,
                            false
                        )
                    }
                    .create()
                    .show()
            } else {
                callMe(
                    this@BalanceFragment.requireActivity(),
                    _mbwManager!!.selectedAccount.id,
                    false
                )
            }
        } else {
            AlertDialog.Builder(activity)
                .setMessage(R.string.this_is_read_only_account)
                .setPositiveButton(R.string.button_ok, null).create().show()
        }
    }

    fun onClickReceive() {
        callMe(
            activity!!, _mbwManager!!.selectedAccount,
            _mbwManager!!.selectedAccount.canSpend(), true
        )
    }

    fun onClickScan() {
        if (isBCH) {
            return
        }
        //perform a generic scan, act based upon what we find in the QR code
        val config = genericScanRequest()
        ScanActivity.callMe(this, GENERIC_SCAN_REQUEST, config)
    }

    private val isBCH: Boolean
        get() = _mbwManager!!.selectedAccount is Bip44BCHAccount
                || _mbwManager!!.selectedAccount is SingleAddressBCHAccount

    private fun updateUi() {
        if (!isAdded || _mbwManager!!.selectedAccount.isArchived) {
            return
        }
        val account = Preconditions.checkNotNull(
            _mbwManager!!.selectedAccount
        )
        binding?.tcdFiatDisplay?.coinType = account.coinType
        updateUiKnownBalance(Preconditions.checkNotNull(account.accountBalance), account.coinType)


        // Set BTC rate
        if (!_mbwManager!!.hasFiatCurrency()) {
            // No fiat currency selected by user
            binding?.tvBtcRate?.visibility = View.INVISIBLE
            binding?.exchangeSourceLayout?.visibility = View.GONE
        } else {
            val value =
                _mbwManager!!.exchangeRateManager[account.coinType.oneCoin(), _mbwManager!!.getFiatCurrency(
                    account.coinType
                )]
            val exchange = _mbwManager!!.exchangeRateManager.getCurrentExchangeSourceName(
                _mbwManager!!.selectedAccount.coinType.symbol
            )
            binding?.tvBtcRate?.text = if (value == null) {
                // We have no price, exchange not available
                // or no exchange rate providers for the account
                if (exchange != null) {
                    resources.getString(R.string.exchange_source_not_available, exchange)
                } else {
                    resources.getString(R.string.no_exchange_available)
                }
            } else {
                resources.getString(
                    R.string.balance_rate,
                    account.coinType.symbol,
                    _mbwManager!!.getFiatCurrency(account.coinType).symbol,
                    value.toFriendlyString()
                )
            }
            binding?.tvBtcRate?.visibility = View.VISIBLE
            binding?.exchangeSource?.text = exchange
            binding?.exchangeSourceLayout?.visibility =
                if (exchange != null) View.VISIBLE else View.GONE
        }
    }

    private fun updateUiKnownBalance(balance: Balance, coinType: AssetInfo) {
        val valueString: CharSequence =
            balance.spendable.toStringWithUnit(_mbwManager!!.getDenomination(coinType))
        binding?.tvBalance?.text = valueString

        // Show alternative values
        binding?.tcdFiatDisplay?.setValue(balance.spendable)

        // Show/Hide Receiving
        if (balance.pendingReceiving.isPositive()) {
            val receivingString = balance.pendingReceiving.toStringWithUnit(
                _mbwManager!!.getDenomination(coinType)
            )
            val receivingText = resources.getString(R.string.receiving, receivingString)
            val tvReceiving = binding?.tvReceiving
            binding?.tvReceiving?.text = receivingText
            binding?.tvReceiving?.visibility = View.VISIBLE
        } else {
            binding?.tvReceiving?.visibility = View.GONE
        }
        // show fiat value (if balance is in btc)
        setFiatValue(binding!!.tvReceivingFiat, balance.pendingReceiving, true)

        // Show/Hide Sending
        if (balance.sendingToForeignAddresses.isPositive()) {
            val sendingString = balance.sendingToForeignAddresses
                .toStringWithUnit(_mbwManager!!.getDenomination(coinType))
            val sendingText = resources.getString(R.string.sending, sendingString)
            val tvSending = binding?.tvSending
            tvSending?.text = sendingText
            tvSending?.visibility = View.VISIBLE
        } else {
            binding?.tvSending?.visibility = View.GONE
        }
        // show fiat value (if balance is in btc)
        setFiatValue(binding!!.tvSendingFiat, balance.sendingToForeignAddresses, true)

        // set exchange item
        binding?.exchangeSource?.text =
            _mbwManager!!.exchangeRateManager.getCurrentExchangeSourceName(
                _mbwManager!!.selectedAccount.coinType.symbol
            )
    }

    private fun setFiatValue(tv: TextView, value: Value, hideOnZeroBalance: Boolean) {
        if (!_mbwManager!!.hasFiatCurrency() || _exchangeRatePrice == null || (hideOnZeroBalance && !value.isPositive())) {
            tv.visibility = View.GONE
        } else {
            try {
                val converted =
                    _mbwManager!!.exchangeRateManager[value, _mbwManager!!.getFiatCurrency(
                        _mbwManager!!.selectedAccount.coinType
                    )]
                if (converted != null) {
                    tv.visibility = View.VISIBLE
                    tv.text = converted.toStringWithUnit()
                } else {
                    tv.visibility = View.GONE
                }
            } catch (ex: IllegalArgumentException) {
                // something failed while calculating the bitcoin amount
                tv.visibility = View.GONE
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == GENERIC_SCAN_REQUEST) {
            if (resultCode != Activity.RESULT_OK) {
                //report to user in case of error
                //if no scan handlers match successfully, this is the last resort to display an error msg
                ScanActivity.toastScanError(resultCode, data, activity)
            } else {
                val type =
                    data!!.getSerializableExtra(StringHandlerActivity.RESULT_TYPE_KEY) as ResultType?
                when (type) {
                    ResultType.PRIVATE_KEY -> {
                        val key = data.getPrivateKey()
                        // ask user what WIF privkey he/she scanned as there are options
                        val selectedItem = IntArray(1)
                        val choices = arrayOfNulls<CharSequence>(2)
                        choices[0] = "BTC"
                        choices[1] = "FIO"
                        AlertDialog.Builder(requireActivity())
                            .setTitle("Choose blockchain")
                            .setSingleChoiceItems(
                                choices, 0
                            ) { dialogInterface: DialogInterface?, i: Int ->
                                selectedItem[0] = i
                            }
                            .setPositiveButton(requireActivity().getString(R.string.ok)) { dialogInterface: DialogInterface?, i: Int ->
                                val account = if (selectedItem[0] == 0) {
                                    _mbwManager!!.createOnTheFlyAccount(
                                        key,
                                        Utils.getBtcCoinType()
                                    )
                                } else {
                                    _mbwManager!!.createOnTheFlyAccount(
                                        key,
                                        Utils.getFIOCoinType()
                                    )
                                }
                                //we dont know yet where at what to send
                                callMeWithResult(
                                    requireActivity(), account, true,
                                    StringHandlerActivity.SEND_INITIALIZATION_CODE
                                )
                            }
                            .setNegativeButton(this.getString(R.string.cancel), null)
                            .show()
                    }

                    ResultType.ADDRESS -> {
                        val address = data.getAddress()
                        startActivity(
                            getIntent(
                                activity!!,
                                _mbwManager!!.selectedAccount.id,
                                zeroValue(_mbwManager!!.selectedAccount.coinType),
                                address, false
                            )
                                .addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
                        )
                    }

                    ResultType.ASSET_URI -> {
                        val uri = data.getAssetUri()
                        startActivity(
                            getIntent(activity!!, _mbwManager!!.selectedAccount.id, uri, false)
                                .addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
                        )
                    }

                    ResultType.HD_NODE -> {
                        val hdKeyNode = data.getHdKeyNode()
                        if (hdKeyNode.isPrivateHdKeyNode) {
                            //its an xPriv, we want to cold-spend from it
                            val tempWalletManager = _mbwManager!!.getWalletManager(true)
                            val acc = tempWalletManager.createAccounts(
                                UnrelatedHDAccountConfig(
                                    listOf(
                                        hdKeyNode
                                    )
                                )
                            )[0]
                            tempWalletManager.startSynchronization(acc)
                            callMeWithResult(
                                activity!!, acc, true,
                                StringHandlerActivity.SEND_INITIALIZATION_CODE
                            )
                        } else {
                            //its xPub, we want to send to it
                            val intent =
                                getIntent(activity!!, _mbwManager!!.selectedAccount.id, hdKeyNode)
                            intent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
                            startActivity(intent)
                        }
                    }

                    ResultType.SHARE -> {
                        val share = data.getShare()
                        callMe(activity!!, share, StringHandlerActivity.IMPORT_SSS_CONTENT_CODE)
                    }

                    ResultType.URI -> {
                        // open HandleUrlActivity and let it decide what to do with this URL (check if its a payment request)
                        val uri = data.getUri()
                        startActivity(HandleUrlActivity.getIntent(activity, uri))
                    }

                    ResultType.POP_REQUEST -> {
                        val popRequest = data.getPopRequest()
                        startActivity(
                            Intent(activity, PopActivity::class.java)
                                .putExtra("popRequest", popRequest)
                                .addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
                        )
                    }

                    ResultType.BIT_ID_REQUEST -> {
                        val request = data.getBitIdRequest()
                        BitIDAuthenticationActivity.callMe(activity, request)
                    }

                    else -> {}
                }
            }
        }
    }

    @Subscribe
    fun refreshingExchangeRatesFailed(event: RefreshingExchangeRatesFailed?) {
        _toaster!!.toastConnectionError()
        _exchangeRatePrice = null
    }

    @Subscribe
    fun exchangeRatesRefreshed(event: ExchangeRatesRefreshed?) {
        _exchangeRatePrice =
            _mbwManager!!.currencySwitcher.getExchangeRatePrice(_mbwManager!!.selectedAccount.coinType)
        updateUi()
        updateExchangeSourceMenu()
    }

    @Subscribe
    fun exchangeSourceChanged(event: ExchangeSourceChanged?) {
        _exchangeRatePrice =
            _mbwManager!!.currencySwitcher.getExchangeRatePrice(_mbwManager!!.selectedAccount.coinType)
        updateUi()
    }

    @Subscribe
    fun selectedCurrencyChanged(event: SelectedCurrencyChanged?) {
        _exchangeRatePrice =
            _mbwManager!!.currencySwitcher.getExchangeRatePrice(_mbwManager!!.selectedAccount.coinType)
        updateUi()
        updateExchangeSourceMenu()
    }

    /**
     * The selected Account changed, update UI to reflect other BalanceSatoshis
     */
    @Subscribe
    fun selectedAccountChanged(event: SelectedAccountChanged?) {
        updateUi()
        updateExchangeSourceMenu()
    }

    /**
     * balance has changed, update UI
     */
    @Subscribe
    fun balanceChanged(event: BalanceChanged?) {
        updateUi()
    }

    @Subscribe
    fun accountChanged(event: AccountChanged?) {
        updateUi()
    }

    @Subscribe
    fun syncStopped(event: SyncStopped?) {
        updateUi()
    }

    companion object {
        const val COINMARKETCAP: String = "Coinmarketcap"
        const val GENERIC_SCAN_REQUEST: Int = 4
    }
}
