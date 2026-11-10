package com.mycelium.wapi.wallet.single

import com.mrd.bitlib.crypto.InMemoryPrivateKey
import com.mrd.bitlib.model.AddressType
import com.mrd.bitlib.model.NetworkParameters
import com.mycelium.wapi.wallet.btc.single.SingleAddressAccount
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Backup verification looks an account up by re-deriving its ID from the scanned
 * private key. These tests pin the contract between ID derivation at account
 * creation ([com.mycelium.wapi.wallet.btc.single.BitcoinSingleAddressModule.createAccount]
 * derives the ID from a forced P2SH_P2WPKH address, even for uncompressed keys) and
 * the candidate IDs used by verification, so a valid backup can never fail to match
 * its own account.
 */
class SingleAddressAccountIdTest {
    private val network = NetworkParameters.productionNetwork

    @Test
    fun possibleIdsContainCreationIdForUncompressedKey() {
        val publicKey = InMemoryPrivateKey(ByteArray(32) { 1 }, false).publicKey
        val creationId = SingleAddressAccount.calculateId(
                publicKey.toAddress(network, AddressType.P2SH_P2WPKH, true))

        val candidates = SingleAddressAccount.calculatePossibleIds(publicKey, network)

        assertTrue("Backup verification would not find an account imported from an uncompressed key",
                candidates.contains(creationId))
    }

    @Test
    fun possibleIdsContainCreationIdForCompressedKey() {
        val publicKey = InMemoryPrivateKey(ByteArray(32) { 1 }, true).publicKey
        val creationId = SingleAddressAccount.calculateId(
                publicKey.toAddress(network, AddressType.P2SH_P2WPKH, true))

        val candidates = SingleAddressAccount.calculatePossibleIds(publicKey, network)

        assertTrue(candidates.contains(creationId))
    }

    @Test
    fun possibleIdsContainLegacyP2pkhIdForBothKeyTypes() {
        for (compressed in listOf(false, true)) {
            val publicKey = InMemoryPrivateKey(ByteArray(32) { 2 }, compressed).publicKey
            val legacyId = SingleAddressAccount.calculateId(
                    publicKey.toAddress(network, AddressType.P2PKH))

            val candidates = SingleAddressAccount.calculatePossibleIds(publicKey, network)

            assertTrue("Legacy P2PKH-based account ID must stay verifiable (compressed=$compressed)",
                    candidates.contains(legacyId))
        }
    }
}
