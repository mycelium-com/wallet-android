package com.mycelium.wapi.content.btc

import com.mrd.bitlib.model.NetworkParameters
import com.mycelium.wapi.content.CryptoUriParser
import com.mycelium.wapi.wallet.btc.coins.BitcoinMain
import com.mycelium.wapi.wallet.btc.coins.BitcoinTest


class BitcoinUriParser(override val network: NetworkParameters)
    : CryptoUriParser(network, "bitcoin", BitcoinMain, BitcoinTest)