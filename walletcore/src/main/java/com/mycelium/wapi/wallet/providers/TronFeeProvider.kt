package com.mycelium.wapi.wallet.providers

import com.mycelium.wapi.wallet.FeeEstimationsGeneric
import com.mycelium.wapi.wallet.coins.AssetInfo
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.tron.coins.TronMain
import com.mycelium.wapi.wallet.tron.coins.TronTest

/**
 * TRON has no fee market: a transfer costs a fixed amount of bandwidth (free quota or
 * 1000 sun/byte). The generic fee levels are therefore constant and only serve the parts of
 * the app that expect a fee estimation for every coin; the send flow uses
 * [com.mycelium.wapi.wallet.tron.TronFeeEstimator] instead.
 */
class TronFeeProvider(isTestnet: Boolean) : FeeProvider {
    override val coinType: AssetInfo = if (isTestnet) TronTest else TronMain

    override suspend fun updateFeeEstimationsAsync() {}

    // 268 bandwidth * 1000 sun = 0.268 TRX, the worst-case cost of a plain TRX transfer
    override var estimation: FeeEstimationsGeneric = FeeEstimationsGeneric(
        Value.valueOf(coinType, 268_000),
        Value.valueOf(coinType, 268_000),
        Value.valueOf(coinType, 268_000),
        Value.valueOf(coinType, 268_000),
        0)
}
