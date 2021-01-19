package com.mrd.bitlib.crypto

import com.mrd.bitlib.model.AddressType
import com.mrd.bitlib.model.BitcoinAddress
import com.mrd.bitlib.model.NetworkParameters
import com.mrd.bitlib.util.HashUtils
import com.mrd.bitlib.util.HexUtils.toBytes
import com.mrd.bitlib.util.X509Utils
import org.junit.Test
import com.mycelium.testhelper.SignatureTestVectors

import org.junit.Assert.*
import java.lang.Exception

class PublicKeyTest {
    // we found problems spending from a SA account derived from 91tVaPm7poHgaNx2vYKtim84ykVzoB8opPv1dgieHvoAAHSmyX1
    // 91tVaPm7poHgaNx2vYKtim84ykVzoB8opPv1dgieHvoAAHSmyX1 is an uncompressed testnet Private Key WIF 51 characters base58
    val sk = InMemoryPrivateKey("91tVaPm7poHgaNx2vYKtim84ykVzoB8opPv1dgieHvoAAHSmyX1", NetworkParameters.testNetwork)
    val pk = sk.publicKey

    @Test
    fun getQ() {
        val pk = PublicKey(toBytes("03ad1d8e89212f0b92c74d23bb710c00662ad1470198ac48c43f7d6f93a2a26873"))
        assertTrue(pk.Q.isCompressed)
    }

    @Test
    fun uncompressedYieldsP2PKHandP2SH_P2WPKH() {
        assertFalse(pk.Q.isCompressed)
        val allAddresses = pk.getAllSupportedAddresses(NetworkParameters.testNetwork)
        assertNotNull("We should get a p2pkh address ... ", allAddresses[AddressType.P2PKH])
        assertEquals("and only those two addresses.", 1, allAddresses.size)
    }

    @Test(expected = IllegalStateException::class)
    fun P2WPKHFromUncompressedYieldsNull() {
        assertNull(pk.toAddress(NetworkParameters.testNetwork, AddressType.P2WPKH))
    }

    @Test
    fun testVerifyDerEncodedSignature() {
        for (tv in SignatureTestVectors.bitcoinMessageTestVectors) {
            val address = BitcoinAddress.fromString(tv.address)
            val msg = tv.message
            val data = HashUtils.doubleSha256(X509Utils.formatMessageForSigning(msg))
            val sig = tv.signature
            val sigValid = try {
                val signedMessage = SignedMessage.validate(address, msg, sig)
                signedMessage.publicKey.verifyDerEncodedSignature(data, signedMessage.derEncodedSignature)
            } catch (e: Exception) {
                false
            }
            assertTrue("Test Vector ${tv.name}:\n${tv.message}\nshould verify\n", sigValid)
        }
    }
}