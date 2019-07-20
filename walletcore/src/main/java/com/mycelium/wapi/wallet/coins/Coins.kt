package com.mycelium.wapi.wallet.coins

import com.mycelium.wapi.wallet.bch.coins.BchMain
import com.mycelium.wapi.wallet.bch.coins.BchTest
import com.mycelium.wapi.wallet.btc.coins.BitcoinMain
import com.mycelium.wapi.wallet.btc.coins.BitcoinTest
import com.mycelium.wapi.wallet.colu.coins.*
import com.mycelium.wapi.wallet.eth.coins.EthMain
import com.mycelium.wapi.wallet.eth.coins.EthTest

public val COINS = mapOf<String, CryptoCurrency>(
        BitcoinMain.get().id to BitcoinMain.get(),
        BitcoinTest.get().id to BitcoinTest.get(),
        BchMain.id to BchMain,
        BchTest.id to BchTest,
        EthMain.id to EthMain,
        EthTest.id to EthTest,
        MASSCoin.id to MASSCoin,
        MASSCoinTest.id to MASSCoinTest,
        MTCoin.id to MTCoin,
        MTCoinTest.id to MTCoinTest,
        RMCCoin.id to RMCCoin,
        RMCCoinTest.id to RMCCoinTest
)