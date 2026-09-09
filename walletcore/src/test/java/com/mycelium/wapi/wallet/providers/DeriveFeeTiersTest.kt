package com.mycelium.wapi.wallet.providers

import com.mycelium.wapi.wallet.eth.EthFeeLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigInteger

/**
 * Blockbook's Ethereum estimatefee answers the same number for every block target,
 * so the four fee tiers used to collapse into a single value that barely cleared
 * the base fee — the picker did nothing and sends could sit in the mempool. These
 * cover the derivation that replaced it.
 */
class DeriveFeeTiersTest {

    // Sepolia base fee observed while reproducing the stuck-transaction report.
    private val baseFee = BigInteger("1086298502")

    @Test
    fun `tiers are strictly increasing`() {
        val caps = deriveFeeTiers(baseFee).map { it.maxFeePerGas }
        caps.zipWithNext { a, b -> assertTrue("$a should be below $b", a < b) }
    }

    @Test
    fun `every tier clears the current base fee`() {
        deriveFeeTiers(baseFee).forEach {
            assertTrue(
                "${it.level} cap ${it.maxFeePerGas} must exceed base fee $baseFee",
                it.maxFeePerGas > baseFee
            )
        }
    }

    @Test
    fun `every tier leaves room for its tip above the base fee`() {
        deriveFeeTiers(baseFee).forEach {
            assertTrue(
                "${it.level} leaves no headroom for a tip",
                it.maxFeePerGas - baseFee >= it.maxPriorityFeePerGas
            )
        }
    }

    @Test
    fun `caps follow baseFee times headroom plus tip`() {
        val tiers = deriveFeeTiers(baseFee).associateBy { it.level }
        assertEquals(
            baseFee * BigInteger.valueOf(105) / BigInteger.valueOf(100) + BigInteger.valueOf(50_000_000),
            tiers[EthFeeLevel.LOW]!!.maxFeePerGas
        )
        assertEquals(
            baseFee * BigInteger.valueOf(180) / BigInteger.valueOf(100) + BigInteger.valueOf(2_000_000_000),
            tiers[EthFeeLevel.HIGH]!!.maxFeePerGas
        )
    }

    @Test
    fun `a zero base fee still yields usable non-zero tips`() {
        deriveFeeTiers(BigInteger.ZERO).forEach {
            assertTrue(it.maxPriorityFeePerGas.signum() > 0)
            assertEquals(it.maxPriorityFeePerGas, it.maxFeePerGas)
        }
    }
}
