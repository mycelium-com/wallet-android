package com.mycelium.wapi.wallet.tron

import com.mycelium.wapi.wallet.tron.remote.AccountResourceInfo
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The node pays a transaction's bandwidth from the staked quota or from the free daily quota,
 * never by combining both. Summing the pools said "(free)" for a 345-byte TRC20 transfer when
 * 334 free + 23 staked were left, and the network then burned 0.345 TRX (Nile tx 3e9643bd…).
 */
class AccountResourceInfoTest {
    private fun info(free: Long, staked: Long) = AccountResourceInfo(free, staked, 0, 0, 0)

    @Test
    fun poolsAreNotSummed() {
        assertFalse(info(free = 334, staked = 23).coversBandwidth(345))
        assertFalse(info(free = 200, staked = 200).coversBandwidth(345))
    }

    @Test
    fun eitherPoolAloneCovers() {
        assertTrue(info(free = 345, staked = 0).coversBandwidth(345))
        assertTrue(info(free = 0, staked = 345).coversBandwidth(345))
        assertTrue(info(free = 600, staked = 23).coversBandwidth(268))
    }

    @Test
    fun nothingLeft() {
        assertFalse(info(free = 0, staked = 0).coversBandwidth(268))
    }
}
