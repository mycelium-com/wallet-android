package com.mycelium.wapi.wallet

import com.mrd.bitlib.crypto.*
import com.mrd.bitlib.model.AddressType
import com.mrd.bitlib.model.NetworkParameters
import org.junit.Test

import org.junit.Assert.assertEquals

import java.security.SecureRandom

class BitIdKeyDerivationTest {

    private class MyRandomSource : RandomSource {
        internal var rand: SecureRandom = SecureRandom(byteArrayOf(42))

        override fun nextBytes(bytes: ByteArray) {
            rand.nextBytes(bytes)
        }
    }


    @Test
    @Throws(KeyCipher.InvalidKeyCipher::class)
    fun bitIdDefaultAccount() {
        val seed = Bip39.generateSeedFromWordList(WORD_LIST, "")
        val rootNode = HdKeyNode.fromSeed(seed.bip32Seed)
        val store = SecureKeyValueStore(InMemoryWalletManagerBacking(), MyRandomSource())
        val cipher = AesKeyCipher.defaultKeyCipher()

        val identityManager = IdentityAccountKeyManager.createNew(rootNode, store, cipher)
        val priv = identityManager.getPrivateKeyForWebsite(WEBSITE, cipher)
        val pub = priv.publicKey
        val address = pub.toAddress(NetworkParameters.productionNetwork, AddressType.P2PKH)

        assertEquals(PUBKEY_DEFAULT, pub.toString())
        assertEquals(ADDRESS_DEFAULT, address!!.toString())
    }

    @Test
    @Throws(KeyCipher.InvalidKeyCipher::class)
    fun bitIdOtherAccount() {
        val seed = Bip39.generateSeedFromWordList(WORD_LIST, PWD)
        val rootNode = HdKeyNode.fromSeed(seed.bip32Seed)
        val store = SecureKeyValueStore(InMemoryWalletManagerBacking(), MyRandomSource())
        val cipher = AesKeyCipher.defaultKeyCipher()

        val identityManager = IdentityAccountKeyManager.createNew(rootNode, store, cipher)
        val priv = identityManager.getPrivateKeyForWebsite(WEBSITE, cipher)
        val pub = priv.publicKey
        val address = pub.toAddress(NetworkParameters.productionNetwork, AddressType.P2PKH)

        assertEquals(PUBKEY_OTHER, pub.toString())
        assertEquals(ADDRESS_OTHER, address!!.toString())
    }

    @Test
    @Throws(KeyCipher.InvalidKeyCipher::class)
    fun bitIdBipTestVector() {
        val seed = Bip39.generateSeedFromWordList(WORD_LIST_BITID, "")
        val rootNode = HdKeyNode.fromSeed(seed.bip32Seed)
        val store = SecureKeyValueStore(InMemoryWalletManagerBacking(), MyRandomSource())
        val cipher = AesKeyCipher.defaultKeyCipher()

        val identityManager = IdentityAccountKeyManager.createNew(rootNode, store, cipher)
        val priv = identityManager.getPrivateKeyForWebsite(WEBSITE_BITID, cipher)
        val pub = priv.publicKey
        val address = pub.toAddress(NetworkParameters.productionNetwork, AddressType.P2PKH)

        assertEquals(ADDRESS_BITID, address!!.toString())
    }

    companion object {
        private val WORD_LIST = arrayOf("abandon", "abandon", "abandon", "abandon", "abandon", "abandon", "abandon", "abandon", "abandon", "abandon", "abandon", "about")
        private const val PWD = "SecondIdentity"
        private const val WEBSITE = "https://satoshi@bitcoin.org/login"
        private const val PUBKEY_DEFAULT = "030a79ba07392dafab29e2bf01917dcb2b1cb235ccad9c7a59639ad0f84c3f619c"
        private const val ADDRESS_DEFAULT = "1LbxwgBqp6VYXfoadiLRVF1jaDxqL4SdRz"
        private const val PUBKEY_OTHER = "0265da9147121706403032fb22107206b0c510de65a19711eca5781edf67639598"
        private const val ADDRESS_OTHER = "11XiTMf6dULM8Uk7QohJMDEvdW6Lqy2gG"

        // test vectors from https://github.com/bitid/bitid/blob/master/BIP_draft.md
        private val WORD_LIST_BITID = arrayOf("inhale", "praise", "target", "steak", "garlic", "cricket", "paper", "better", "evil", "almost", "sadness", "crawl", "city", "banner", "amused", "fringe", "fox", "insect", "roast", "aunt", "prefer", "hollow", "basic", "ladder")
        private const val WEBSITE_BITID = "http://bitid.bitcoin.blue/callback"
        private const val ADDRESS_BITID = "1J34vj4wowwPYafbeibZGht3zy3qERoUM1"
    }
}
