package com.mycelium.wallet.activity.main.address

import android.app.Application

class AddressFragmentCoinsModel(app: Application) : AddressFragmentViewModel(app) {

    override fun qrClickReaction() {
        if (account.receivingAddress.isPresent) {
            //ReceiveCoinsActivity.callMe(activity, account, account.canSpend())
        }
    }
}