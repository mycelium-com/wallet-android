package com.mycelium.wapi.wallet.eth

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.mrd.bitlib.util.HexUtils
import com.mycelium.generated.wallet.database.WalletDB
import com.mycelium.wapi.wallet.AesKeyCipher
import com.mycelium.wapi.wallet.BroadcastResult
import com.mycelium.wapi.wallet.BroadcastResultType
import com.mycelium.wapi.wallet.coins.Balance
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.eth.coins.EthTest
import com.mycelium.wapi.wallet.genericdb.EthAccountBacking
import com.mycelium.wapi.wallet.genericdb.accountBackingAdapter
import com.mycelium.wapi.wallet.genericdb.accountContextAdapter
import com.mycelium.wapi.wallet.genericdb.deprecatedAccountAdapter
import com.mycelium.wapi.wallet.genericdb.energyServiceFeeLinkAdapter
import com.mycelium.wapi.wallet.genericdb.erc20ContextAdapter
import com.mycelium.wapi.wallet.genericdb.ethAccountBackingAdapter
import com.mycelium.wapi.wallet.genericdb.ethContextAdapter
import com.mycelium.wapi.wallet.genericdb.feeEstimatorAdapter
import com.mycelium.wapi.wallet.genericdb.trc20ContextAdapter
import com.mycelium.wapi.wallet.genericdb.tronAccountBackingAdapter
import com.mycelium.wapi.wallet.genericdb.tronContextAdapter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.web3j.crypto.Credentials
import java.io.IOException
import java.math.BigInteger
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.UUID

/**
 * A broadcast whose response is lost must never be reported as a plain failure: the user would
 * retry with a fresh tx and pay twice. See [broadcastSignedEthTx] and EthAccount.broadcastTx.
 */
class EthBroadcastRecoveryTest {
    private val noSleep: (Long) -> Unit = {}
    private val ok = EthBlockchainService.SendResult(true, null)
    private fun rejected(msg: String) = EthBlockchainService.SendResult(false, msg)

    private fun broadcast(sends: List<() -> EthBlockchainService.SendResult>,
                          known: () -> Boolean = { throw IOException("404") }): Pair<BroadcastResult, Int> {
        val queue = sends.iterator()
        var calls = 0
        val result = broadcastSignedEthTx("0xabc",
            send = {
                calls++
                queue.next().invoke()
            },
            isKnown = known,
            sleep = noSleep)
        return result to calls
    }

    private val timeout: () -> EthBlockchainService.SendResult = { throw SocketTimeoutException("read timed out") }

    @Test
    fun `accepted first time`() {
        val (result, calls) = broadcast(listOf({ ok }))
        assertEquals(BroadcastResultType.SUCCESS, result.resultType)
        assertEquals(1, calls)
    }

    @Test
    fun `first-send rejection is final, even nonce too low`() {
        val (result, calls) = broadcast(listOf({ rejected("nonce too low") }))
        assertEquals(BroadcastResultType.REJECT_INVALID_TX_PARAMS, result.resultType)
        assertEquals(1, calls)
    }

    @Test
    fun `lost response but the node knows the tx`() {
        val (result, calls) = broadcast(listOf(timeout), known = { true })
        assertEquals(BroadcastResultType.SUCCESS, result.resultType)
        assertEquals(1, calls)
    }

    @Test
    fun `lost response, re-send of the same bytes accepted`() {
        val (result, _) = broadcast(listOf(timeout, { ok }))
        assertEquals(BroadcastResultType.SUCCESS, result.resultType)
    }

    @Test
    fun `lost response, re-send answered already known`() {
        val (result, _) = broadcast(listOf(timeout, { rejected("already known") }))
        assertEquals(BroadcastResultType.SUCCESS, result.resultType)
    }

    @Test
    fun `lost response, tx already mined - re-send answered nonce too low`() {
        val (result, _) = broadcast(listOf(timeout, { rejected("nonce too low: next nonce 6, tx nonce 5") }))
        assertEquals(BroadcastResultType.SUCCESS, result.resultType)
    }

    @Test
    fun `lost response, re-send explicitly rejected and tx unknown`() {
        val (result, _) = broadcast(listOf(timeout, { rejected("insufficient funds for gas * price + value") }))
        assertEquals(BroadcastResultType.REJECT_INVALID_TX_PARAMS, result.resultType)
    }

    @Test
    fun `node unreachable after a possibly delivered send is UNKNOWN_OUTCOME`() {
        val (result, calls) = broadcast(listOf(timeout, timeout, timeout, timeout))
        assertEquals(BroadcastResultType.UNKNOWN_OUTCOME, result.resultType)
        assertEquals(4, calls)
    }

    @Test
    fun `no connection ever established is NO_SERVER_CONNECTION`() {
        val dns: () -> EthBlockchainService.SendResult = { throw UnknownHostException("blockbook") }
        val (result, _) = broadcast(listOf(dns, dns, dns, dns))
        assertEquals(BroadcastResultType.NO_SERVER_CONNECTION, result.resultType)
    }

    @Test
    fun `a re-send that may have gone out turns NO_SERVER_CONNECTION into UNKNOWN_OUTCOME`() {
        val dns: () -> EthBlockchainService.SendResult = { throw UnknownHostException("blockbook") }
        val (result, _) = broadcast(listOf(dns, timeout, dns, dns))
        assertEquals(BroadcastResultType.UNKNOWN_OUTCOME, result.resultType)
    }

    // ------------------------------------------------------------------ EthAccount

    private val ownerKey = "0x4c0883a69102937d6231471b5dbb6204fe5129617082792ae468d01a3f362318"
    private val recipient = "0x1e05057855bb37ba4a057fa09bfe191066e15d2f"
    private lateinit var backing: EthAccountBacking

    @Before
    fun setUp() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        WalletDB.Schema.create(driver)
        val db = WalletDB(driver, accountBackingAdapter, accountContextAdapter,
            deprecatedAccountAdapter, energyServiceFeeLinkAdapter,
            erc20ContextAdapter, ethAccountBackingAdapter, ethContextAdapter,
            feeEstimatorAdapter,
            trc20ContextAdapter, tronAccountBackingAdapter, tronContextAdapter)
        backing = EthAccountBacking(db, UUID.randomUUID(), EthTest)
    }

    /** A node that drops every response to sendtx; [knowsTx] decides what /tx/<hash> answers. */
    private class LossyNode(private val knowsTx: Boolean) : EthBlockchainService(emptyList()) {
        var sends = 0
        override fun sendTransaction(hex: String): EthBlockchainService.SendResult {
            sends++
            throw SocketTimeoutException("read timed out")
        }

        override fun getTransaction(hash: String): Tx =
            if (knowsTx) Tx() else throw IOException("Unexpected code 400")
    }

    private fun account(node: EthBlockchainService): EthAccount {
        val context = EthAccountContext(UUID.randomUUID(), EthTest, "eth", Balance.getZeroBalance(EthTest),
            {}, { null }, 0, nonce = BigInteger.valueOf(5))
        return EthAccount(11155111L, context, Credentials.create(ownerKey), backing, null, node).apply {
            broadcastRecheckDelaysMs = longArrayOf(0, 0, 0)
        }
    }

    private fun signedTx(account: EthAccount): EthTransaction =
        EthTransaction(EthTest, recipient, Value.valueOf(EthTest, BigInteger.valueOf(1_000_000)),
            BigInteger.valueOf(20_000_000_000), BigInteger.valueOf(5), BigInteger.valueOf(21_000), "").also {
            account.signTx(it, AesKeyCipher.defaultKeyCipher())
        }

    @Test
    fun `unknown outcome is stored as pending and keeps the nonce`() {
        val node = LossyNode(knowsTx = false)
        val account = account(node)
        val tx = signedTx(account)

        val result = account.broadcastTx(tx)

        assertEquals(BroadcastResultType.UNKNOWN_OUTCOME, result.resultType)
        assertEquals(4, node.sends) // first send + 3 re-sends of the same bytes
        val pending = account.getLivePendingTransactions()
        assertEquals(1, pending.size)
        assertEquals("0x" + HexUtils.toHex(tx.txHash), "0x" + HexUtils.toHex(pending[0].id))
        assertEquals(EthTxStatus.PENDING, pending[0].status)
        assertEquals(BigInteger.valueOf(5), pending[0].nonce)
        // a retry reuses nonce 5 and cannot mine alongside the lost tx
        assertEquals(BigInteger.valueOf(5), account.getNonce())
    }

    @Test
    fun `lost response of a tx the node has is a normal success`() {
        val node = LossyNode(knowsTx = true)
        val account = account(node)
        val tx = signedTx(account)

        val result = account.broadcastTx(tx)

        assertEquals(BroadcastResultType.SUCCESS, result.resultType)
        assertEquals(1, node.sends)
        assertEquals(1, account.getLivePendingTransactions().size)
        assertEquals(BigInteger.valueOf(6), account.getNonce())
    }

    @Test
    fun `explicit rejection stores nothing`() {
        val node = object : EthBlockchainService(emptyList()) {
            override fun sendTransaction(hex: String) = EthBlockchainService.SendResult(false, "intrinsic gas too low")
        }
        val account = account(node)

        val result = account.broadcastTx(signedTx(account))

        assertEquals(BroadcastResultType.REJECT_INVALID_TX_PARAMS, result.resultType)
        assertTrue(account.getLivePendingTransactions().isEmpty())
        assertEquals(BigInteger.valueOf(5), account.getNonce())
    }
}
