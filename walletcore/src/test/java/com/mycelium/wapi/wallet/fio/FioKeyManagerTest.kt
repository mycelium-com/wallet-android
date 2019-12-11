package com.mycelium.wapi.wallet.fio

import com.mrd.bitlib.crypto.Bip39
import com.mrd.bitlib.crypto.InMemoryPrivateKey
import com.mrd.bitlib.crypto.RandomSource
import com.mrd.bitlib.model.NetworkParameters
import com.mycelium.wapi.wallet.AesKeyCipher
import com.mycelium.wapi.wallet.SecureKeyValueStore
import com.mycelium.wapi.wallet.btc.InMemoryBtcWalletManagerBacking
import com.mycelium.wapi.wallet.masterseed.MasterSeedManager
import org.junit.Assert
import org.junit.Before
import org.junit.Test

import org.mockito.Mockito

class FioKeyManagerTest {
    val masterSeedWords = "valley alien library bread worry brother bundle hammer loyal barely dune brave".split(" ")
    val fioAddress = "FIO5kJKNHwctcfUM5XZyiWSqSTM5HTzznJP9F3ZdbhaQAHEVq575o"
    val privKey: InMemoryPrivateKey
    val sut: FioKeyManager

    init {
        val base58PrivKey = "5Kbb37EAqQgZ9vWUHoPiC2uXYhyGSFNbL6oiDp24Ea1ADxV1qnu"
        privKey = InMemoryPrivateKey.fromBase58String(base58PrivKey, NetworkParameters.productionNetwork).get()
        val backing = InMemoryBtcWalletManagerBacking()
        val fakeRandomSource = Mockito.mock<RandomSource>(RandomSource::class.java)
        val store = SecureKeyValueStore(backing, fakeRandomSource)
        val masterSeedManager = MasterSeedManager(store).apply {
            val masterSeed = Bip39.generateSeedFromWordList(masterSeedWords, "")
            configureBip32MasterSeed(masterSeed, AesKeyCipher.defaultKeyCipher())
        }
        sut = FioKeyManager(masterSeedManager)
    }

    @Test
    fun getFioPublicKey() {
        val actualPublicKey = sut.getFioPublicKey()
        val expectPublicKey = privKey.publicKey
        Assert.assertEquals(expectPublicKey, actualPublicKey)
    }

    @Test
    fun formatPubKey() {
        val actualPublicKey = sut.getFioPublicKey()
        Assert.assertEquals(fioAddress, sut.formatPubKey(actualPublicKey))
    }
}