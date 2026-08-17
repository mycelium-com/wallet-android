package com.mycelium.wapi.wallet.eth

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.mrd.bitlib.crypto.InMemoryPrivateKey
import com.mycelium.generated.wallet.database.WalletDB
import com.mycelium.wapi.wallet.Address
import com.mycelium.wapi.wallet.BroadcastResult
import com.mycelium.wapi.wallet.EthTransactionSummary
import com.mycelium.wapi.wallet.Fee
import com.mycelium.wapi.wallet.KeyCipher
import com.mycelium.wapi.wallet.SyncMode
import com.mycelium.wapi.wallet.Transaction
import com.mycelium.wapi.wallet.TransactionData
import com.mycelium.wapi.wallet.coins.Balance
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.eth.coins.EthTest
import com.mycelium.wapi.wallet.genericdb.BTCVAccountBackingAdapter
import com.mycelium.wapi.wallet.genericdb.BTCVContextAdapter
import com.mycelium.wapi.wallet.genericdb.BTCVOutgoingTxAdapter
import com.mycelium.wapi.wallet.genericdb.BTCVPtxoAdapter
import com.mycelium.wapi.wallet.genericdb.BTCVRefersPtxoAdapter
import com.mycelium.wapi.wallet.genericdb.BTCVTransactionAdapter
import com.mycelium.wapi.wallet.genericdb.BTCVUtxoAdapter
import com.mycelium.wapi.wallet.genericdb.EthAccountBacking
import com.mycelium.wapi.wallet.genericdb.accountBackingAdapter
import com.mycelium.wapi.wallet.genericdb.accountContextAdapter
import com.mycelium.wapi.wallet.genericdb.erc20ContextAdapter
import com.mycelium.wapi.wallet.genericdb.ethAccountBackingAdapter
import com.mycelium.wapi.wallet.genericdb.ethContextAdapter
import com.mycelium.wapi.wallet.genericdb.feeEstimatorAdapter
import com.mycelium.wapi.wallet.genericdb.fioAccountBackingAdapter
import com.mycelium.wapi.wallet.genericdb.fioContextAdapter
import com.mycelium.wapi.wallet.genericdb.fioKnownNamesAdapter
import com.mycelium.wapi.wallet.genericdb.fioNameAccountMappingsAdapter
import com.mycelium.wapi.wallet.genericdb.fioOtherBlockchainTransactionsAdapter
import com.mycelium.wapi.wallet.genericdb.fioReceivedRequestsAdapter
import com.mycelium.wapi.wallet.genericdb.fioSentRequestsAdapter
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.math.BigInteger
import java.util.UUID

/**
 * Regression tests for the pending-tx lifecycle reconciliation.
 *
 * The key scenario: a wallet holds both accounts A and B and sends ETH
 * A -> B. On B's side the tx is an *incoming* pending tx whose nonce
 * belongs to A's tx stream. B almost always has its own confirmed
 * outgoing tx with the same small nonce (both streams start at 0), and
 * matching nonces across different senders falsely flagged the fresh
 * incoming tx as REPLACED.
 */
class ReconcileLocalPendingTest {
    private val mapper = ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    private val ownerAddress = "0x065a575c8aeda4d4b1261fb741246018980486e3"
    private val counterpartyAddress = "0x1e05057855bb37ba4a057fa09bfe191066e15d2f"
    private val thirdPartyAddress = "0x2222222222333333333344444444445555555555"

    private lateinit var backing: EthAccountBacking
    private lateinit var account: TestEthAccount

    @Before
    fun setUp() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        WalletDB.Schema.create(driver)
        val db = WalletDB(driver, accountBackingAdapter, accountContextAdapter,
                BTCVAccountBackingAdapter, BTCVContextAdapter, BTCVOutgoingTxAdapter,
                BTCVPtxoAdapter, BTCVRefersPtxoAdapter, BTCVTransactionAdapter, BTCVUtxoAdapter,
                erc20ContextAdapter, ethAccountBackingAdapter, ethContextAdapter,
                feeEstimatorAdapter, fioAccountBackingAdapter, fioContextAdapter,
                fioKnownNamesAdapter, fioNameAccountMappingsAdapter,
                fioOtherBlockchainTransactionsAdapter, fioReceivedRequestsAdapter, fioSentRequestsAdapter)
        backing = EthAccountBacking(db, UUID.randomUUID(), EthTest)
        account = TestEthAccount(backing, EthBlockchainService(emptyList()),
                EthAddress(EthTest, ownerAddress))
    }

    private fun putLocalTx(txid: String, from: String, to: String, nonce: Long,
                           confirmations: Int, timestamp: Long, valueWei: Long = 1_000_000L) {
        backing.putTransaction(
                if (confirmations == 0) -1 else 100,
                timestamp, txid, "", from, to,
                Value.valueOf(EthTest, BigInteger.valueOf(valueWei)),
                Value.valueOf(EthTest, BigInteger.valueOf(21_000L * 20)),
                confirmations, BigInteger.valueOf(nonce), BigInteger.valueOf(20))
    }

    private fun remoteTx(txid: String, from: String, to: String, nonce: Long,
                         confirmations: Int, valueWei: Long = 1_000_000L): Tx =
            mapper.readValue("""{
                "txid": "$txid",
                "vin": [{"addresses": ["$from"]}],
                "vout": [{"addresses": ["$to"]}],
                "blockHeight": ${if (confirmations == 0) 0 else 100},
                "confirmations": $confirmations,
                "blockTime": 1713200000,
                "value": "$valueWei",
                "fees": "420000",
                "ethereumSpecific": {"nonce": "$nonce", "gasLimit": "21000", "gasUsed": "21000",
                                     "gasPrice": "20", "status": true},
                "tokenTransfers": []
            }""", Tx::class.java)

    private fun statusOf(txid: String): EthTxStatus =
            (backing.getTransactionSummary(txid, ownerAddress) as EthTransactionSummary).status

    @Test
    fun `incoming pending tx is not replaced by owner's own tx with same nonce`() {
        val now = System.currentTimeMillis() / 1000
        // The owner's own first outgoing tx, confirmed long ago, nonce 0.
        putLocalTx("0xaa01", ownerAddress, thirdPartyAddress, nonce = 0, confirmations = 10, timestamp = now - 100_000)
        // Fresh incoming pending tx from another account; the counterparty's
        // nonce also happens to be 0 — nonces of different senders are unrelated.
        putLocalTx("0xbb02", counterpartyAddress, ownerAddress, nonce = 0, confirmations = 0, timestamp = now)

        val remote = listOf(
                remoteTx("0xaa01", ownerAddress, thirdPartyAddress, nonce = 0, confirmations = 10),
                remoteTx("0xbb02", counterpartyAddress, ownerAddress, nonce = 0, confirmations = 0))
        runBlocking { account.reconcile(remote, setOf("0xaa01", "0xbb02")) }

        assertEquals(EthTxStatus.PENDING, statusOf("0xbb02"))
    }

    @Test
    fun `outgoing pending tx is marked replaced by confirmed same-nonce tx`() {
        val now = System.currentTimeMillis() / 1000
        putLocalTx("0xcc03", ownerAddress, thirdPartyAddress, nonce = 1, confirmations = 0, timestamp = now)

        // A different tx from the owner at the same nonce has confirmed.
        val remote = listOf(
                remoteTx("0xdd04", ownerAddress, counterpartyAddress, nonce = 1, confirmations = 3))
        runBlocking { account.reconcile(remote, setOf("0xdd04")) }

        assertEquals(EthTxStatus.REPLACED, statusOf("0xcc03"))
    }

    @Test
    fun `outgoing pending tx is marked canceled when replacement is zero-value self-transfer`() {
        val now = System.currentTimeMillis() / 1000
        putLocalTx("0xee05", ownerAddress, thirdPartyAddress, nonce = 2, confirmations = 0, timestamp = now)

        val remote = listOf(
                remoteTx("0xff06", ownerAddress, ownerAddress, nonce = 2, confirmations = 3, valueWei = 0))
        runBlocking { account.reconcile(remote, setOf("0xff06")) }

        assertEquals(EthTxStatus.CANCELED, statusOf("0xee05"))
    }

    @Test
    fun `incoming pending tx replaced by counterparty's confirmed same-nonce tx`() {
        val now = System.currentTimeMillis() / 1000
        putLocalTx("0xab07", counterpartyAddress, ownerAddress, nonce = 5, confirmations = 0, timestamp = now)

        // The sender bumped their tx: a different confirmed tx from the SAME
        // sender at the same nonce (still paying us, so it's in our history).
        val remote = listOf(
                remoteTx("0xcd08", counterpartyAddress, ownerAddress, nonce = 5, confirmations = 2))
        runBlocking { account.reconcile(remote, setOf("0xcd08")) }

        assertEquals(EthTxStatus.REPLACED, statusOf("0xab07"))
    }

    @Test
    fun `sticky replaced status is preserved while tx stays unconfirmed`() {
        val now = System.currentTimeMillis() / 1000
        putLocalTx("0x1109", ownerAddress, thirdPartyAddress, nonce = 3, confirmations = 0, timestamp = now)
        backing.markTransactionStatus("0x1109", EthTxStatus.REPLACED)

        // Blockbook's stale index re-serves the tx, still unconfirmed — the
        // local verdict must survive the upsert.
        putLocalTx("0x1109", ownerAddress, thirdPartyAddress, nonce = 3, confirmations = 0, timestamp = now)

        assertEquals(EthTxStatus.REPLACED, statusOf("0x1109"))
    }

    @Test
    fun `sticky replaced status heals once the tx actually confirms`() {
        val now = System.currentTimeMillis() / 1000
        putLocalTx("0x220a", counterpartyAddress, ownerAddress, nonce = 0, confirmations = 0, timestamp = now)
        // A stale/wrong local verdict...
        backing.markTransactionStatus("0x220a", EthTxStatus.REPLACED)

        // ...must yield to on-chain truth: the tx demonstrably mined.
        putLocalTx("0x220a", counterpartyAddress, ownerAddress, nonce = 0, confirmations = 12, timestamp = now)

        assertEquals(EthTxStatus.CONFIRMED, statusOf("0x220a"))
    }
}

/** Minimal concrete account exposing the protected reconcile hook. */
private class TestEthAccount(
        backing: EthAccountBacking,
        blockchainService: EthBlockchainService,
        address: EthAddress,
) : AbstractEthERC20Account(EthTest, null, backing, blockchainService,
        TestEthAccount::class.simpleName, address) {
    private var nonce: BigInteger = BigInteger.ZERO

    suspend fun reconcile(fullAddressTxs: List<Tx>, knownAliveTxids: Set<String>) =
            reconcileLocalPending(fullAddressTxs, knownAliveTxids)

    override suspend fun doSynchronization(mode: SyncMode?) = true
    override fun setNonce(nonce: BigInteger) { this.nonce = nonce }
    override fun getNonce() = nonce
    override fun setBlockChainHeight(height: Int) {}
    override fun getBlockChainHeight() = 100
    override fun updateBalanceCache() = false
    override fun ethFeeSpendable(): Value = Value.zeroValue(EthTest)
    override suspend fun cancelTransaction(record: EthTransactionSummary, estimate: CancelEstimate): BroadcastResult =
            throw UnsupportedOperationException()

    override fun createTx(address: Address, amount: Value, fee: Fee, data: TransactionData?): Transaction =
            throw UnsupportedOperationException()
    override fun signTx(request: Transaction, keyCipher: KeyCipher) = throw UnsupportedOperationException()
    override fun broadcastTx(tx: Transaction): BroadcastResult = throw UnsupportedOperationException()
    override fun signMessage(message: String, address: Address?): String = throw UnsupportedOperationException()
    override val coinType get() = EthTest
    override val basedOnCoinType get() = EthTest
    override val accountBalance: Balance get() = Balance.getZeroBalance(EthTest)
    override var label: String = "test"
    override val isArchived = false
    override fun archiveAccount() {}
    override fun activateAccount() {}
    override fun dropCachedData() {}
    override fun isVisible() = true
    override fun isDerivedFromInternalMasterseed() = false
    override val id: UUID = UUID.randomUUID()
    override fun broadcastOutgoingTransactions() = true
    override fun calculateMaxSpendableAmount(minerFeePerKilobyte: Value, destinationAddress: EthAddress?, txData: TransactionData?): Value =
            Value.zeroValue(EthTest)
    override val syncTotalRetrievedTransactions = 0
    override val typicalEstimatedTransactionSize = 21000
    override fun getPrivateKey(cipher: KeyCipher): InMemoryPrivateKey = throw UnsupportedOperationException()
}
