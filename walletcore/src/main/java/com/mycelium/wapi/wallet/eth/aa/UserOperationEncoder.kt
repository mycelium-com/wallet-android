package com.mycelium.wapi.wallet.eth.aa

import org.web3j.crypto.Hash
import org.web3j.utils.Numeric
import java.math.BigInteger

/**
 * ABI encoding and hashing of ERC-4337 UserOperations (EntryPoint v0.8, EIP-712 userOpHash)
 * and of the ERC-20 calls batched into them.
 */
object UserOperationEncoder {
    private val EXECUTE_BATCH_SELECTOR = selector("executeBatch((address,uint256,bytes)[])")
    private val APPROVE_SELECTOR = selector("approve(address,uint256)")
    private val TRANSFER_SELECTOR = selector("transfer(address,uint256)")

    private val PACKED_USEROP_TYPEHASH: ByteArray = Hash.sha3(
        ("PackedUserOperation(address sender,uint256 nonce,bytes initCode,bytes callData," +
                "bytes32 accountGasLimits,uint256 preVerificationGas,bytes32 gasFees,bytes paymasterAndData)").toByteArray()
    )
    private val DOMAIN_TYPEHASH: ByteArray = Hash.sha3(
        "EIP712Domain(string name,string version,uint256 chainId,address verifyingContract)".toByteArray()
    )

    private fun selector(signature: String) = Hash.sha3(signature.toByteArray()).copyOfRange(0, 4)

    /** `executeBatch((address,uint256,bytes)[])` of Simple7702Account. */
    fun encodeBatchExecute(calls: List<Call>): ByteArray {
        val tuples = calls.map { call ->
            encodeAddress(call.to) + uint256(call.value) + uint256(BigInteger.valueOf(96)) + encodeDynamicBytes(call.data)
        }
        var array = uint256(BigInteger.valueOf(calls.size.toLong()))
        var offset = calls.size * 32L
        for (tuple in tuples) {
            array += uint256(BigInteger.valueOf(offset))
            offset += tuple.size
        }
        tuples.forEach { array += it }
        return EXECUTE_BATCH_SELECTOR + uint256(BigInteger.valueOf(32)) + array
    }

    fun encodeERC20Approve(spender: String, amount: BigInteger): ByteArray =
        APPROVE_SELECTOR + encodeAddress(spender) + uint256(amount)

    fun encodeERC20Transfer(to: String, amount: BigInteger): ByteArray =
        TRANSFER_SELECTOR + encodeAddress(to) + uint256(amount)

    /**
     * userOpHash per EntryPoint v0.8 (EIP-712, domain name "ERC4337" version "1"):
     * keccak256(0x1901 || domainSeparator || structHash).
     */
    fun getUserOpHash(userOp: UserOperation, entryPointAddress: String, chainId: Long): ByteArray {
        val accountGasLimits = Numeric.toBytesPadded(userOp.verificationGasLimit, 16) +
                Numeric.toBytesPadded(userOp.callGasLimit, 16)
        val gasFees = Numeric.toBytesPadded(userOp.maxPriorityFeePerGas, 16) +
                Numeric.toBytesPadded(userOp.maxFeePerGas, 16)
        val structHash = Hash.sha3(PACKED_USEROP_TYPEHASH +
                encodeAddress(userOp.sender) +
                uint256(userOp.nonce) +
                Hash.sha3(ByteArray(0)) + // initCode: EIP-7702 accounts are never deployed by a factory
                Hash.sha3(userOp.callData) +
                accountGasLimits +
                uint256(userOp.preVerificationGas) +
                gasFees +
                Hash.sha3(paymasterAndData(userOp)))
        val domainSeparator = Hash.sha3(DOMAIN_TYPEHASH +
                Hash.sha3("ERC4337".toByteArray()) +
                Hash.sha3("1".toByteArray()) +
                uint256(BigInteger.valueOf(chainId)) +
                encodeAddress(entryPointAddress))
        return Hash.sha3(byteArrayOf(0x19, 0x01) + domainSeparator + structHash)
    }

    /** paymaster (20) || verificationGasLimit (16) || postOpGasLimit (16) || paymasterData. */
    private fun paymasterAndData(userOp: UserOperation): ByteArray {
        val paymaster = userOp.paymaster ?: return ByteArray(0)
        return addressBytes(paymaster) +
                Numeric.toBytesPadded(userOp.paymasterVerificationGasLimit ?: BigInteger.ZERO, 16) +
                Numeric.toBytesPadded(userOp.paymasterPostOpGasLimit ?: BigInteger.ZERO, 16) +
                (userOp.paymasterData ?: ByteArray(0))
    }

    /** RPC representation: numbers as 0x-hex quantities, bytes as 0x-hex data. */
    fun toRpcMap(userOp: UserOperation): LinkedHashMap<String, Any> = linkedMapOf<String, Any>().apply {
        put("sender", userOp.sender)
        put("nonce", hex(userOp.nonce))
        put("callData", Numeric.toHexString(userOp.callData))
        put("callGasLimit", hex(userOp.callGasLimit))
        put("verificationGasLimit", hex(userOp.verificationGasLimit))
        put("preVerificationGas", hex(userOp.preVerificationGas))
        put("maxFeePerGas", hex(userOp.maxFeePerGas))
        put("maxPriorityFeePerGas", hex(userOp.maxPriorityFeePerGas))
        userOp.paymaster?.let { paymaster ->
            put("paymaster", paymaster)
            put("paymasterVerificationGasLimit", hex(userOp.paymasterVerificationGasLimit ?: BigInteger.ZERO))
            put("paymasterPostOpGasLimit", hex(userOp.paymasterPostOpGasLimit ?: BigInteger.ZERO))
            put("paymasterData", Numeric.toHexString(userOp.paymasterData ?: ByteArray(0)))
        }
        put("signature", Numeric.toHexString(userOp.signature))
    }

    fun hex(value: BigInteger): String = Numeric.toHexStringWithPrefix(value)

    private fun encodeAddress(address: String) = ByteArray(12) + addressBytes(address)

    private fun addressBytes(address: String): ByteArray {
        val clean = Numeric.cleanHexPrefix(address)
        require(clean.length == 40) { "Invalid address: $address" }
        return Numeric.hexStringToByteArray(clean)
    }

    private fun uint256(value: BigInteger) = Numeric.toBytesPadded(value, 32)

    private fun encodeDynamicBytes(data: ByteArray): ByteArray {
        val padded = data.copyOf((data.size + 31) / 32 * 32)
        return uint256(BigInteger.valueOf(data.size.toLong())) + padded
    }
}
