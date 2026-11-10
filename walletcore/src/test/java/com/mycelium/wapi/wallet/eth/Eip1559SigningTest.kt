package com.mycelium.wapi.wallet.eth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.web3j.crypto.Credentials
import org.web3j.crypto.Hash
import org.web3j.crypto.RawTransaction
import org.web3j.crypto.TransactionDecoder
import org.web3j.crypto.TransactionEncoder
import org.web3j.crypto.transaction.type.Transaction1559
import org.web3j.utils.Numeric
import java.math.BigInteger

/**
 * De-risks Этап 2 of the EIP-1559 migration: proves the exact Web3j 4.12.3
 * API we plan to call in [EthAccount.signTx] really produces a valid type-2
 * envelope, and — critically — confirms the parameter ORDER of the 8-arg
 * [RawTransaction.createTransaction] overload by round-tripping through the
 * decoder. If web3j ever reorders those args, this test breaks loudly instead
 * of us silently signing a tx with the tip and cap swapped.
 */
class Eip1559SigningTest {
    // Throwaway key — test vector only, never used on any real account.
    private val credentials = Credentials.create(
        "0x4646464646464646464646464646464646464646464646464646464646464646")

    private val chainId = 1L
    private val nonce = BigInteger.valueOf(9)
    private val gasLimit = BigInteger.valueOf(21_000)
    private val to = "0x3535353535353535353535353535353535353535"
    private val value = BigInteger("1000000000000000000") // 1 ETH
    private val data = ""
    private val maxPriorityFeePerGas = BigInteger("2000000000")  // 2 Gwei tip
    private val maxFeePerGas = BigInteger("100000000000")        // 100 Gwei cap

    @Test
    fun `type-2 overload signs an EIP-1559 envelope`() {
        val raw = RawTransaction.createTransaction(
            chainId, nonce, gasLimit, to, value, data,
            maxPriorityFeePerGas, maxFeePerGas)

        // chainId is embedded in the type-2 envelope, so signMessage is called
        // WITHOUT the chainId arg (unlike the legacy EIP-155 path).
        val signed = TransactionEncoder.signMessage(raw, credentials)

        // EIP-2718 typed-tx envelope: first byte is the transaction type (0x02).
        assertEquals("first byte must be the type-2 marker", 0x02.toByte(), signed[0])

        // The txid we persist must be keccak over the actually-broadcast bytes.
        val txHash = Hash.sha3(signed)
        assertEquals(32, txHash.size)
    }

    @Test
    fun `decoded type-2 tx round-trips fields in the order we pass them`() {
        val raw = RawTransaction.createTransaction(
            chainId, nonce, gasLimit, to, value, data,
            maxPriorityFeePerGas, maxFeePerGas)
        val signedHex = Numeric.toHexString(TransactionEncoder.signMessage(raw, credentials))

        val decoded = TransactionDecoder.decode(signedHex)
        assertTrue("decoded tx must be EIP-1559", decoded.type.isEip1559)

        assertEquals(nonce, decoded.nonce)
        assertEquals(gasLimit, decoded.gasLimit)
        assertEquals(to.lowercase(), decoded.to.lowercase())
        assertEquals(value, decoded.value)

        val tx1559 = decoded.transaction as Transaction1559
        // The two fee fields are the migration's whole point — verify they land
        // where we intend and are NOT swapped.
        assertEquals("tip (maxPriorityFeePerGas)", maxPriorityFeePerGas, tx1559.maxPriorityFeePerGas)
        assertEquals("cap (maxFeePerGas)", maxFeePerGas, tx1559.maxFeePerGas)
        assertEquals(chainId, tx1559.chainId)
    }

    @Test
    fun `legacy path stays type-0 and differs from type-2`() {
        val legacyRaw = RawTransaction.createTransaction(
            nonce, maxFeePerGas /* used as gasPrice */, gasLimit, to, value, data)
        val legacySigned = TransactionEncoder.signMessage(legacyRaw, chainId, credentials)

        // Legacy is an RLP list, whose first byte is >= 0xc0 — never 0x02.
        assertNotEquals(0x02.toByte(), legacySigned[0])
        assertTrue("legacy must be an RLP list", (legacySigned[0].toInt() and 0xff) >= 0xc0)
    }
}
