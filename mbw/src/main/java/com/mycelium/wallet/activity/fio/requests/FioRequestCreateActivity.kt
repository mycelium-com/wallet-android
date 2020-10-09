package com.mycelium.wallet.activity.fio.requests

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Window
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProviders
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.GetAmountActivity
import com.mycelium.wallet.activity.fio.requests.viewmodels.FioRequestCreateViewModel
import com.mycelium.wallet.activity.receive.ReceiveCoinsActivity
import com.mycelium.wallet.activity.send.ManualAddressEntry
import com.mycelium.wallet.activity.send.SendCoinsActivity
import com.mycelium.wallet.databinding.FioRequestCreateNameBinding
import com.mycelium.wapi.wallet.Address
import com.mycelium.wapi.wallet.coins.Value


class FioRequestCreateActivity : AppCompatActivity() {

    private lateinit var viewModel: FioRequestCreateViewModel

    companion object {
        const val FIO_ADDRESS_TO = "FIO_ADDRESS_TO"
        const val FIO_TOKEN_TO = "FIO_TOKEN_TO"

        //same as in SendCoinsActivity
        const val AMOUNT = "amount"

        @JvmStatic
        fun start(context: Context, amount: Value?, fioAdrressTo: String, fioTokenTo: Address?) {
            val starter = Intent(context, FioRequestCreateActivity::class.java)
                    .putExtra(AMOUNT, amount)
                    .putExtra(FIO_ADDRESS_TO, fioAdrressTo)
                    .putExtra(FIO_TOKEN_TO, fioTokenTo)
            context.startActivity(starter)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(FioRequestCreateViewModel::class.java)
        val account = MbwManager.getInstance(this).selectedAccount

        val fioAddressTo = intent.getStringExtra(FIO_ADDRESS_TO)
        val tokenAddressTo = intent.getSerializableExtra(FIO_TOKEN_TO) as Address?
        if (!viewModel.isInitialized()) {
            viewModel.init(account, intent)
        }

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
                }.apply {
                    lifecycleOwner = this@FioRequestCreateActivity
                    with(this) {
                        btNextButton.setOnClickListener {
                            //show loader
                            viewModel?.sendRequest(this@FioRequestCreateActivity,{
                                //hide loader
                            },{
                                //error
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
                        btEnterAmount.setOnClickListener {
                            onClickAmount()
                        }
                    }
                }


    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        viewModel.processReceivedResults(requestCode, resultCode, data, this)
        super.onActivityResult(requestCode, resultCode, data)
    }

    fun onClickAmount() {
        val account = viewModel.getAccount()
        GetAmountActivity.callMeToSend(this, SendCoinsActivity.GET_AMOUNT_RESULT_CODE, account.id,
                viewModel.getAmount().value, viewModel.getSelectedFee().value,
                viewModel.isColdStorage(), viewModel.getReceivingAddress().value)
    }

    fun showPayeeSelector() {
        val payeeFioAddreses = viewModel.getPayeeFioAddreses()
        val payeeFioAddresesStr = payeeFioAddreses?.map { it.name }?.toTypedArray()
        AlertDialog.Builder(this)
                .setSingleChoiceItems(payeeFioAddresesStr, 0, null)
                .setPositiveButton(R.string.button_ok) { dialog, whichButton ->
                    dialog.dismiss()
                    val selectedPosition: Int = (dialog as AlertDialog).getListView().getCheckedItemPosition()
                    val payeeAddress = payeeFioAddreses?.get(selectedPosition)
                    viewModel.payeeFioName.value = payeeAddress?.name
                }
                .show()
    }
}