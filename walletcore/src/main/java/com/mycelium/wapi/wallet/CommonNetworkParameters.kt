package com.mycelium.wapi.wallet

import java.io.Serializable

//TODO divide on Common and BTC based parameters
interface CommonNetworkParameters : Serializable {

    fun getStandardAddressHeader(): Int

    fun getMultisigAddressHeader(): Int

    fun getGenesisBlock(): ByteArray?

    fun getPort(): Int

    fun getPacketMagic(): Int

    fun getPacketMagicBytes(): ByteArray?

    fun isProdnet(): Boolean

    fun isRegTest(): Boolean

    fun isTestnet(): Boolean

    // used for Trezor coin_name
    fun getCoinName(): String?

    fun getBip44CoinType(): Int
}