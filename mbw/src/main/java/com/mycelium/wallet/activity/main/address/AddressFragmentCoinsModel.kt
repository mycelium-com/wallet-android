package com.mycelium.wallet.activity.main.address

import android.app.Application
import android.support.v4.app.FragmentActivity
import com.mycelium.wallet.activity.receive.ReceiveCoinsActivity
import com.mycelium.wallet.coinapult.CoinapultAccount

class AddressFragmentCoinsModel(app: Application) : AddressFragmentViewModel(app) {
    override fun qrClickReaction(activity: FragmentActivity) {
        if (model.account.receivingAddress.isPresent &&
                model.account !is CoinapultAccount) {
            ReceiveCoinsActivity.callMe(activity, model.account, model.account.canSpend())
        }
    }
}