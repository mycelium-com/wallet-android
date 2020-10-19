package com.mycelium.wapi.wallet.fio.coins

object FIOMain : FIOToken() {
    override val url = "https://fio.greymass.com/v1/"

    init {
        id = "fio.main"
        name = "FIO"
    }
}