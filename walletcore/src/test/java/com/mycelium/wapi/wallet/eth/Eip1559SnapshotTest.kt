package com.mycelium.wapi.wallet.eth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigInteger

/**
 * Covers Этап 1's tip-recovery logic ([EthBlockchainService.priorityFeeFor]):
 * the fee picker only carries the chosen maxFeePerGas cap, so signing recovers
 * the matching tip from the cached snapshot. Verifies tier matching and the
 * headroom/floor clamps that keep the recovered tip valid for a type-2 tx.
 */
class Eip1559SnapshotTest {
    private val service = EthBlockchainService(emptyList())

    private fun gwei(n: Long): BigInteger = BigInteger.valueOf(n).multiply(BigInteger.valueOf(1_000_000_000))

    private fun snapshot(baseFeeGwei: Long, vararg tiers: Eip1559Tier) =
        Eip1559Snapshot(gwei(baseFeeGwei), tiers.toList())

    private fun tier(level: EthFeeLevel, capGwei: Long, tipGwei: Long) =
        Eip1559Tier(level, gwei(capGwei), gwei(tipGwei))

    @Test
    fun `null when no snapshot cached`() {
        service.clearEip1559Fees()
        assertNull(service.priorityFeeFor(gwei(50)))
    }

    @Test
    fun `exact tier match returns that tier tip`() {
        service.cacheEip1559Fees(
            snapshot(10,
                tier(EthFeeLevel.LOW, capGwei = 20, tipGwei = 1),
                tier(EthFeeLevel.HIGH, capGwei = 100, tipGwei = 5)))
        assertEquals(gwei(5), service.priorityFeeFor(gwei(100)))
    }

    @Test
    fun `nearest tier wins when cap is between tiers`() {
        service.cacheEip1559Fees(
            snapshot(10,
                tier(EthFeeLevel.LOW, capGwei = 20, tipGwei = 1),
                tier(EthFeeLevel.ECONOMY, capGwei = 40, tipGwei = 2),
                tier(EthFeeLevel.NORMAL, capGwei = 60, tipGwei = 3)))
        // 45 is closest to ECONOMY (40) → tip 2 Gwei
        assertEquals(gwei(2), service.priorityFeeFor(gwei(45)))
    }

    @Test
    fun `tip clamped down to headroom`() {
        // baseFee 10, cap 12 → headroom 2 Gwei, but tier tip is 5 Gwei.
        service.cacheEip1559Fees(
            snapshot(10, tier(EthFeeLevel.LOW, capGwei = 12, tipGwei = 5)))
        assertEquals(gwei(2), service.priorityFeeFor(gwei(12)))
    }

    @Test
    fun `tip floored at one wei`() {
        service.cacheEip1559Fees(
            snapshot(10, Eip1559Tier(EthFeeLevel.LOW, gwei(50), BigInteger.ZERO)))
        assertEquals(BigInteger.ONE, service.priorityFeeFor(gwei(50)))
    }

    @Test
    fun `null when cap leaves no headroom above base fee`() {
        service.cacheEip1559Fees(
            snapshot(10, tier(EthFeeLevel.LOW, capGwei = 20, tipGwei = 1)))
        // chosen cap == base fee → headroom 0 → fall back to legacy signing
        assertNull(service.priorityFeeFor(gwei(10)))
    }

    @Test
    fun `clear resets recovery`() {
        service.cacheEip1559Fees(
            snapshot(10, tier(EthFeeLevel.LOW, capGwei = 20, tipGwei = 1)))
        service.clearEip1559Fees()
        assertNull(service.priorityFeeFor(gwei(20)))
    }
}
