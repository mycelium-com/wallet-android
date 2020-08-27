package com.mycelium.wallet.activity.receive

import android.app.Application
import com.mycelium.wallet.R


class ReceiveFIOViewModel(application: Application) : ReceiveGenericCoinsViewModel(application) {
    override fun getTitle(): String = context.getString(R.string.receive_title_fio)
}