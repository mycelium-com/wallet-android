package com.mycelium.wallet.activity.fio.requests

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProviders
import com.google.gson.Gson
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.fio.requests.viewmodels.FioSendRequestViewModel
import com.mycelium.wallet.databinding.FioSendRequestActivityBinding
import fiofoundation.io.fiosdk.models.fionetworkprovider.FIORequestContent

class FioSendStatusActivity : AppCompatActivity() {

//    private lateinit var viewModel: FioSendRequestViewModel
    private lateinit var mbwManager: MbwManager

    companion object{
        val CONTENT = "CONTENT"
        fun start(activity:Activity) {
            with(Intent(activity,FioSendStatusActivity::class.java)) {
                activity.startActivity(this)
            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        viewModel= ViewModelProviders.of(this).get(FioSendRequestViewModel::class.java)
        val fioRequestContent = Gson().fromJson(intent.getStringExtra(CONTENT), FIORequestContent::class.java)

        setContentView(R.layout.fio_send_request_status_activity)
//        DataBindingUtil.setContentView<FioSendRequestActivityBinding>(this,
//                R.layout.fio_send_request_status_activity)
//                .also {
//                    it.viewModel = viewModel
//                    it.activity = this
//                }.apply {
//                    with(this) {
//
//                    }
//                }
    }
}