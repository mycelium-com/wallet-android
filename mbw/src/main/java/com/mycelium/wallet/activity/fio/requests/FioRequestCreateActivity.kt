package com.mycelium.wallet.activity.fio.requests

import android.content.Context
import android.content.Intent
import android.os.Bundle
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
        const val ADDRESS_TO = "ADDRESS_TO"
        const val AMOUNT = "AMOUNT"

        @JvmStatic
        fun start(context: Context, amount: Value?, addressTo: String) {
            val starter = Intent(context, FioRequestCreateActivity::class.java)
                    .putExtra(AMOUNT, amount)
                    .putExtra(ADDRESS_TO, addressTo)
            context.startActivity(starter)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProviders.of(this).get(FioRequestBtcViewModel::class.java)

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
}