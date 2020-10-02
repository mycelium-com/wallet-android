package com.mycelium.wallet.activity.fio.requests

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.fio.requests.viewmodels.FioRequestBtcViewModel
import com.mycelium.wallet.activity.fio.requests.viewmodels.FioSendRequestViewModel
import com.mycelium.wallet.databinding.FioSendRequestActivityBinding
import com.mycelium.wallet.databinding.FragmentFioRequestBtcNameBinding
import com.mycelium.wallet.databinding.FragmentFioRequestBtcNameBindingImpl

class FioRequestBtcActivity : AppCompatActivity() {

    private lateinit var viewModel: FioRequestBtcViewModel
    private lateinit var mbwManager: MbwManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        DataBindingUtil.setContentView<FragmentFioRequestBtcNameBinding>(this,
                R.layout.fragment_fio_request_btc_name)
                .also {
                    it.viewModel = viewModel
                }.apply {
                    with(this) {

                    }
                }
    }
}