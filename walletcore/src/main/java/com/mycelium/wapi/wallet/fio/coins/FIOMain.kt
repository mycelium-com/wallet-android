package com.mycelium.wapi.wallet.fio.coins

import com.mycelium.wapi.wallet.fio.FIOToken

object FIOMain : FIOToken() {
    const val url = "http://fioprotocol.io/v1/"

    init {
        id = "fio.main"
        name = "Fio"
    }
}