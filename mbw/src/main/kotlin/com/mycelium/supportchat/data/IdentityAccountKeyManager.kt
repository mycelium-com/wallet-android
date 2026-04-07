package com.mycelium.supportchat.data

import com.mrd.bitlib.crypto.PrivateKey
import com.mycelium.wapi.wallet.KeyCipher

interface IdentityAccountKeyManager {
    fun getPublicKeyForWebsite(key: String, cipher: KeyCipher): ByteArray
    fun getPrivateKeyForWebsite(key: String, cipher: KeyCipher): PrivateKey
}
