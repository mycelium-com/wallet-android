package com.mycelium.wallet.activity.main.address

import android.app.Application
import android.support.v7.app.AppCompatActivity
import com.mycelium.wallet.activity.receive.ReceiveCoinsActivity

class AddressFragmentCoinsModel(app: Application) : AddressFragmentViewModel(app) {

    override fun qrClickReaction(activity: AppCompatActivity) {
        if (account.receivingAddress.isPresent) {
            ReceiveCoinsActivity.callMe(activity, account, account.canSpend())
        }
    }
}