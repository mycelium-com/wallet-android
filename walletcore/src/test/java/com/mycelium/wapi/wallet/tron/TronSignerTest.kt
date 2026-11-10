package com.mycelium.wapi.wallet.tron

import com.mrd.bitlib.util.HashUtils
import com.mrd.bitlib.util.HexUtils
import com.mycelium.wapi.wallet.tron.coins.TronMain
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.web3j.crypto.Credentials
import java.math.BigInteger

/**
 * Vectors from TrustWallet Core `tests/chains/Tron/{Address,Signer}Tests.cpp` — the reference
 * implementation this wallet used to sign with. Matching ids proves our hand-rolled protobuf
 * framing and TAPOS derivation are byte-identical; matching signatures prove the RFC6979
 * secp256k1 signing and recovery-id encoding agree.
 */
class TronSignerTest {
    private val privateKey = "2d8f68944bdbfbc0769542fba8fc2d2a3de67393334471624364c7006da2aa54"
    private val credentials = Credentials.create(privateKey)

    // BlockHeader from the wallet-core signer tests
    private val blockRef = blockRef(
        timestamp = 1539295479000,
        number = 3111739,
        version = 3,
        txTrieRoot = "64288c2db0641316762a99dbb02ef7c90f968b60f9f2e410835980614332f86d",
        parentHash = "00000000002f7b3af4f5f8b9e23a30c530f719f165b742e7358536b280eead2d",
        witnessAddress = "415863f6091b8e71766da808b1dd3159790f61de7d",
    )
    private val txTime = TronTxTime(1539295479000, 1539295479000 + 10 * 60 * 60 * 1000)

    @Test
    fun addressDerivation() {
        assertEquals("TJRyWwFs9wTFGZg3JbrVriFbNfCug5tDeC", TronKeys.addressOf(credentials.ecKeyPair))
        assertEquals("THRF3GuPnvvPzKoaT8pJex5XHmo8NNbCb3",
            TronKeys.addressOf(Credentials.create("BE88DF1D0BF30A923CB39C3BB953178BAAF3726E8D3CE81E7C8462E046E0D835").ecKeyPair))
    }

    @Test
    fun addressValidation() {
        assertTrue(TronAddress.isValid("TJRyWwFs9wTFGZg3JbrVriFbNfCug5tDeC"))
        assertFalse(TronAddress.isValid("TJRyWwFs9wTFGZg3JbrVriFbNfCug5tDeD"))
        assertFalse(TronAddress.isValid("0x5aAeb6053F3E94C9b9A09f33669435E7Ef1BeAed"))
        assertFalse(TronAddress.isValid("175tWpb8K1S7NmH4Zx6rewF9WQrcZv245W"))
        assertFalse(TronAddress.isValid("abc"))
        assertNull(TronMain.parseAddress("2MegQ6oqSda2tTagdEzBA"))
        val addr = TronAddress(TronMain, "TJRyWwFs9wTFGZg3JbrVriFbNfCug5tDeC")
        assertEquals("TJRyWwFs9wTFGZg3JbrVriFbNfCug5tDeC", TronAddress.fromHex(addr.hexString))
        assertEquals(21, addr.getBytes().size)
        assertEquals(0x41.toByte(), addr.getBytes()[0])
    }

    @Test
    fun signTransfer() {
        val owner = TronAddress(TronMain, "TJRyWwFs9wTFGZg3JbrVriFbNfCug5tDeC")
        val to = TronAddress(TronMain, "THTR75o8xXAgCTQqpiot2AFRAjvW1tSbVV")
        val raw = TronTransactionBuilder.buildTransfer(blockRef, owner.getBytes(), to.getBytes(), 2000000, txTime)
        assertEquals("dc6f6d9325ee44ab3c00528472be16e1572ab076aa161ccd12515029869d0451", raw.txIdHex)
        val signature = TronKeys.sign(raw.txId, credentials.ecKeyPair)
        assertEquals(
            "ede769f6df28aefe6a846be169958c155e23e7e5c9621d2e8dce1719b4d952b63e8a8bf9f00e41204ac1bf69b1a663dacdf764367e48e4a5afcd6b055a747fb200",
            HexUtils.toHex(signature)
        )
        // Transaction { raw_data = 1, signature = 2 }
        val signed = TronTransactionBuilder.encodeSigned(raw.rawData, signature)
        assertEquals(0x0a, signed[0].toInt())
        assertTrue(HexUtils.toHex(signed).endsWith("1241" + HexUtils.toHex(signature)))
    }

    @Test
    fun signTrc20Transfer() {
        val owner = TronAddress(TronMain, "TJRyWwFs9wTFGZg3JbrVriFbNfCug5tDeC")
        val contract = TronAddress(TronMain, "THTR75o8xXAgCTQqpiot2AFRAjvW1tSbVV")
        val to = TronAddress(TronMain, "TW1dU4L3eNm7Lw8WvieLKEHpXWAussRG9Z")
        // wallet-core pads the full 21-byte (0x41-prefixed) address; the TVM reads the low 20
        // bytes either way. Reproduce their exact call data to compare ids.
        val callData = HexUtils.toBytes("a9059cbb" + HexUtils.toHex(to.getBytes()).padStart(64, '0') +
                BigInteger.valueOf(1000).toString(16).padStart(64, '0'))
        // No fee_limit in the wallet-core vector: build the raw tx via the transfer path with
        // fee_limit forced to 0 by using the internal writer contract shape.
        val raw = buildTriggerWithoutFeeLimit(owner.getBytes(), contract.getBytes(), callData)
        assertEquals("0d644290e3cf554f6219c7747f5287589b6e7e30e1b02793b48ba362da6a5058", raw.txIdHex)
        val signature = TronKeys.sign(raw.txId, credentials.ecKeyPair)
        assertEquals(
            "bec790877b3a008640781e3948b070740b1f6023c29ecb3f7b5835433c13fc5835e5cad3bd44360ff2ddad5ed7dc9d7dee6878f90e86a40355b7697f5954b88c01",
            HexUtils.toHex(signature)
        )
    }

    @Test
    fun trc20CallDataUsesStandardAbiAddress() {
        val to = TronAddress(TronMain, "TW1dU4L3eNm7Lw8WvieLKEHpXWAussRG9Z")
        val data = TronTransactionBuilder.trc20TransferData(to, BigInteger.valueOf(1000))
        assertEquals(68, data.size)
        val hex = HexUtils.toHex(data)
        assertTrue(hex.startsWith("a9059cbb" + "000000000000000000000000" + HexUtils.toHex(to.evmBytes)))
        assertTrue(hex.endsWith("3e8"))
    }

    @Test
    fun feeLimitIsEncodedInRawData() {
        val owner = TronAddress(TronMain, "TJRyWwFs9wTFGZg3JbrVriFbNfCug5tDeC")
        val contract = TronAddress(TronMain, "THTR75o8xXAgCTQqpiot2AFRAjvW1tSbVV")
        val to = TronAddress(TronMain, "TW1dU4L3eNm7Lw8WvieLKEHpXWAussRG9Z")
        val callData = TronTransactionBuilder.trc20TransferData(to, BigInteger.valueOf(1000))
        val raw = TronTransactionBuilder.buildTriggerSmartContract(blockRef, owner.getBytes(), contract.getBytes(),
            callData, 15_000_000, 0, txTime)
        // field 18 varint key = (18 << 3) | 0 = 0x90 0x01, followed by varint(15_000_000)
        val feeLimitVarint = ProtoWriter().apply { varintField(18, 15_000_000) }.toByteArray()
        assertTrue(HexUtils.toHex(raw.rawData).endsWith(HexUtils.toHex(feeLimitVarint)))
        assertArrayEquals(HashUtils.sha256(raw.rawData).bytes, raw.txId)
    }

    @Test
    fun signingOutputCarriesTronJson() {
        val owner = TronAddress(TronMain, "TJRyWwFs9wTFGZg3JbrVriFbNfCug5tDeC")
        val to = TronAddress(TronMain, "THTR75o8xXAgCTQqpiot2AFRAjvW1tSbVV")
        val raw = TronTransactionBuilder.buildTransfer(blockRef, owner.getBytes(), to.getBytes(), 2000000, txTime)
        val signature = TronKeys.sign(raw.txId, credentials.ecKeyPair)
        val out = TronTransactionBuilder.encodeSigningOutput(raw, signature, blockRef)
        val hex = HexUtils.toHex(out)
        // field 1 = id (32 bytes), field 2 = signature (65 bytes), 3/4 = ref block, 5 = json
        assertTrue(hex.startsWith("0a20" + raw.txIdHex + "1241" + HexUtils.toHex(signature) + "1a02" + "7b3b" + "2208"))
        val json = com.fasterxml.jackson.databind.ObjectMapper().readTree(TronTransactionBuilder.signedTransactionJson(raw, signature))
        assertEquals(raw.txIdHex, json.path("txID").asText())
        assertEquals(raw.rawDataHex, json.path("raw_data_hex").asText())
        assertEquals(HexUtils.toHex(signature), json.path("signature").get(0).asText())
        val contract = json.path("raw_data").path("contract").get(0)
        assertEquals("TransferContract", contract.path("type").asText())
        assertEquals("type.googleapis.com/protocol.TransferContract", contract.path("parameter").path("type_url").asText())
        assertEquals(owner.hexString, contract.path("parameter").path("value").path("owner_address").asText())
        assertEquals(2000000L, contract.path("parameter").path("value").path("amount").asLong())
        assertEquals("7b3b", json.path("raw_data").path("ref_block_bytes").asText())
        assertEquals(txTime.expiration, json.path("raw_data").path("expiration").asLong())
        assertTrue(json.path("raw_data").path("fee_limit").isMissingNode)
    }

    @Test
    fun refBlockFields() {
        assertEquals("7b3b", HexUtils.toHex(TronTransactionBuilder.refBlockBytes(3111739)))
        assertEquals("0000", HexUtils.toHex(TronTransactionBuilder.refBlockBytes(0x10000L)))
        assertEquals("ffff", HexUtils.toHex(TronTransactionBuilder.refBlockBytes(0xFFFFL)))
        assertEquals(8, TronTransactionBuilder.refBlockHash(blockRef).size)
    }

    @Test
    fun protoWriterVarints() {
        assertEquals("", HexUtils.toHex(ProtoWriter().apply { varintField(1, 0) }.toByteArray()))
        assertEquals("0801", HexUtils.toHex(ProtoWriter().apply { varintField(1, 1) }.toByteArray()))
        assertEquals("08ac02", HexUtils.toHex(ProtoWriter().apply { varintField(1, 300) }.toByteArray()))
        // negative longs take 10 bytes
        assertEquals(11, ProtoWriter().apply { varintField(1, -1) }.toByteArray().size)
        assertEquals("1203616263", HexUtils.toHex(ProtoWriter().apply { stringField(2, "abc") }.toByteArray()))
        // empty message fields are still written
        assertEquals("0a00", HexUtils.toHex(ProtoWriter().apply { messageField(1, ByteArray(0)) }.toByteArray()))
    }

    @Test
    fun messageSigningIsRecoverable() {
        val sig = TronKeys.signMessage("hello", credentials.ecKeyPair)
        assertEquals(130, sig.length)
        assertTrue(sig.endsWith("1b") || sig.endsWith("1c"))
    }

    /** TW builds the vector without fee_limit — mirror buildTriggerSmartContract with fee_limit = 0. */
    private fun buildTriggerWithoutFeeLimit(owner: ByteArray, contract: ByteArray, callData: ByteArray): TronRawTransaction {
        val body = ProtoWriter().apply {
            bytesField(1, owner)
            bytesField(2, contract)
            bytesField(4, callData)
        }.toByteArray()
        val parameter = ProtoWriter().apply {
            stringField(1, "type.googleapis.com/protocol.TriggerSmartContract")
            bytesField(2, body)
        }.toByteArray()
        val contractMsg = ProtoWriter().apply {
            varintField(1, TronTransactionBuilder.CONTRACT_TYPE_TRIGGER_SMART_CONTRACT.toLong())
            messageField(2, parameter)
        }.toByteArray()
        val rawData = ProtoWriter().apply {
            bytesField(1, TronTransactionBuilder.refBlockBytes(blockRef.number))
            bytesField(4, TronTransactionBuilder.refBlockHash(blockRef))
            varintField(8, txTime.expiration)
            messageField(11, contractMsg)
            varintField(14, txTime.timestamp)
        }.toByteArray()
        return TronRawTransaction(rawData, HashUtils.sha256(rawData).bytes, "{}")
    }

    /**
     * Block id as java-tron computes it: sha256(BlockHeader.raw) — bytes [8,16) are what TAPOS
     * uses, so overwriting the first 8 bytes with the number is not needed here.
     * BlockHeader.raw { timestamp = 1, txTrieRoot = 2, parentHash = 3, number = 7, witness_address = 9, version = 10 }
     */
    private fun blockRef(timestamp: Long, number: Long, version: Long, txTrieRoot: String, parentHash: String,
                         witnessAddress: String): TronBlockRef {
        val headerRaw = ProtoWriter().apply {
            varintField(1, timestamp)
            bytesField(2, HexUtils.toBytes(txTrieRoot))
            bytesField(3, HexUtils.toBytes(parentHash))
            varintField(7, number)
            bytesField(9, HexUtils.toBytes(witnessAddress))
            varintField(10, version)
        }.toByteArray()
        return TronBlockRef(number, HexUtils.toHex(HashUtils.sha256(headerRaw).bytes), timestamp)
    }
}
