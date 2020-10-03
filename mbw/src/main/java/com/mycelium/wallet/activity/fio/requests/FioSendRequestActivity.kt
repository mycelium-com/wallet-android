package com.mycelium.wallet.activity.fio.requests

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProviders
import com.google.gson.Gson
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.fio.requests.viewmodels.FioSendRequestViewModel
import com.mycelium.wallet.databinding.FioSendRequestActivityBinding
import fiofoundation.io.fiosdk.models.fionetworkprovider.FIORequestContent

class FioSendRequestActivity : AppCompatActivity() {

    private lateinit var viewModel: FioSendRequestViewModel
    private lateinit var mbwManager: MbwManager

    companion object{
        val CONTENT = "CONTENT"
        fun start(activity:Activity, item: FIORequestContent) {
            with(Intent(activity,FioSendRequestActivity::class.java)) {
                putExtra(CONTENT,item.toJson())
                activity.startActivity(this)
            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel= ViewModelProviders.of(this).get(FioSendRequestViewModel::class.java)
        val fioRequestContent = Gson().fromJson(intent.getStringExtra(CONTENT), FIORequestContent::class.java)

        setContentView(R.layout.fio_send_request_activity)
//        DataBindingUtil.setContentView<FioSendRequestActivityBinding>(this,
//                R.layout.fio_send_request_activity)
//                .also {
//                    it.viewModel = viewModel
//                    it.activity = this
//                }.apply {
//                    with(this) {
//
//                    }
//                }

        findViewById<Button>(R.id.btSend).setOnClickListener {
            onClickSend()
        }
    }

    fun onClickDecline() {
        viewModel.decline()
    }
    fun onClickSend(){
        viewModel.pay()
        FioSendStatusActivity.start(this)
    }
}