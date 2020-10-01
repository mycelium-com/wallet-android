package com.mycelium.wallet.activity.fio.requests.viewmodels

import android.app.Activity
import android.app.Application
import android.view.View
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mycelium.wallet.activity.send.model.SendCoinsViewModel
import com.mycelium.wallet.activity.util.FeeFormatter
import java.util.regex.Pattern

open class FioRequestBtcViewModel() : ViewModel() {


    val fioName = MutableLiveData<String>()

}