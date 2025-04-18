package com.mycelium.wapi.wallet

import com.mrd.bitlib.crypto.RandomSource
import com.mrd.bitlib.util.BitUtils
import com.mrd.bitlib.util.HexUtils
import com.mycelium.wapi.wallet.KeyCipher.InvalidKeyCipher
import com.mycelium.wapi.wallet.btc.InMemoryBtcWalletManagerBacking
import org.junit.Assert
import org.junit.Test
import java.security.SecureRandom

class SecureKeyValueStoreTest {
    private class MyRandomSource : RandomSource {
        var _rnd: SecureRandom = SecureRandom(byteArrayOf(42))

        override fun nextBytes(bytes: ByteArray?) {
            _rnd.nextBytes(bytes)
        }
    }

    @Test
    @Throws(InvalidKeyCipher::class)
    fun storeAndRetrieveEncrypted() {
        val store = SecureKeyValueStore(InMemoryBtcWalletManagerBacking(), MyRandomSource())
        val cipher: KeyCipher? = AesKeyCipher.defaultKeyCipher()
        store.encryptAndStoreValue(ID_1, VALUE_1, cipher)
        val result = store.getDecryptedValue(ID_1, cipher)
        Assert.assertTrue(BitUtils.areEqual(result, VALUE_1))
    }

    @Test
    @Throws(InvalidKeyCipher::class)
    fun storeAndRetrievePlaintext() {
        val store = SecureKeyValueStore(InMemoryBtcWalletManagerBacking(), MyRandomSource())
        store.storePlaintextValue(ID_1, VALUE_1)
        val result = store.getPlaintextValue(ID_1)
        Assert.assertTrue(BitUtils.areEqual(result, VALUE_1))
    }


    companion object {
        private val ID_1: ByteArray = HexUtils.toBytes("000102030405060708090a0b0c0d0e0f")
        private val VALUE_1: ByteArray = HexUtils.toBytes("0123456789abcdef")
    }
}
