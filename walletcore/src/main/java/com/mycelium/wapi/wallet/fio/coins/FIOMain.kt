package com.mycelium.wapi.wallet.fio.coins

object FIOMain : FIOToken() {
    override val url = "http://fioprotocol.io/v1/"

    init {
        id = "fio.main"
        name = "FIO"
    }
}