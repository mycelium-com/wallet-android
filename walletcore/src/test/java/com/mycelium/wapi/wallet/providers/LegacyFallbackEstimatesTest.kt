package com.mycelium.wapi.wallet.providers

import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.eth.EthBlockchainService
import com.mycelium.wapi.wallet.eth.EthFeeLevel
import com.mycelium.wapi.wallet.eth.Eip1559Snapshot
import com.mycelium.wapi.wallet.eth.Eip1559Tier
import com.mycelium.wapi.wallet.eth.coins.EthMain
import com.mycelium.wapi.wallet.eth.coins.EthTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigInteger

/**
 * The blockbook fallback on an EIP-1559 chain. Reproduces the Sepolia send that went out
 * legacy at 1.05 Gwei while the picker showed 2.2–2.7 Gwei caps: four `estimatefee` answers
 * that differed by a few wei were treated as real tiers and the cached snapshot was dropped.
 */
class LegacyFallbackEstimatesTest {
    private val service = EthBlockchainService(emptyList())
    private fun v(wei: Long) = Value.valueOf(EthTest, wei)

    // Base fee readings a block apart, as returned by two blockbook servers on 2026-09-15.
    private val drifting = listOf(v(1_051_163_147), v(1_051_163_147), v(1_051_170_001), v(1_051_163_147))
    private val equal = List(4) { v(1_051_163_147) }

    @Test
    fun `drifting readings on a 1559 chain keep a snapshot and derived tiers`() {
        val est = legacyFallbackEstimates(EthTest, drifting, service, 1L)
        val snapshot = service.lastEip1559Fees()
        assertNotNull("snapshot must be cached so the send signs type-2", snapshot)
        assertEquals(BigInteger.valueOf(1_051_170_001), snapshot!!.baseFeePerGas)
        assertEquals(deriveFeeTiers(snapshot.baseFeePerGas)[0].maxFeePerGas, est.low.value)
        assertEquals(deriveFeeTiers(snapshot.baseFeePerGas)[3].maxFeePerGas, est.high.value)
        assertTrue("tiers must differ", est.low.value < est.economy.value && est.normal.value < est.high.value)
    }

    @Test
    fun `drifting and equal readings produce the same shape`() {
        val a = legacyFallbackEstimates(EthTest, equal, service, 1L)
        val b = legacyFallbackEstimates(EthTest, listOf(v(1_051_163_147), v(1_051_163_147), v(1_051_163_147), v(1_051_163_147)), service, 1L)
        assertEquals(a.normal, b.normal)
    }

    @Test
    fun `displayed cap recovers a tip from the cached snapshot`() {
        val est = legacyFallbackEstimates(EthTest, drifting, service, 1L)
        // What the user picks is the cap; signing must find a tip for it.
        assertNotNull(service.priorityFeeFor(est.normal.value))
        assertNotNull(service.priorityFeeFor(est.low.value))
    }

    @Test
    fun `legacy chain passes readings through and clears the snapshot`() {
        service.cacheEip1559Fees(Eip1559Snapshot(BigInteger.TEN,
            listOf(Eip1559Tier(EthFeeLevel.LOW, BigInteger.TEN, BigInteger.ONE))))
        val readings = listOf(v(1), v(2), v(3), v(4)).map { Value.valueOf(EthMain, it.value) }
        val est = legacyFallbackEstimates(EthMain, readings, service, 1L)
        assertEquals(BigInteger.ONE, est.low.value)
        assertEquals(BigInteger.valueOf(4), est.high.value)
        assertNull(service.lastEip1559Fees())
    }
}
