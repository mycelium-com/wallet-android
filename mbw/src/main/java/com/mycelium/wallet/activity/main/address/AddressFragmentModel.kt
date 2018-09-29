package com.mycelium.wallet.activity.main.address

import android.app.Application
import android.os.Bundle
import android.view.View
import android.widget.Toast
import butterknife.BindView
import com.google.common.base.Optional
import com.mrd.bitlib.model.Address
import com.mrd.bitlib.util.CoinUtil
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.receive.ReceiveCoinsModel
import com.mycelium.wallet.activity.util.AccountDisplayType
import com.mycelium.wallet.activity.util.QrImageView
import com.mycelium.wapi.wallet.currency.CurrencyValue
import com.mycelium.wapi.wallet.currency.ExchangeBasedCurrencyValue
import com.squareup.otto.Bus

class AddressFragmentModel(
        val context: Application
) {
    private var mbwManager: MbwManager = MbwManager.getInstance(context)
    private var showBip44Path: Boolean = false

    init {
        showBip44Path = mbwManager.getMetadataStorage().getShowBip44Path()
    }

    fun getShowBip44Path(): Boolean {
        return showBip44Path
    }
}