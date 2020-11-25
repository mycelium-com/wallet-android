package com.mycelium.wapi.wallet.coins

import com.mycelium.wapi.wallet.bch.coins.BchMain
import com.mycelium.wapi.wallet.bch.coins.BchTest
import com.mycelium.wapi.wallet.btc.coins.BitcoinMain
import com.mycelium.wapi.wallet.btc.coins.BitcoinTest
import com.mycelium.wapi.wallet.colu.coins.*
import com.mycelium.wapi.wallet.eth.coins.EthMain
import com.mycelium.wapi.wallet.eth.coins.EthTest
import com.mycelium.wapi.wallet.fio.coins.FIOMain
import com.mycelium.wapi.wallet.fio.coins.FIOTest
import java.util.*

val COINS_SET = setOf<CryptoCurrency>(
        BitcoinMain.get(), BitcoinTest.get(),
        BchMain, BchTest,
        EthMain, EthTest,
        FIOMain, FIOTest,
        MASSCoin, MASSCoinTest,
        MTCoin, MTCoinTest,
        RMCCoin, RMCCoinTest
)

val COINS = COINS_SET.map { it.id to it }.toMap()

val SYMBOL_COIN_MAP = COINS_SET.map { it.symbol.toUpperCase(Locale.US) to it }.toMap()