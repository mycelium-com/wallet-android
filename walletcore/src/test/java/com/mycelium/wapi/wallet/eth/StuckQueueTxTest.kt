package com.mycelium.wapi.wallet.eth

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigInteger

/**
 * The queue of 0x065A…86E3 on 2026-10-26: a cancel at nonce 26 priced under a base fee that had
 * since quadrupled, later cancels at the market. Every later tx waits behind 26.
 */
class StuckQueueTxTest {
    private val mapper = ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    private val owner = "0x065a575c8aeda4d4b1261fb741246018980486e3"
    private val payee = "0xcc633e861ade1dd52b70edc70bdffc40b2eb59c5"
    private val baseFee = BigInteger.valueOf(307_000_000)

    private fun tx(txid: String, nonce: Long, gasPrice: Long, from: String = owner,
                   to: String = owner, confirmations: Int = 0): Tx =
        mapper.readValue("""{
            "txid": "$txid",
            "vin": [{"addresses": ["$from"]}],
            "vout": [{"addresses": ["$to"]}],
            "blockHeight": ${if (confirmations == 0) 0 else 100},
            "confirmations": $confirmations,
            "value": "0",
            "ethereumSpecific": {"nonce": "$nonce", "gasLimit": "50000", "gasPrice": "$gasPrice", "status": true},
            "tokenTransfers": []
        }""", Tx::class.java)

    private val queue = listOf(
        tx("0x7535", 28, 620_009_962),
        tx("0xc46f", 27, 569_574_478),
        tx("0xb4e8", 27, 300_000_000, to = payee), // replaced in the mempool by the dearer 0xc46f
        tx("0xa102", 26, 74_864_249),
        tx("0xefbf", 25, 39_194_828, confirmations = 5),
    )

    @Test
    fun `the earliest tx under the base fee holds the queue`() {
        val blocker = pickStuckQueueTx(queue, owner, beforeNonce = null, baseFee = baseFee)
        assertEquals("0xa102", blocker?.txid)
        assertEquals(BigInteger.valueOf(26), blocker?.nonce)
    }

    @Test
    fun `a later tx is reported waiting behind it`() {
        assertEquals("0xa102", pickStuckQueueTx(queue, owner, BigInteger.valueOf(28), baseFee)?.txid)
    }

    @Test
    fun `the stuck tx itself waits behind nothing`() {
        assertNull(pickStuckQueueTx(queue, owner, BigInteger.valueOf(26), baseFee))
    }

    @Test
    fun `a slot is judged by its dearest tx`() {
        // 27 holds a 0.3 gwei send and its 0.57 gwei cancel; with 26 gone and the base fee at
        // 0.4 the slot is fine because the mempool kept the cancel.
        val rest = queue.filter { it.txid != "0xa102" }
        assertNull(pickStuckQueueTx(rest, owner, null, BigInteger.valueOf(400_000_000)))
    }

    @Test
    fun `incoming pending txs carry someone else's nonce and are ignored`() {
        val incoming = listOf(tx("0x9999", 3, 1, from = payee))
        assertNull(pickStuckQueueTx(incoming, owner, null, baseFee))
    }
}
