package com.mycelium.wallet.activity.main.address

import android.app.Application
import android.support.v4.app.FragmentActivity
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import com.mycelium.wallet.activity.receive.ReceiveCoinsActivity
import com.mycelium.wallet.coinapult.CoinapultAccount

class AddressFragmentCoinsModel(app: Application) : AddressFragmentViewModel(app) {
    override fun qrClickReaction(activity: FragmentActivity) {
        if (model.account is CoinapultAccount) {
            Utils.showSimpleMessageDialog(activity, R.string.coinapult_gone_details);
        } else if (model.account.receivingAddress.isPresent) {
            ReceiveCoinsActivity.callMe(activity, model.account, model.account.canSpend())
        }
    }
}