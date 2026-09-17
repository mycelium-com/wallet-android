package com.mycelium.wapi.wallet.eth

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigInteger

class DefaultGasLimitTest {
    @Test
    fun `plain transfer`() {
        assertEquals(BigInteger.valueOf(21000), EthAccount.defaultGasLimit(null))
        assertEquals(BigInteger.valueOf(21000), EthAccount.defaultGasLimit(""))
    }

    @Test
    fun `calldata is charged at the EIP-7623 floor`() {
        // 0x1234: two non-zero bytes = 8 tokens x 10 gas; accepted on Sepolia at exactly 21080, rejected at 21079
        assertEquals(BigInteger.valueOf(21080), EthAccount.defaultGasLimit("0x1234"))
        // zero byte = 1 token, non-zero = 4 tokens
        assertEquals(BigInteger.valueOf(21000 + 50), EthAccount.defaultGasLimit("0x00ff"))
    }
}
