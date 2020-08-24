package com.mycelium.wapi.wallet.fio.coins

import com.mycelium.wapi.wallet.fio.FIOToken

object FIOTest : FIOToken() {
    const val url = "http://testnet.fioprotocol.io/v1/"

    init {
        id = "fio.test"
        name = "Fio Test"
    }
}