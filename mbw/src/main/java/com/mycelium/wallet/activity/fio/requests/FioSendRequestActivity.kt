package com.mycelium.wallet.activity.fio.requests

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.fio.requests.viewmodels.FioSendRequestViewModel
import com.mycelium.wallet.databinding.FioSendRequestActivityBinding

class FioSendRequestActivity : AppCompatActivity() {

    private lateinit var viewModel: FioSendRequestViewModel
    private lateinit var mbwManager: MbwManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        DataBindingUtil.setContentView<FioSendRequestActivityBinding>(this,
                R.layout.fio_send_request_activity)
                .also {
                    it.viewModel = viewModel
                    it.activity = this
                }.apply {
                    with(this) {

                    }
                }
    }

    fun onClickDecline() {
        viewModel.decline()
    }
    fun onClickSend(){
        viewModel.pay()
    }
}