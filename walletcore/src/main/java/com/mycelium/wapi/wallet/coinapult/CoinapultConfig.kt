package com.mycelium.wapi.wallet.coinapult

import com.mycelium.wapi.wallet.manager.Config


class CoinapultConfig(val currency: Currency) : Config {
    override fun getType(): String = "coinapult"
}