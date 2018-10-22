package com.mycelium.wapi.wallet.btc.single

import com.mrd.bitlib.crypto.InMemoryPrivateKey
import com.mrd.bitlib.crypto.PublicKey
import com.mycelium.wapi.wallet.KeyCipher
import com.mycelium.wapi.wallet.manager.Config

open class PublicSingleConfig(val publicKey: PublicKey) : Config {
    override fun getType(): String = "public_bitcoin_single"
}

open class PrivateSingleConfig(val privateKey: InMemoryPrivateKey, val cipher: KeyCipher) : Config {
    override fun getType(): String = "private_bitcoin_single"
}

