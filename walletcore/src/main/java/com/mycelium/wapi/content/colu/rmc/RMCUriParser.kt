package com.mycelium.wapi.content.colu.rmc

import com.mrd.bitlib.model.NetworkParameters
import com.mycelium.wapi.content.CryptoUriParser
import com.mycelium.wapi.wallet.colu.coins.RMCCoin
import com.mycelium.wapi.wallet.colu.coins.RMCCoinTest

class RMCUriParser(override val network: NetworkParameters)
    : CryptoUriParser(network, "rmc", RMCCoin, RMCCoinTest)