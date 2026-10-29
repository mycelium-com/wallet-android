package com.mycelium.wapi.wallet.eth.aa

import org.junit.Assert.assertEquals
import org.junit.Test
import org.web3j.utils.Numeric
import java.math.BigInteger

/** Expected values come from the chain / foundry, not from this encoder (see each test). */
class UserOperationEncoderTest {
    private val paymaster = "0x888888888888Ec68A58AB8094Cc1AD20Ba3D2402"
    private val usdc = "0x1c7D4B196Cb0C7B01d743Fbc6116a902379C7238"
    private val sender = "0x058c34d11b52B2D3AB3F2B44a94213584c2DC991"

    /**
     * Reference: EntryPoint v0.8 on Sepolia,
     * `cast call 0x4337…ff108 "getUserOpHash((address,uint256,bytes,bytes,bytes32,uint256,bytes32,bytes,bytes))(bytes32)"`
     * with the same packed fields.
     */
    @Test
    fun userOpHashMatchesEntryPoint() {
        val op = UserOperation(
            sender = sender,
            nonce = BigInteger.valueOf(5),
            callData = Numeric.hexStringToByteArray("0xdeadbeef"),
            callGasLimit = BigInteger.valueOf(0x5678),
            verificationGasLimit = BigInteger.valueOf(0x1234),
            preVerificationGas = BigInteger.valueOf(39612),
            maxFeePerGas = BigInteger.valueOf(0x77359400),
            maxPriorityFeePerGas = BigInteger.valueOf(0x3b9aca00),
            paymaster = paymaster,
            paymasterVerificationGasLimit = BigInteger.valueOf(100_000),
            paymasterPostOpGasLimit = BigInteger.valueOf(50_000),
            paymasterData = Numeric.hexStringToByteArray("0x03aabb")
        )
        val hash = UserOperationEncoder.getUserOpHash(op, AAChains.ENTRY_POINT_V08, AAChains.ETHEREUM_SEPOLIA)
        assertEquals("0xa37cef518f89262c147a0f3fba8fc59199b1ff58ecfbc4bd67ae101da5e29456", Numeric.toHexString(hash))
    }

    /** Reference: `cast calldata "executeBatch((address,uint256,bytes)[])" "[(usdc,0,approve…),(sender,7,0x)]"`. */
    @Test
    fun executeBatchMatchesAbi() {
        val calls = listOf(
            Call(usdc, data = UserOperationEncoder.encodeERC20Approve(paymaster, BigInteger.ZERO)),
            Call(sender, value = BigInteger.valueOf(7))
        )
        val expected = "0x34fcd5be" +
                "0000000000000000000000000000000000000000000000000000000000000020" +
                "0000000000000000000000000000000000000000000000000000000000000002" +
                "0000000000000000000000000000000000000000000000000000000000000040" +
                "0000000000000000000000000000000000000000000000000000000000000120" +
                "0000000000000000000000001c7d4b196cb0c7b01d743fbc6116a902379c7238" +
                "0000000000000000000000000000000000000000000000000000000000000000" +
                "0000000000000000000000000000000000000000000000000000000000000060" +
                "0000000000000000000000000000000000000000000000000000000000000044" +
                "095ea7b3000000000000000000000000888888888888ec68a58ab8094cc1ad20" +
                "ba3d240200000000000000000000000000000000000000000000000000000000" +
                "0000000000000000000000000000000000000000000000000000000000000000" +
                "000000000000000000000000058c34d11b52b2d3ab3f2b44a94213584c2dc991" +
                "0000000000000000000000000000000000000000000000000000000000000007" +
                "0000000000000000000000000000000000000000000000000000000000000060" +
                "0000000000000000000000000000000000000000000000000000000000000000"
        assertEquals(expected, Numeric.toHexString(UserOperationEncoder.encodeBatchExecute(calls)))
    }
}
