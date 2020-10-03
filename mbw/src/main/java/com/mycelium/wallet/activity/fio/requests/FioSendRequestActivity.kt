package com.mycelium.wallet.activity.fio.requests

import android.app.Activity
import android.content.Intent
import android.graphics.Point
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.google.gson.Gson
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.fio.requests.viewmodels.FioSendRequestViewModel
import com.mycelium.wallet.activity.send.adapter.FeeLvlViewAdapter
import com.mycelium.wallet.activity.send.adapter.FeeViewAdapter
import com.mycelium.wallet.activity.send.model.SendBtcViewModel
import com.mycelium.wallet.activity.send.model.SendCoinsViewModel
import com.mycelium.wallet.activity.send.model.SendEthViewModel
import com.mycelium.wallet.activity.send.model.SendFioViewModel
import com.mycelium.wallet.databinding.FioSendRequestActivityBinding
import com.mycelium.wapi.wallet.btc.bip44.HDAccount
import com.mycelium.wapi.wallet.btc.bip44.getBTCBip44Accounts
import com.mycelium.wapi.wallet.btc.single.SingleAddressAccount
import com.mycelium.wapi.wallet.coins.COINS
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.erc20.ERC20Account
import com.mycelium.wapi.wallet.eth.EthAccount
import com.mycelium.wapi.wallet.fio.FioAccount
import com.mycelium.wapi.wallet.fio.FioModule
import fiofoundation.io.fiosdk.models.fionetworkprovider.FIORequestContent
import kotlinx.android.synthetic.main.fio_send_request_activity.*
import kotlinx.android.synthetic.main.send_coins_activity.root
import kotlinx.android.synthetic.main.send_coins_fee_selector.*
import java.math.BigDecimal
import java.math.BigInteger

class FioSendRequestActivity : AppCompatActivity() {

    private lateinit var fioRequestViewModel: FioSendRequestViewModel
    private lateinit var sendViewModel: SendCoinsViewModel
    private lateinit var mbwManager: MbwManager

    companion object {
        const val CONTENT = "CONTENT"
        fun start(activity: Activity, item: FIORequestContent) {
            with(Intent(activity, FioSendRequestActivity::class.java)) {
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

        fioRequestViewModel.memoFrom.value = fioRequestContent.deserializedContent!!.memo ?: ""
        fioRequestViewModel.from.value = fioRequestContent.payeeFioAddress
        fioRequestViewModel.satisfyRequestFrom.value = fioRequestContent.payerFioAddress
        val requestedCurrency = COINS.values.firstOrNull { it.symbol.toUpperCase() == fioRequestContent.deserializedContent!!.chainCode }
                ?: throw IllegalStateException("Unexpected currency ${fioRequestContent.deserializedContent!!.chainCode}")

        fioRequestViewModel.amount.value = Value.valueOf(requestedCurrency, strToBigInteger(requestedCurrency,
                fioRequestContent.deserializedContent!!.amount))

        val walletManager = MbwManager.getInstance(this).getWalletManager(false)
        val fioModule = walletManager.getModuleById(FioModule.ID) as FioModule

        // TODO uncomment and use getConnectedAccounts() when mapping saving is implemented, remove call to server
//        val mappedAccounts = fioModule.getConnectedAccounts(fioRequestViewModel.satisfyRequestFrom.value!!)
//        val account = mappedAccounts.firstOrNull { it.coinType.id == requestedCurrency.id }
//                ?: throw IllegalStateException("No mapped address of type ${fioRequestContent.deserializedContent!!.chainCode}")

        // taking first btc account to check that the whole thing works
        val account = walletManager.getBTCBip44Accounts()[0]

        sendViewModel = when (account) {
            is SingleAddressAccount, is HDAccount -> viewModelProvider.get(SendBtcViewModel::class.java)
            is EthAccount, is ERC20Account -> viewModelProvider.get(SendEthViewModel::class.java)
            is FioAccount -> viewModelProvider.get(SendFioViewModel::class.java)
            else -> throw NotImplementedError()
        }
        if (!sendViewModel.isInitialized()) {
            sendViewModel.init(account, intent)
        }
        DataBindingUtil.setContentView<FioSendRequestActivityBinding>(this, R.layout.fio_send_request_activity)
                .also {
                    it.fioRequestViewModel = fioRequestViewModel
                    it.sendViewModel = sendViewModel
                    it.activity = this
                    it.lifecycleOwner = this
                }.apply {
                    with(this) {

                    }
                }

        initFeeView()
        initFeeLvlView()
        findViewById<Button>(R.id.btSend).setOnClickListener {
            onClickSend()
        }
        tvSatisfyFromAccount.text = "${account.label} - ${account.accountBalance.spendable}"

        supportActionBar?.run {
            setHomeAsUpIndicator(R.drawable.ic_back_arrow)
            setDisplayHomeAsUpEnabled(true)
            title = "Send ${fioRequestContent.deserializedContent!!.chainCode}"
        }
    }

    private fun strToBigInteger(coinType: CryptoCurrency, amountStr: String): BigInteger =
            BigDecimal(amountStr).movePointRight(coinType.unitExponent).toBigIntegerExact()

    fun onClickDecline() {
        fioRequestViewModel.decline()
    }

    fun onClickSend() {
        fioRequestViewModel.pay()
        FioSendStatusActivity.start(this)
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

    override fun onOptionsItemSelected(item: MenuItem?): Boolean =
            when (item?.itemId) {
                android.R.id.home -> {
                    onBackPressed()
                    true
                }
                else -> super.onOptionsItemSelected(item)
            }
}