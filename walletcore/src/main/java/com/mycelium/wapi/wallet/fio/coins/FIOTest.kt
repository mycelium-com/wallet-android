package com.mycelium.wapi.wallet.fio.coins

object FIOTest : FIOToken("fio.test", "FIO test") {
    override val url = "http://testnet.fioprotocol.io/v1/"
}