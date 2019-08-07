package com.mycelium.wapi.wallet.manager

import com.mycelium.wapi.wallet.genericdb.FeeEstimationsBacking
import com.mycelium.wapi.wallet.providers.EthFeeProvider
import com.mycelium.wapi.wallet.providers.FeeProvider

class FeeEstimations(estimationsBacking: FeeEstimationsBacking) {
    val feeProviders = ArrayList<FeeProvider>(2)
    init {
        feeProviders.add(EthFeeProvider(estimationsBacking))
    }

    // todo recurrently and on start check for estimation updates for all the currencies
    // todo elegant architectural decision depending on testnet/not needed, probably add some logic into coins
}