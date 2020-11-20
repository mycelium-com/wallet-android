package com.mycelium.bequant.signup.viewmodel

import android.app.Application
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.mycelium.wallet.BuildConfig
import com.mycelium.wallet.R
import com.nulabinc.zxcvbn.Zxcvbn


class SignUpViewModel(application: Application) : AndroidViewModel(application) {

    val buildVersion = MutableLiveData(application.getString(R.string.build_version, BuildConfig.VERSION_NAME))
    val email = MutableLiveData<String>()
    val password = MutableLiveData<String>()
    val passwordNoteVisibility = MutableLiveData(View.VISIBLE)
    val passwordLevelVisibility = MutableLiveData(View.GONE)
    val passwordLevelText = MutableLiveData<String>()
    val repeatPassword = MutableLiveData<String>()
    val country = MutableLiveData<String>()
    val referralCode = MutableLiveData<String>()

    fun calculatePasswordLevel(password: String, passwordLevel: ProgressBar, passwordLevelLabel: TextView) {
        val strength = Zxcvbn().measure(password)
        passwordNoteVisibility.value = if (password.isNotEmpty()) View.GONE else View.VISIBLE
        passwordLevelVisibility.value = if (password.isNotEmpty()) View.VISIBLE else View.GONE
        passwordLevel.progress = strength.score * 30
        val resources = passwordLevel.resources
        passwordLevel.progressDrawable = resources.getDrawable(
                when (strength.score) {
                    0 -> R.drawable.bequant_password_red_line
                    1 -> R.drawable.bequant_password_red_line
                    2 -> R.drawable.bequant_password_yellow_line
                    else -> R.drawable.bequant_password_green_line
                })
        passwordLevelText.value = when (strength.score) {
            0 -> resources.getString(R.string.bequant_password_weak)
            1 -> resources.getString(R.string.bequant_password_fair)
            2 -> resources.getString(R.string.bequant_password_good)
            3 -> resources.getString(R.string.bequant_password_strong)
            else -> resources.getString(R.string.bequant_password_very_strong)
        }
        passwordLevelLabel.setTextColor(resources.getColor(when (strength.score) {
            0 -> R.color.bequant_red
            1 -> R.color.bequant_red
            2 -> R.color.bequant_password_yellow
            else -> R.color.bequant_password_green
        }))
    }
}