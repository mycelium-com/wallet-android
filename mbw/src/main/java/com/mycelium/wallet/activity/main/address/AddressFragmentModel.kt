package com.mycelium.wallet.activity.main.address

import android.app.Application
import android.os.Bundle
import android.view.View
import butterknife.BindView
import com.google.common.base.Optional
import com.mrd.bitlib.model.Address
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.activity.receive.ReceiveCoinsModel
import com.mycelium.wallet.activity.util.QrImageView
import com.mycelium.wapi.wallet.currency.CurrencyValue
import com.squareup.otto.Bus

class AddressFragmentModel(
        val context: Application
) {
    private var mbwManager: MbwManager = MbwManager.getInstance(context)
    private var showBip44Path: Boolean = false

    init {
        showBip44Path = mbwManager.getMetadataStorage().getShowBip44Path()
    }
    fun getAddress(): Optional<Address> {
        return mbwManager.getSelectedAccount().getReceivingAddress()
    }
}