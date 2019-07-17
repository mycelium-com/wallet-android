package com.mycelium.wallet.activity.main.address

import android.app.Application
import androidx.fragment.app.FragmentActivity
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import com.mycelium.wallet.activity.receive.ReceiveCoinsActivity
import com.mycelium.wapi.wallet.coinapult.CoinapultAccount

class AddressFragmentCoinsModel(app: Application) : AddressFragmentViewModel(app) {
    override fun qrClickReaction(activity: FragmentActivity) {
        if (model.account is CoinapultAccount) {
            Utils.showSimpleMessageDialog(activity, R.string.coinapult_gone_details);
        } else if (model.account.receiveAddress != null) {
            ReceiveCoinsActivity.callMe(activity, model.account, model.account.canSpend())
        }
    }
}
