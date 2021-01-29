package com.mycelium.wapi.content

import com.mrd.bitlib.model.NetworkParameters
import com.mycelium.wapi.wallet.btcvault.coins.BitcoinVaultMain
import com.mycelium.wapi.wallet.btcvault.coins.BitcoinVaultTest


class BitcoinVaultUriParser(override val network: NetworkParameters)
    : CryptoUriParser(network, "bitcoinvault", BitcoinVaultMain, BitcoinVaultTest)