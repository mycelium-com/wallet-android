package com.mycelium.wapi.wallet.eth

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.Assert.*
import org.junit.Test
import java.math.BigInteger

class TxTest {
    private val mapper = ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    private val ownerAddress = "0xaabbccddee1122334455aabbccddee1122334455"
    private val recipientAddress = "0x1111111111222222222233333333334444444444"
    private val contractAddress = "0xdac17f958d2ee523a2206206994597c13d831ec7"

    // ERC20 transfer(address,uint256) selector = a9059cbb
    // address padded to 32 bytes: 0000000000000000000000001111111111222222222233333333334444444444
    // uint256 50000000 (50 USDT) = 0000000000000000000000000000000000000000000000000000000002faf080
    private val transferInputData = "0xa9059cbb" +
            "000000000000000000000000" + recipientAddress.removePrefix("0x") +
            "0000000000000000000000000000000000000000000000000000000002faf080"

    private fun buildTxJson(
        from: String = ownerAddress,
        to: String = contractAddress,
        confirmations: Int = 0,
        tokenTransfers: String = "[]",
        inputData: String? = transferInputData
    ): String {
        val ethereumSpecific = if (inputData != null) {
            """{"nonce": "140", "gasLimit": "90000", "gasUsed": "0", "gasPrice": "35000000000", "status": true, "data": "$inputData"}"""
        } else {
            """{"nonce": "140", "gasLimit": "90000", "gasUsed": "55500", "gasPrice": "35000000000", "status": true}"""
        }
        return """{
            "txid": "0xdeadbeef",
            "vin": [{"addresses": ["$from"]}],
            "vout": [{"addresses": ["$to"]}],
            "blockHeight": 0,
            "confirmations": $confirmations,
            "blockTime": 1713200000,
            "value": "0",
            "fees": "0",
            "ethereumSpecific": $ethereumSpecific,
            "tokenTransfers": $tokenTransfers
        }"""
    }

    private fun type2TxJson(confirmations: Int) = """{
            "txid": "0xdeadbeef", "vin": [{"addresses": ["$ownerAddress"]}], "vout": [{"addresses": ["$recipientAddress"]}],
            "blockHeight": 0, "confirmations": $confirmations, "blockTime": 1713200000, "value": "0", "fees": "0",
            "ethereumSpecific": {"nonce": "7", "gasLimit": "21000", "gasUsed": "21000", "gasPrice": "4338980918",
                "maxFeePerGas": "8000000000", "maxPriorityFeePerGas": "2000000000", "status": true}
        }"""

    @Test
    fun `pending type-2 tx reports its cap as gas price`() {
        // blockbook's gasPrice of a pending type-2 tx is the effective price; replacements must bump the cap
        val tx = mapper.readValue(type2TxJson(confirmations = 0), Tx::class.java)
        assertEquals(BigInteger("8000000000"), tx.gasPrice)
    }

    @Test
    fun `mined type-2 tx reports the effective gas price`() {
        val tx = mapper.readValue(type2TxJson(confirmations = 3), Tx::class.java)
        assertEquals(BigInteger("4338980918"), tx.gasPrice)
    }

    @Test
    fun `legacy tx without cap keeps gas price`() {
        val tx = mapper.readValue(buildTxJson(), Tx::class.java)
        assertEquals(BigInteger("35000000000"), tx.gasPrice)
    }

    @Test
    fun `getPendingTokenTransfer decodes ERC20 transfer from input data`() {
        val tx = mapper.readValue(buildTxJson(), Tx::class.java)
        val transfer = tx.getPendingTokenTransfer(contractAddress, ownerAddress)
        assertNotNull(transfer)
        assertEquals(ownerAddress, transfer!!.from)
        assertEquals(recipientAddress, transfer.to)
        assertEquals(BigInteger("50000000"), transfer.value)
    }

    @Test
    fun `getPendingTokenTransfer returns null for non-transfer selector`() {
        val badData = "0x12345678" + "0".repeat(128)
        val tx = mapper.readValue(buildTxJson(inputData = badData), Tx::class.java)
        val transfer = tx.getPendingTokenTransfer(contractAddress, ownerAddress)
        assertNull(transfer)
    }

    @Test
    fun `getPendingTokenTransfer returns null when no data`() {
        val tx = mapper.readValue(buildTxJson(inputData = null), Tx::class.java)
        val transfer = tx.getPendingTokenTransfer(contractAddress, ownerAddress)
        assertNull(transfer)
    }

    @Test
    fun `getPendingTokenTransfer decodes regardless of outer to field`() {
        // Blockbook synthesizes vout[0] as token recipient (not contract) for
        // ERC20 transfers, so the function must not gate on tx.to matching the
        // contract address. Caller scopes to the relevant contract.
        val tx = mapper.readValue(buildTxJson(to = recipientAddress), Tx::class.java)
        val transfer = tx.getPendingTokenTransfer(contractAddress, ownerAddress)
        assertNotNull(transfer)
        assertEquals(BigInteger("50000000"), transfer!!.value)
    }

    @Test
    fun `getPendingTokenTransfer returns null when owner not involved`() {
        val tx = mapper.readValue(buildTxJson(from = "0xsomeoneelse000000000000000000000000000000"), Tx::class.java)
        val transfer = tx.getPendingTokenTransfer(contractAddress, ownerAddress)
        assertNull(transfer)
    }

    @Test
    fun `getPendingTokenTransfer prefers tokenTransfers when present`() {
        val tokenTransfersJson = """[{
            "from": "$ownerAddress",
            "to": "$recipientAddress",
            "contract": "$contractAddress",
            "token": "",
            "name": "USDT",
            "value": "99000000"
        }]"""
        val tx = mapper.readValue(buildTxJson(tokenTransfers = tokenTransfersJson), Tx::class.java)
        val transfer = tx.getPendingTokenTransfer(contractAddress, ownerAddress)
        assertNotNull(transfer)
        assertEquals(BigInteger("99000000"), transfer!!.value)
    }

    @Test
    fun `getTokenTransfer sums multiple transfers to same owner`() {
        val tokenTransfersJson = """[
            {"from": "$ownerAddress", "to": "$recipientAddress", "contract": "$contractAddress", "token": "", "name": "USDT", "value": "10000000"},
            {"from": "$ownerAddress", "to": "$recipientAddress", "contract": "$contractAddress", "token": "", "name": "USDT", "value": "20000000"}
        ]"""
        val tx = mapper.readValue(buildTxJson(tokenTransfers = tokenTransfersJson), Tx::class.java)
        val transfer = tx.getTokenTransfer(contractAddress, ownerAddress)
        assertNotNull(transfer)
        assertEquals(BigInteger("30000000"), transfer!!.value)
    }

    @Test
    fun `getTokenTransfer nets opposing legs of the same token`() {
        // A swap-shaped tx: the owner sends 30 and gets 10 of the same token back.
        // Summing reported 40 moving in the direction of the first leg.
        val tokenTransfersJson = """[
            {"from": "$ownerAddress", "to": "$recipientAddress", "contract": "$contractAddress", "token": "", "name": "USDT", "value": "30000000"},
            {"from": "$recipientAddress", "to": "$ownerAddress", "contract": "$contractAddress", "token": "", "name": "USDT", "value": "10000000"}
        ]"""
        val tx = mapper.readValue(buildTxJson(tokenTransfers = tokenTransfersJson), Tx::class.java)
        val transfer = tx.getTokenTransfer(contractAddress, ownerAddress)
        assertNotNull(transfer)
        assertEquals(BigInteger("20000000"), transfer!!.value)
        // net is outgoing, so the owner must be the sender and the counterparty the recipient
        assertEquals(ownerAddress, transfer.from)
        assertEquals(recipientAddress, transfer.to)
    }

    @Test
    fun `getTokenTransfer reports net incoming when the owner receives more than it sends`() {
        val tokenTransfersJson = """[
            {"from": "$ownerAddress", "to": "$recipientAddress", "contract": "$contractAddress", "token": "", "name": "USDT", "value": "10000000"},
            {"from": "$recipientAddress", "to": "$ownerAddress", "contract": "$contractAddress", "token": "", "name": "USDT", "value": "25000000"}
        ]"""
        val tx = mapper.readValue(buildTxJson(tokenTransfers = tokenTransfersJson), Tx::class.java)
        val transfer = tx.getTokenTransfer(contractAddress, ownerAddress)
        assertNotNull(transfer)
        assertEquals(BigInteger("15000000"), transfer!!.value)
        assertEquals(recipientAddress, transfer.from)
        assertEquals(ownerAddress, transfer.to)
    }

    @Test
    fun `failureReason surfaces the servers revert message`() {
        val json = buildTxJson(confirmations = 12, inputData = null)
            .replace(""""status": true""", """"status": false, "error": "Out of gas."""")
        val tx = mapper.readValue(json, Tx::class.java)
        assertFalse(tx.success)
        assertEquals("Out of gas.", tx.failureReason)
    }

    @Test
    fun `failureReason is null for a successful tx`() {
        val tx = mapper.readValue(buildTxJson(confirmations = 12, inputData = null), Tx::class.java)
        assertTrue(tx.success)
        assertNull(tx.failureReason)
    }

    @Test
    fun `getPendingTokenTransfer handles incoming pending token transfer`() {
        val tx = mapper.readValue(buildTxJson(from = "0xsender0000000000000000000000000000000000"), Tx::class.java)
        // recipientAddress (decoded from inputData) != ownerAddress, from != ownerAddress
        val transfer = tx.getPendingTokenTransfer(contractAddress, ownerAddress)
        assertNull(transfer)
    }
}
