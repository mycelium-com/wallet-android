package com.mycelium.wapi.wallet.tron

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.mrd.bitlib.util.HashUtils
import com.mrd.bitlib.util.HexUtils
import java.math.BigInteger

/**
 * Reference block for TAPOS. [blockIdHex] is the 32-byte block id as returned by
 * `wallet/getnowblock` (`blockID`); its first 8 bytes are the block number and bytes [8,16)
 * are what `raw_data.ref_block_hash` carries.
 */
data class TronBlockRef(val number: Long, val blockIdHex: String, val timestampMs: Long)

/** `raw_data.timestamp` / `raw_data.expiration`, epoch milliseconds. */
data class TronTxTime(val timestamp: Long, val expiration: Long)

/**
 * A locally built, not-yet-signed TRON transaction. [txId] = sha256(rawData) is computed
 * here — it is what gets signed, so no remote party contributes to the signed payload.
 */
class TronRawTransaction(val rawData: ByteArray, val txId: ByteArray, val rawDataJson: String) {
    val rawDataHex: String get() = HexUtils.toHex(rawData)
    val txIdHex: String get() = HexUtils.toHex(txId)
}

/**
 * Builds and hashes `Transaction.raw` locally, following the official `core/Tron.proto`
 * field numbers, and wraps a signature into the final `Transaction` protobuf that
 * `wallet/broadcasthex` accepts. Only the contract shapes this wallet signs are implemented.
 */
object TronTransactionBuilder {
    // Transaction.Contract.ContractType
    const val CONTRACT_TYPE_TRANSFER = 1
    const val CONTRACT_TYPE_TRIGGER_SMART_CONTRACT = 31

    private const val ANY_TYPE_URL_PREFIX = "type.googleapis.com/protocol."

    /** How long a signed transfer stays broadcastable (node ceiling is 24h). */
    const val DEFAULT_EXPIRATION_MS = 10 * 60_000L

    /** TransferContract { owner_address = 1, to_address = 2, amount = 3 } */
    fun buildTransfer(
        blockRef: TronBlockRef,
        owner: ByteArray,
        to: ByteArray,
        amountSun: Long,
        time: TronTxTime = txTime(),
    ): TronRawTransaction {
        require(amountSun > 0) { "TRX amount must be positive" }
        val contract = ProtoWriter().apply {
            bytesField(1, owner)
            bytesField(2, to)
            varintField(3, amountSun)
        }.toByteArray()
        val json = obj {
            put("owner_address", HexUtils.toHex(owner))
            put("to_address", HexUtils.toHex(to))
            put("amount", amountSun)
        }
        return buildRawTransaction(blockRef, CONTRACT_TYPE_TRANSFER, "TransferContract", contract, json, time, 0)
    }

    /**
     * TriggerSmartContract { owner_address = 1, contract_address = 2, call_value = 3, data = 4 }
     * with `raw.fee_limit` (field 18) — required for smart contract execution.
     */
    fun buildTriggerSmartContract(
        blockRef: TronBlockRef,
        owner: ByteArray,
        contract: ByteArray,
        callData: ByteArray,
        feeLimitSun: Long,
        callValueSun: Long = 0,
        time: TronTxTime = txTime(),
    ): TronRawTransaction {
        require(feeLimitSun > 0) { "fee_limit must be positive for smart contract calls" }
        val body = ProtoWriter().apply {
            bytesField(1, owner)
            bytesField(2, contract)
            varintField(3, callValueSun)
            bytesField(4, callData)
        }.toByteArray()
        val json = obj {
            put("owner_address", HexUtils.toHex(owner))
            put("contract_address", HexUtils.toHex(contract))
            put("data", HexUtils.toHex(callData))
            if (callValueSun > 0) put("call_value", callValueSun)
        }
        return buildRawTransaction(
            blockRef, CONTRACT_TYPE_TRIGGER_SMART_CONTRACT, "TriggerSmartContract", body, json, time, feeLimitSun
        )
    }

    /** ABI data for TRC20 `transfer(address,uint256)`: a9059cbb + 32-byte address + 32-byte amount. */
    fun trc20TransferData(to: TronAddress, amount: BigInteger): ByteArray {
        require(amount.signum() > 0) { "Token amount must be positive" }
        val amountHex = amount.toString(16).padStart(64, '0')
        require(amountHex.length == 64) { "Token amount does not fit into uint256" }
        return HexUtils.toBytes(TRC20_TRANSFER_SELECTOR + HexUtils.toHex(to.evmBytes).padStart(64, '0') + amountHex)
    }

    /** Transaction { raw_data = 1, signature = 2 } — the payload for `wallet/broadcasthex`. */
    fun encodeSigned(rawData: ByteArray, signature: ByteArray): ByteArray = ProtoWriter().apply {
        messageField(1, rawData)
        bytesField(2, signature)
    }.toByteArray()

    fun txTime(expirationMs: Long = DEFAULT_EXPIRATION_MS, nowMs: Long = System.currentTimeMillis()): TronTxTime {
        require(expirationMs > 0) { "expiration must be positive" }
        return TronTxTime(timestamp = nowMs, expiration = nowMs + expirationMs)
    }

    private fun buildRawTransaction(
        blockRef: TronBlockRef,
        contractType: Int,
        contractTypeName: String,
        contractBody: ByteArray,
        contractJson: ObjectNode,
        time: TronTxTime,
        feeLimit: Long,
    ): TronRawTransaction {
        // google.protobuf.Any { type_url = 1, value = 2 }
        val parameter = ProtoWriter().apply {
            stringField(1, ANY_TYPE_URL_PREFIX + contractTypeName)
            bytesField(2, contractBody)
        }.toByteArray()

        // Transaction.Contract { type = 1 (enum), parameter = 2 }
        val contract = ProtoWriter().apply {
            varintField(1, contractType.toLong())
            messageField(2, parameter)
        }.toByteArray()

        // Transaction.raw { ref_block_bytes = 1, ref_block_hash = 4, expiration = 8,
        //                   contract = 11, timestamp = 14, fee_limit = 18 }
        val refBlockBytes = refBlockBytes(blockRef.number)
        val refBlockHash = refBlockHash(blockRef)
        val rawData = ProtoWriter().apply {
            bytesField(1, refBlockBytes)
            bytesField(4, refBlockHash)
            varintField(8, time.expiration)
            messageField(11, contract)
            varintField(14, time.timestamp)
            varintField(18, feeLimit)
        }.toByteArray()

        // java-tron style JSON of raw_data (hex addresses, as TrustWallet Core / `visible=false` produce it)
        val json = obj {
            put("ref_block_bytes", HexUtils.toHex(refBlockBytes))
            put("ref_block_hash", HexUtils.toHex(refBlockHash))
            if (feeLimit > 0) put("fee_limit", feeLimit)
            put("timestamp", time.timestamp)
            put("expiration", time.expiration)
            putArray("contract").add(obj {
                put("type", contractTypeName)
                set<ObjectNode>("parameter", obj {
                    put("type_url", ANY_TYPE_URL_PREFIX + contractTypeName)
                    set<ObjectNode>("value", contractJson)
                })
            })
        }
        return TronRawTransaction(rawData, HashUtils.sha256(rawData).bytes, json.toString())
    }

    /**
     * Full transaction JSON as `wallet/broadcasttransaction` accepts it (and as TrustWallet Core's
     * `SigningOutput.json` carries it): `txID`, `raw_data`, `raw_data_hex`, `signature[]`.
     */
    fun signedTransactionJson(raw: TronRawTransaction, signature: ByteArray): String = obj {
        put("txID", raw.txIdHex)
        set<ObjectNode>("raw_data", mapper.readTree(raw.rawDataJson) as ObjectNode)
        put("raw_data_hex", raw.rawDataHex)
        putArray("signature").add(HexUtils.toHex(signature))
    }.toString()

    /**
     * TrustWallet Core `TW.Tron.Proto.SigningOutput` protobuf — the wire format the energy
     * service partner API expects for `signed_tx_*` (the KMP wallet passed `AnySigner.sign()`
     * output through verbatim): id = 1, signature = 2, ref_block_bytes = 3, ref_block_hash = 4,
     * json = 5.
     */
    fun encodeSigningOutput(raw: TronRawTransaction, signature: ByteArray, blockRef: TronBlockRef): ByteArray =
        ProtoWriter().apply {
            bytesField(1, raw.txId)
            bytesField(2, signature)
            bytesField(3, refBlockBytes(blockRef.number))
            bytesField(4, refBlockHash(blockRef))
            stringField(5, signedTransactionJson(raw, signature))
        }.toByteArray()

    private val mapper = ObjectMapper()
    private inline fun obj(fill: ObjectNode.() -> Unit): ObjectNode = mapper.createObjectNode().apply(fill)

    /** Low 16 bits of the block number, big-endian. */
    internal fun refBlockBytes(blockNumber: Long): ByteArray {
        require(blockNumber >= 0) { "Block number must not be negative: $blockNumber" }
        return byteArrayOf(((blockNumber shr 8) and 0xFF).toByte(), (blockNumber and 0xFF).toByte())
    }

    /** Bytes [8,16) of the block id. */
    internal fun refBlockHash(blockRef: TronBlockRef): ByteArray {
        val id = HexUtils.toBytes(blockRef.blockIdHex.removePrefix("0x"))
        require(id.size == 32) { "Block id must be 32 bytes" }
        return id.copyOfRange(8, 16)
    }

    private const val TRC20_TRANSFER_SELECTOR = "a9059cbb"
}

/**
 * Minimal protobuf writer: varint and length-delimited fields only, which is all the TRON
 * transaction shapes need. Follows proto3 in omitting zero/empty scalar fields.
 */
internal class ProtoWriter {
    private var buf = ByteArray(64)
    private var size = 0

    fun varintField(field: Int, value: Long) {
        if (value == 0L) return
        writeKey(field, WIRE_VARINT)
        writeVarint(value)
    }

    fun bytesField(field: Int, value: ByteArray) {
        if (value.isEmpty()) return
        writeLengthDelimited(field, value)
    }

    fun stringField(field: Int, value: String) {
        if (value.isEmpty()) return
        writeLengthDelimited(field, value.toByteArray(Charsets.UTF_8))
    }

    /** Message fields are written even when empty: presence is meaningful for them. */
    fun messageField(field: Int, value: ByteArray) = writeLengthDelimited(field, value)

    fun toByteArray(): ByteArray = buf.copyOf(size)

    private fun writeKey(field: Int, wireType: Int) =
        writeVarint(((field shl 3) or wireType).toLong())

    private fun writeLengthDelimited(field: Int, value: ByteArray) {
        writeKey(field, WIRE_LENGTH_DELIMITED)
        writeVarint(value.size.toLong())
        ensureCapacity(value.size)
        value.copyInto(buf, size)
        size += value.size
    }

    private fun writeVarint(value: Long) {
        ensureCapacity(MAX_VARINT_BYTES)
        var remaining = value
        while (true) {
            val chunk = (remaining and 0x7F).toInt()
            // ushr, not shr: a negative Long must encode as 10 bytes, not spin forever.
            remaining = remaining ushr 7
            if (remaining == 0L) {
                buf[size++] = chunk.toByte()
                return
            }
            buf[size++] = (chunk or 0x80).toByte()
        }
    }

    private fun ensureCapacity(extra: Int) {
        if (size + extra <= buf.size) return
        var capacity = buf.size
        while (capacity < size + extra) capacity *= 2
        buf = buf.copyOf(capacity)
    }

    private companion object {
        const val WIRE_VARINT = 0
        const val WIRE_LENGTH_DELIMITED = 2
        const val MAX_VARINT_BYTES = 10
    }
}
