package com.mycelium.wapi.wallet.single

import com.mrd.bitlib.StandardTransactionBuilder
import com.mrd.bitlib.crypto.InMemoryPrivateKey
import com.mrd.bitlib.crypto.RandomSource
import com.mrd.bitlib.model.AddressType
import com.mrd.bitlib.model.BitcoinAddress
import com.mrd.bitlib.model.BitcoinTransaction
import com.mrd.bitlib.model.InputWitness
import com.mrd.bitlib.model.NetworkParameters
import com.mrd.bitlib.model.OutPoint
import com.mrd.bitlib.model.ScriptInput
import com.mrd.bitlib.model.TransactionInput
import com.mrd.bitlib.model.TransactionOutput
import com.mrd.bitlib.util.HashUtils
import com.mrd.bitlib.util.HexUtils
import com.mrd.bitlib.util.Sha256Hash
import com.mycelium.wapi.api.Wapi
import com.mycelium.wapi.api.WapiResponse
import com.mycelium.wapi.api.lib.TransactionExApi
import com.mycelium.wapi.api.request.*
import com.mycelium.wapi.api.response.*
import com.mycelium.wapi.model.TransactionOutputEx
import com.mycelium.wapi.model.TransactionStatus
import com.mycelium.wapi.wallet.AesKeyCipher
import com.mycelium.wapi.wallet.SecureKeyValueStore
import com.mycelium.wapi.wallet.SyncMode
import com.mycelium.wapi.wallet.btc.BtcReceiver
import com.mycelium.wapi.wallet.btc.ChangeAddressMode
import com.mycelium.wapi.wallet.btc.InMemoryBtcWalletManagerBacking
import com.mycelium.wapi.wallet.btc.Reference
import com.mycelium.wapi.wallet.btc.single.PublicPrivateKeyStore
import com.mycelium.wapi.wallet.btc.single.SingleAddressAccount
import com.mycelium.wapi.wallet.btc.single.SingleAddressAccountContext
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

/**
 * A server (or whoever sits between us and it) hands the wallet transactions and coins. These
 * tests play such a server against a real single address account and check that what it invents
 * does not become our history, our balance or something we sign.
 */
class ServerForgedSpendTest {
    private val network = NetworkParameters.testNetwork
    private val key = InMemoryPrivateKey(HexUtils.toBytes("42" + "00".repeat(31)), true)
    private val stranger = InMemoryPrivateKey(HexUtils.toBytes("43" + "00".repeat(31)), true)
        .publicKey.toAddress(network, AddressType.P2WPKH)
    private lateinit var account: SingleAddressAccount
    private lateinit var server: FakeServer

    private fun ours(type: AddressType): BitcoinAddress = key.publicKey.toAddress(network, type)

    @Before
    fun setUp() {
        val backing = InMemoryBtcWalletManagerBacking()
        val store = PublicPrivateKeyStore(SecureKeyValueStore(backing, mock(RandomSource::class.java)))
        val addresses = key.publicKey.getAllSupportedAddresses(network)
        addresses.values.forEach { store.setPrivateKey(it.allAddressBytes, key, AesKeyCipher.defaultKeyCipher()) }
        val id = SingleAddressAccount.calculateId(ours(AddressType.P2SH_P2WPKH))
        val context = SingleAddressAccountContext(id, addresses, false, 0, AddressType.P2WPKH)
        backing.createSingleAddressAccountContext(context)
        server = FakeServer()
        account = SingleAddressAccount(context, store, network, backing.getSingleAddressAccountBacking(id), server,
            Reference(ChangeAddressMode.PRIVACY))
    }

    private fun sync() = runBlocking { account.doSynchronization(SyncMode.NORMAL) }

    private fun tx(inputs: List<OutPoint>, outputs: List<Pair<BitcoinAddress, Long>>) = BitcoinTransaction(
        1,
        inputs.map { TransactionInput(it, ScriptInput.EMPTY, -3, 0) }.toTypedArray(),
        outputs.map { (address, value) -> StandardTransactionBuilder.createOutput(address, value, network) }.toTypedArray(),
        0
    )

    /** A stranger's coin [grandparent] paying 1 BTC to us in [funding], confirmed, at a fee of 10000 sat. */
    private val grandparent = tx(listOf(OutPoint(HashUtils.sha256(byteArrayOf(1)), 0)), listOf(stranger to 100_010_000))
    private val funding = tx(listOf(OutPoint(grandparent.id, 0)), listOf(ours(AddressType.P2WPKH) to 100_000_000))

    private fun fundAccount() {
        server.publish(grandparent, 100)
        server.publish(funding, 101)
        sync()
        assertEquals(100_000_000L, account.accountBalance.confirmed.valueAsLong)
    }

    @Test
    fun forgedSpendOfOurCoinIsDropped() {
        fundAccount()
        // Unsigned: our coin to the attacker, change to us
        val forged = tx(listOf(OutPoint(funding.id, 0)), listOf(stranger to 60_000_000, ours(AddressType.P2WPKH) to 40_000_000))
        server.spend(OutPoint(funding.id, 0))
        server.publish(forged, -1)
        sync()

        assertNull("forgery stored as our transaction", account.getTransaction(forged.id))
        assertTrue("forged change counted", account.accountBacking.allUnspentOutputs.none { it.value == 40_000_000L })
    }

    @Test
    fun ourOwnSpendFromTheServerIsKept() {
        fundAccount()
        // As if sent from the same key on another device: signed by us, unknown locally
        val unsigned = account.createUnsignedTransaction(listOf(BtcReceiver(stranger, 60_000_000)), 1000)
        val signed = account.signTransaction(unsigned, AesKeyCipher.defaultKeyCipher())
        server.spend(OutPoint(funding.id, 0))
        server.publish(signed, -1)
        sync()

        assertNotNull(account.getTransaction(signed.id))
        assertTrue(account.accountBacking.allUnspentOutputs.any { it.value == signed.outputs.first { o -> o.script.getAddress(network) != stranger }.value })
    }

    @Test
    fun bytesNotHashingToTheTxidAreDropped() {
        server.publish(grandparent, 100)
        server.publish(funding, 101)
        // Under the funding's txid the server serves some other transaction paying us 5 BTC
        server.serveAs(funding.id, tx(listOf(OutPoint(grandparent.id, 0)), listOf(ours(AddressType.P2WPKH) to 500_000_000)))
        sync()

        assertNull(account.getTransaction(funding.id))
        assertEquals(0L, account.accountBalance.confirmed.valueAsLong)
    }

    @Test
    fun understatedCoinIsDropped() {
        server.publish(grandparent, 100)
        server.publish(funding, 101)
        server.lieAboutValue(OutPoint(funding.id, 0), 10_000_000)
        sync()

        assertEquals(0L, account.accountBalance.confirmed.valueAsLong)
    }

    @Test
    fun signingRefusesACoinItsTransactionContradicts() {
        fundAccount()
        // A coin stored before sync checked it, at a tenth of its value
        val outPoint = OutPoint(funding.id, 0)
        val stored = account.accountBacking.getUnspentOutput(outPoint)
        account.accountBacking.putUnspentOutput(TransactionOutputEx(outPoint, stored.height, 10_000_000, stored.script, false))
        val unsigned = account.createUnsignedTransaction(listOf(BtcReceiver(stranger, 5_000_000)), 1000)
        try {
            account.signTransaction(unsigned, AesKeyCipher.defaultKeyCipher())
            fail("signed a spend of a coin whose transaction says another value")
        } catch (expected: IllegalStateException) {
        }
    }

    @Test
    fun cpfpFeeComesFromVerifiedParents() {
        server.publish(grandparent, 100)
        server.publish(funding, -1)
        sync()

        val (fee, size) = account.getCpfpParentFeeAndVsize(funding.id)!!
        assertEquals(10_000L, fee)
        assertEquals(funding.vsize(), size)
    }

    @Test
    fun cpfpIgnoresPaddedWitness() {
        // Same txid, but the witness blown up by the server: no miner pays for those bytes
        val padded = BitcoinTransaction.fromBytes(funding.toBytes()).apply {
            inputs[0].witness = InputWitness(1).apply { setStack(0, ByteArray(50_000)) }
        }
        assertEquals(funding.id, padded.id)
        server.publish(grandparent, 100)
        server.publish(funding, -1)
        server.serveAs(funding.id, padded)
        sync()

        val (_, size) = account.getCpfpParentFeeAndVsize(funding.id)!!
        assertTrue("$size", size < funding.vsize() + 50)
    }

    private class FakeServer : Wapi {
        private val height = 200
        private val transactions = mutableMapOf<Sha256Hash, Pair<BitcoinTransaction, Int>>()
        private val servedAs = mutableMapOf<Sha256Hash, BitcoinTransaction>()
        private val spent = mutableSetOf<OutPoint>()
        private val values = mutableMapOf<OutPoint, Long>()

        fun publish(tx: BitcoinTransaction, height: Int) { transactions[tx.id] = tx to height }
        fun serveAs(txid: Sha256Hash, tx: BitcoinTransaction) { servedAs[txid] = tx }
        fun spend(outPoint: OutPoint) { spent += outPoint }
        fun lieAboutValue(outPoint: OutPoint, value: Long) { values[outPoint] = value }

        private fun outputsTo(address: BitcoinAddress) = transactions.values.flatMap { (tx, height) ->
            tx.outputs.mapIndexedNotNull { i, output: TransactionOutput ->
                val outPoint = OutPoint(tx.id, i)
                if (output.script.getAddress(address.network) == address && outPoint !in spent) {
                    TransactionOutputEx(outPoint, height, values[outPoint] ?: output.value, output.script.scriptBytes, false)
                } else null
            }
        }

        override suspend fun queryUnspentOutputs(request: QueryUnspentOutputsRequest) =
            WapiResponse(QueryUnspentOutputsResponse(height, request.addresses.flatMap { outputsTo(it) }))

        override suspend fun queryTransactionInventory(request: QueryTransactionInventoryRequest) =
            WapiResponse(QueryTransactionInventoryResponse(height, transactions.values.filter { (tx, _) ->
                tx.outputs.any { it.script.getAddress(request.addresses[0].network) in request.addresses } ||
                    tx.inputs.any { input -> transactions[input.outPoint.txid]?.first?.outputs?.getOrNull(input.outPoint.index)
                        ?.script?.getAddress(request.addresses[0].network) in request.addresses }
            }.map { it.first.id }))

        override suspend fun getTransactions(request: GetTransactionsRequest) =
            WapiResponse(GetTransactionsResponse(request.txIds.mapNotNull { txid ->
                val (tx, height) = transactions[txid] ?: return@mapNotNull null
                val bytes = (servedAs[txid] ?: tx).toBytes()
                TransactionExApi(txid, tx.hash, height, 0, bytes, 0, false)
            }))

        override suspend fun checkTransactions(request: CheckTransactionsRequest) =
            WapiResponse(CheckTransactionsResponse(request.txIds.map { txid ->
                TransactionStatus(txid, txid in transactions, 0, transactions[txid]?.second ?: -1, 0, false)
            }))

        override fun broadcastTransaction(request: BroadcastTransactionRequest): WapiResponse<BroadcastTransactionResponse> =
            throw UnsupportedOperationException()
        override fun getExchangeRates(request: GetExchangeRatesRequest?): WapiResponse<GetExchangeRatesResponse?>? = null
        override fun ping(): WapiResponse<PingResponse?>? = null
        override fun collectError(request: ErrorCollectorRequest?): WapiResponse<ErrorCollectorResponse?>? = null
        override fun getVersionInfoEx(request: VersionInfoExRequest?): WapiResponse<VersionInfoExResponse?>? = null
        override suspend fun getMinerFeeEstimations(): WapiResponse<MinerFeeEstimationResponse> =
            throw UnsupportedOperationException()
    }
}
