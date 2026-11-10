package com.mycelium.wapi.wallet.eth

import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.eth.coins.EthTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigInteger

/** Locks the type-2 vs legacy fee accounting on [EthTransaction] (Этап 2). */
class EthTransactionFeeTest {
    private val to = "0x3535353535353535353535353535353535353535"
    private val gasLimit = BigInteger.valueOf(21_000)
    private val nonce = BigInteger.ZERO
    private val cap = BigInteger("100000000000")  // 100 Gwei
    private val tip = BigInteger("2000000000")    // 2 Gwei
    private val legacyPrice = BigInteger("50000000000") // 50 Gwei

    @Test
    fun `legacy tx uses gasPrice`() {
        val tx = EthTransaction(EthTest, to, Value.zeroValue(EthTest), legacyPrice, nonce, gasLimit, "")
        assertFalse(tx.isEip1559)
        assertEquals(legacyPrice, tx.effectiveGasPrice)
        assertEquals(Value.valueOf(EthTest, legacyPrice * gasLimit), tx.totalFee())
    }

    @Test
    fun `type-2 tx uses maxFeePerGas as the fee cap`() {
        val tx = EthTransaction(EthTest, to, Value.zeroValue(EthTest), cap, nonce, gasLimit, "",
                maxFeePerGas = cap, maxPriorityFeePerGas = tip)
        assertTrue(tx.isEip1559)
        assertEquals(cap, tx.effectiveGasPrice)
        // Displayed/accounted fee is the cap * gasLimit, not the tip.
        assertEquals(Value.valueOf(EthTest, cap * gasLimit), tx.totalFee())
    }

    @Test
    fun `tx is legacy when only one 1559 field is set`() {
        val capOnly = EthTransaction(EthTest, to, Value.zeroValue(EthTest), legacyPrice, nonce, gasLimit, "",
                maxFeePerGas = cap)
        val tipOnly = EthTransaction(EthTest, to, Value.zeroValue(EthTest), legacyPrice, nonce, gasLimit, "",
                maxPriorityFeePerGas = tip)
        assertFalse(capOnly.isEip1559)
        assertFalse(tipOnly.isEip1559)
    }
}
