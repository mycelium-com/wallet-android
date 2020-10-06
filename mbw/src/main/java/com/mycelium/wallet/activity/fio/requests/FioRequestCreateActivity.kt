package com.mycelium.wallet.activity.fio.requests

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Window
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProviders
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.fio.requests.viewmodels.FioRequestBtcViewModel
import com.mycelium.wallet.databinding.FioRequestCreateNameBinding
import com.mycelium.wapi.wallet.coins.Value

class FioRequestCreateActivity : AppCompatActivity() {

    private lateinit var viewModel: FioRequestBtcViewModel

    companion object {
        const val FIO_ADDRESS_TO = "FIO_ADDRESS_TO"
        const val FIO_TOKEN_TO = "FIO_TOKEN_TO"
        const val AMOUNT = "AMOUNT"

        @JvmStatic
        fun start(context: Context, amount: Value?, fioAdrressTo: String, fioTokenTo: String) {
            val starter = Intent(context, FioRequestCreateActivity::class.java)
                    .putExtra(AMOUNT, amount)
                    .putExtra(FIO_ADDRESS_TO, fioAdrressTo)
                    .putExtra(FIO_TOKEN_TO, fioTokenTo)
            context.startActivity(starter)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(FioRequestBtcViewModel::class.java)

        val amount = intent.getSerializableExtra(AMOUNT) as Value
        val fioAddressTo = intent.getStringExtra(FIO_ADDRESS_TO)
        val tokenAddressTo = intent.getStringExtra(FIO_TOKEN_TO)

        viewModel.payerFioAddress.value = fioAddressTo
        viewModel.payerTokenPublicAddress.value = tokenAddressTo
        viewModel.amount.value = amount

        requestWindowFeature(Window.FEATURE_NO_TITLE)
        supportActionBar?.run {
            title = getString(R.string.fio_create_request_currency_title, amount.currencySymbol.toUpperCase())
            setHomeAsUpIndicator(R.drawable.ic_back_arrow)
            setHomeButtonEnabled(true)
            setDisplayHomeAsUpEnabled(true)
        }

        DataBindingUtil.setContentView<FioRequestCreateNameBinding>(this,
                R.layout.fio_request_create_name)
                .also {
                    it.viewModel = viewModel
                }.apply {
                    with(this) {
                        btNextButton.setOnClickListener {
                            viewModel?.sendRequest(this@FioRequestCreateActivity)
                        }
                    }
                }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        viewModel.processReceivedResults(requestCode, resultCode, data, this)
        super.onActivityResult(requestCode, resultCode, data)
    }

}