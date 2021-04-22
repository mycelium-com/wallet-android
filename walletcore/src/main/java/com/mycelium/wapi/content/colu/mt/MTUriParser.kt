package com.mycelium.wapi.content.colu.mt

import com.mrd.bitlib.model.NetworkParameters
import com.mycelium.wapi.content.CryptoUriParser
import com.mycelium.wapi.wallet.colu.coins.MTCoin
import com.mycelium.wapi.wallet.colu.coins.MTCoinTest

class MTUriParser(override val network: NetworkParameters)
    : CryptoUriParser(network, "mt", MTCoin, MTCoinTest)
