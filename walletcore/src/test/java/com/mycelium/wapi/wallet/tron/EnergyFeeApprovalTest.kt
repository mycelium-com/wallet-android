package com.mycelium.wapi.wallet.tron

import com.mycelium.wapi.wallet.tron.energy.EnergyFeeQuote
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigInteger

class EnergyFeeApprovalTest {
    private fun quote(units: Long, decimals: Int = 6, wallet: String? = "TFeeWallet") = EnergyFeeQuote(
        transactionId = 1, feeAmount = "", feeAmountUnits = BigInteger.valueOf(units), tokenSymbol = "USDT",
        tokenContract = "", tokenDecimals = decimals, transferMethod = "two_transactions", feeWallet = wallet,
        energyNeeded = 0, bandwidthNeeded = 0, recipientHasToken = true, minFeeLimit = 0,
        expiresAtMs = Long.MAX_VALUE, estimatedTimeSec = null, balanceOk = true, balanceMessage = null,
    )

    private val shown = quote(1_500_000) // 1.5 USDT

    @Test
    fun sameOrLowerFeeIsApproved() {
        assertTrue(quote(1_500_000).isWithinApproval(shown, 6))
        assertTrue(quote(1_000_000).isWithinApproval(shown, 6))
    }

    @Test
    fun smallRepricingWithinToleranceIsApproved() {
        assertTrue(quote(1_650_000).isWithinApproval(shown, 6)) // exactly +10%
        assertFalse(quote(1_650_001).isWithinApproval(shown, 6))
    }

    @Test
    fun inflatedFeeIsRejected() {
        assertFalse(quote(1_000_000_000).isWithinApproval(shown, 6))
    }

    @Test
    fun inflationThroughDecimalsIsRejected() {
        // Same display-looking units, but 3 decimals instead of 6 → 1000x the fee in wallet units
        assertFalse(quote(1_500_000, decimals = 3).isWithinApproval(shown, 6))
    }

    @Test
    fun differentOrMissingFeeWalletIsRejected() {
        assertFalse(quote(1_500_000, wallet = "TAttacker").isWithinApproval(shown, 6))
        assertFalse(quote(1_500_000, wallet = null).isWithinApproval(shown, 6))
    }
}
