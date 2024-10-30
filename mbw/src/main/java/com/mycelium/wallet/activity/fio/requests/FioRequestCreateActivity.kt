package com.mycelium.wallet.activity.fio.requests

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.MenuItem
import android.view.Window
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.GetAmountActivity
import com.mycelium.wallet.activity.fio.requests.viewmodels.FioRequestCreateViewModel
import com.mycelium.wallet.activity.modern.Toaster
import com.mycelium.wallet.activity.receive.ReceiveCoinsActivity
import com.mycelium.wallet.activity.send.ManualAddressEntry
import com.mycelium.wallet.activity.send.SendCoinsActivity
import com.mycelium.wallet.activity.send.event.AmountListener
import com.mycelium.wallet.activity.view.loader
import com.mycelium.wallet.databinding.FioRequestCreateNameBinding
import com.mycelium.wapi.wallet.Address
import com.mycelium.wapi.wallet.coins.Value
import java.util.*
import androidx.activity.viewModels

class FioRequestCreateActivity : AppCompatActivity(), AmountListener {

    private val viewModel: FioRequestCreateViewModel by viewModels()

    companion object {
        const val FIO_ADDRESS_FROM = "FIO_ADDRESS_FROM"
        const val FIO_ADDRESS_TO = "FIO_ADDRESS_TO"
        const val FIO_TOKEN_TO = "FIO_TOKEN_TO"

        @JvmStatic
        fun start(context: Context, amount: Value?, fioAddressFrom: String,
                  fioAdrressTo: String, fioTokenTo: Address?, accountToSelect: UUID) =
                context.startActivity(Intent(context, FioRequestCreateActivity::class.java)
                        .putExtra(SendCoinsActivity.AMOUNT, amount)
                        .putExtra(FIO_ADDRESS_FROM, fioAddressFrom)
                        .putExtra(FIO_ADDRESS_TO, fioAdrressTo)
                        .putExtra(FIO_TOKEN_TO, fioTokenTo)
                        .putExtra(SendCoinsActivity.ACCOUNT, accountToSelect))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val fioAddressFrom = intent.getStringExtra(FIO_ADDRESS_FROM)
        val fioAddressTo = intent.getStringExtra(FIO_ADDRESS_TO)
        val tokenAddressTo = intent.getSerializableExtra(FIO_TOKEN_TO) as Address?
        val mbwManager = MbwManager.getInstance(application)
        val accountId = checkNotNull(intent.getSerializableExtra(SendCoinsActivity.ACCOUNT) as UUID)
        val crashHint = TextUtils.join(", ", intent.extras!!.keySet()) + " (account id was $accountId)"
        val isColdStorage = intent.getBooleanExtra(SendCoinsActivity.IS_COLD_STORAGE, false)
        val account = mbwManager.getWalletManager(isColdStorage).getAccount(accountId)
                ?: throw IllegalStateException(crashHint)

        viewModel.activity = this
        if (!viewModel.isInitialized()) {
            viewModel.init(account, intent)
        }

        if (savedInstanceState != null) {
            viewModel.loadInstance(savedInstanceState)
        }
        viewModel.payeeFioName.value = fioAddressFrom
        viewModel.payerFioName.value = fioAddressTo
        viewModel.payerTokenPublicAddress.value = tokenAddressTo.toString()

        requestWindowFeature(Window.FEATURE_NO_TITLE)
        supportActionBar?.run {
            title = getString(R.string.fio_create_request_currency_title, viewModel.payeeAccount.value?.coinType?.symbol)
            setHomeAsUpIndicator(R.drawable.ic_back_arrow)
            setHomeButtonEnabled(true)
            setDisplayHomeAsUpEnabled(true)
        }

        DataBindingUtil.setContentView<FioRequestCreateNameBinding>(this,
                R.layout.fio_request_create_name)
                .also {
                    it.viewModel = viewModel
                    it.activity = this
                }.apply {
                    lifecycleOwner = this@FioRequestCreateActivity
                    with(this) {
                        btNextButton.setOnClickListener {
                            //show loader
                            loader(true, getString(R.string.sending, "..."))
                            viewModel?.sendRequest(this@FioRequestCreateActivity, {
                                //hide loader
                                loader(false)
                            }, {
                                //error
                                loader(false)
                                Toaster(this@FioRequestCreateActivity).toast("Something went wrong", true)
                            })
                        }
                        tvPayeeFio.setOnClickListener {
                            showPayeeSelector()
                        }
                        tvPayerFioAddress.setOnClickListener {
                            val intent = Intent(this@FioRequestCreateActivity, ManualAddressEntry::class.java)
                                    .putExtra(ManualAddressEntry.FOR_FIO_REQUEST, true)
                            this@FioRequestCreateActivity.startActivityForResult(intent, ReceiveCoinsActivity.MANUAL_ENTRY_RESULT_CODE)
                        }
                        memo.setOnFocusChangeListener { view, b ->
                            if(b) {
                                scroll.postDelayed({ scroll.smoothScrollBy(0, scroll.maxScrollAmount) }, 500)
                            }
                        }
                    }
                }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        viewModel.processReceivedResults(requestCode, resultCode, data, this)
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        viewModel.saveInstance(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onClickAmount() {
        val account = viewModel.getAccount()
        GetAmountActivity.callMeToReceive(this, viewModel.getAmount().value,
                SendCoinsActivity.GET_AMOUNT_RESULT_CODE, account.coinType)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
            when (item.itemId) {
                android.R.id.home -> {
                    onBackPressed()
                    true
                }
                else -> super.onOptionsItemSelected(item)
            }

    fun showPayeeSelector() {
        val payeeFioAddreses = viewModel.getPayeeFioAddreses()
        val payeeFioAddresesStr = payeeFioAddreses?.map { it.name }?.toTypedArray()
        val checkedPosition = payeeFioAddresesStr?.indexOfFirst { it == viewModel.payeeFioName.value }
        AlertDialog.Builder(this)
                .setSingleChoiceItems(payeeFioAddresesStr, checkedPosition ?: 0, null)
                .setPositiveButton(R.string.button_ok) { dialog, _ ->
                    dialog.dismiss()
                    val selectedPosition: Int = (dialog as AlertDialog).listView.checkedItemPosition
                    val payeeAddress = payeeFioAddreses?.get(selectedPosition)
                    viewModel.payeeFioName.value = payeeAddress?.name
                }
                .show()
    }
}