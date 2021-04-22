package com.mycelium.wapi.wallet.btc

import com.mrd.bitlib.crypto.InMemoryPrivateKey
import com.mrd.bitlib.model.BitcoinAddress
import com.mycelium.wapi.wallet.KeyCipher

interface PrivateKeyProvider {
    @Throws(KeyCipher.InvalidKeyCipher::class)
    fun getPrivateKeyForAddress(address: BitcoinAddress, cipher: KeyCipher): InMemoryPrivateKey?

}