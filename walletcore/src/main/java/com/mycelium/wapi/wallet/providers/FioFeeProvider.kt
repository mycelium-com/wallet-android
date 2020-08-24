package com.mycelium.wapi.wallet.providers

import com.mycelium.wapi.wallet.FeeEstimationsGeneric
import com.mycelium.wapi.wallet.coins.AssetInfo
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.fio.coins.FIOMain

class FioFeeProvider : FeeProvider {
    override suspend fun updateFeeEstimationsAsync() {
    }

    override val coinType: AssetInfo = FIOMain

    override var estimation: FeeEstimationsGeneric = FeeEstimationsGeneric(Value.valueOf(coinType, 1000000000),
            Value.valueOf(coinType, 33000000000),
            Value.valueOf(coinType, 67000000000),
            Value.valueOf(coinType, 100000000000),
            0)
}