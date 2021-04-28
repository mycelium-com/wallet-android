package com.mycelium.wapi.content.colu.mss

import com.mrd.bitlib.model.NetworkParameters
import com.mycelium.wapi.content.CryptoUriParser
import com.mycelium.wapi.wallet.colu.coins.MASSCoin
import com.mycelium.wapi.wallet.colu.coins.MASSCoinTest


class MSSUriParser(override val network: NetworkParameters)
    : CryptoUriParser(network, "mss", MASSCoin, MASSCoinTest)