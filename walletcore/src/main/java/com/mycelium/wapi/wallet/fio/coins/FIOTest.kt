package com.mycelium.wapi.wallet.fio.coins

object FIOTest : FIOToken() {
    override val url = "http://testnet.fioprotocol.io/v1/"

    init {
        id = "fio.test"
        name = "FIO test"
    }
}