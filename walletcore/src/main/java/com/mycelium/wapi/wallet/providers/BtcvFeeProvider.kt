package com.mycelium.wapi.wallet.providers

import com.mycelium.wapi.api.Wapi
import com.mycelium.wapi.wallet.FeeEstimationsGeneric
import com.mycelium.wapi.wallet.btcvault.coins.BitcoinVaultMain
import com.mycelium.wapi.wallet.btcvault.coins.BitcoinVaultTest
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.genericdb.FeeEstimationsBacking

class BtcvFeeProvider(testnet: Boolean, wapi: Wapi, feeBacking: FeeEstimationsBacking) :
        WapiFeeProvider(wapi, feeBacking) {
    override val coinType = if (testnet) {
        BitcoinVaultTest
    } else {
        BitcoinVaultMain
    }

    override var estimation = feeBacking.getEstimationForCurrency(coinType)
            ?: FeeEstimationsGeneric(Value.valueOf(coinType, 1000),
                    Value.valueOf(coinType, 3000),
                    Value.valueOf(coinType, 6000),
                    Value.valueOf(coinType, 8000),
                    0)
}