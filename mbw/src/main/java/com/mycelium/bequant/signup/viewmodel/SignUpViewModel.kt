package com.mycelium.bequant.signup.viewmodel

import android.app.Application
import android.view.View
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.mycelium.wallet.BuildConfig
import com.mycelium.wallet.R


class SignUpViewModel(application: Application) : AndroidViewModel(application) {

    val buildVersion = MutableLiveData(application.getString(R.string.build_version, BuildConfig.VERSION_NAME))
    val email = MutableLiveData<String>()
    val password = MutableLiveData<String>()
    val passwordNoteVisibility = MutableLiveData(View.VISIBLE)
    val repeatPassword = MutableLiveData<String>()
    val referralCode = MutableLiveData<String>()
}