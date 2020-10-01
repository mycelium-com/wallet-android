package com.mycelium.wallet.activity.fio.requests.viewmodels

import android.app.Activity
import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mycelium.wallet.activity.send.model.SendCoinsViewModel
import com.mycelium.wallet.activity.util.FeeFormatter
import java.util.regex.Pattern

open class FioSendRequestViewModel(context: Application) {
//    override val uriPattern = Pattern.compile("[a-zA-Z0-9]+")!!
//    override fun sendTransaction(activity: Activity) {
//        TODO("Not yet implemented")
//    }
//
//    override fun getFeeFormatter(): FeeFormatter {
//        TODO("Not yet implemented")
//    }


    val sendTo = MutableLiveData<String>()
    val payingFrom = MutableLiveData<String>()

}