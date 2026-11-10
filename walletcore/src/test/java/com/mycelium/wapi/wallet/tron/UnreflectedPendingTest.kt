package com.mycelium.wapi.wallet.tron

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigInteger

class UnreflectedPendingTest {
    private fun big(v: Long) = BigInteger.valueOf(v)

    @Test
    fun `pending tx the node has not debited yet is reserved`() {
        val before = linkedMapOf("a" to big(495))
        assertEquals(big(1), unreflectedPendingDebits(before, big(495), listOf(PendingDebit("a", big(1), big(1)))))
    }

    @Test
    fun `pending tx already debited by the node is not reserved again`() {
        val before = linkedMapOf("a" to big(495))
        assertEquals(big(0), unreflectedPendingDebits(before, big(494), listOf(PendingDebit("a", big(1), big(1)))))
    }

    @Test
    fun `fee below the estimate still counts as debited`() {
        // estimated fee 2, actually burned 0
        val before = linkedMapOf("a" to big(100))
        assertEquals(big(0), unreflectedPendingDebits(before, big(90), listOf(PendingDebit("a", big(10), big(12)))))
    }

    @Test
    fun `only the first of two sends is on-chain`() {
        val before = linkedMapOf("a" to big(100), "b" to big(100))
        val pending = listOf(PendingDebit("b", big(20), big(20)), PendingDebit("a", big(30), big(30)))
        assertEquals(big(20), unreflectedPendingDebits(before, big(70), pending))
    }

    @Test
    fun `tx without a recorded balance (broadcast before restart) is reserved`() {
        assertEquals(big(5), unreflectedPendingDebits(linkedMapOf(), big(50), listOf(PendingDebit("x", big(5), big(5)))))
    }
}
