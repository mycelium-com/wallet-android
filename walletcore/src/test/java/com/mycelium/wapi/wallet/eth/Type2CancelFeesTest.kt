package com.mycelium.wapi.wallet.eth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigInteger

/**
 * Locks the type-2 cancel/RBF bump invariants (Этап 4). The stuck tx's tip is
 * not persisted, so the bump is derived from the known old cap; these tests
 * prove the result is always a valid, accept-able EIP-1559 replacement.
 */
class Type2CancelFeesTest {
    private fun gwei(n: Long) = BigInteger.valueOf(n).multiply(BigInteger.valueOf(1_000_000_000))

    // oldCap bumped by 10% the same way estimateCancelTx does.
    private fun bumped(oldCapGwei: Long) =
        gwei(oldCapGwei).multiply(BigInteger.valueOf(11)).divide(BigInteger.TEN)

    @Test
    fun `calm network — bump off old cap dominates`() {
        // old cap 100 Gwei → bumped 110 Gwei. Network calm: cap 30, tip 2.
        val (cap, tip) = type2CancelFees(bumped(100), gwei(30), gwei(2))
        assertEquals(gwei(110), cap)
        assertEquals(gwei(110), tip) // tip floored at bumpedOld → guarantees oldTip*1.1
        assertTrue("cap must be >= tip", cap >= tip)
    }

    @Test
    fun `fee spike — network floor dominates`() {
        // old cap 20 Gwei → bumped 22. Network spiked: cap 200, tip 50.
        val (cap, tip) = type2CancelFees(bumped(20), gwei(200), gwei(50))
        assertEquals(gwei(200), cap)
        assertEquals(gwei(50), tip)
        assertTrue("cap must be >= tip", cap >= tip)
    }

    @Test
    fun `cap always raised to at least the tip`() {
        // Pathological: network tip exceeds network cap. cap must still be >= tip.
        val (cap, tip) = type2CancelFees(bumped(10), gwei(15), gwei(80))
        assertEquals(gwei(80), tip)
        assertTrue("cap must be >= tip", cap >= tip)
        assertEquals(gwei(80), cap)
    }

    @Test
    fun `both fields clear the 10 percent replacement threshold over old cap`() {
        val oldCapGwei = 75L
        val (cap, tip) = type2CancelFees(bumped(oldCapGwei), gwei(1), gwei(1))
        val threshold = bumped(oldCapGwei)
        assertTrue("cap >= oldCap*1.1", cap >= threshold)
        assertTrue("tip >= oldCap*1.1 (so >= oldTip*1.1 since oldTip <= oldCap)", tip >= threshold)
    }
}
