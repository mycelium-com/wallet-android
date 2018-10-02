package com.mycelium.wallet.activity.main.address

import android.app.Application
import android.support.v4.app.FragmentActivity
import android.support.v7.app.AppCompatActivity
import com.mycelium.wallet.activity.receive.ReceiveCoinsActivity

class AddressFragmentCoinsModel(app: Application) : AddressFragmentViewModel(app) {

    override fun qrClickReaction(activity: FragmentActivity) {
        if (model.account.receivingAddress.isPresent) {
            ReceiveCoinsActivity.callMe(activity, model.account, model.account.canSpend())
        }
    }
}