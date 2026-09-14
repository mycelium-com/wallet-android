package com.mycelium.wapi.wallet.tron

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.mycelium.generated.wallet.database.WalletDB
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.genericdb.TronAccountBacking
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
import com.mycelium.wapi.wallet.trc20.coins.TRC20Token
import com.mycelium.wapi.wallet.tron.coins.TronMain
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.math.BigInteger
import java.util.UUID

/**
 * Builds before the "every TRON value is stored with the row's currency" rule wrote
 * energy-service rows tagged with the TRC20 token. The token is a runtime object the value
 * adapter cannot resolve, so one such row threw on every read of the history (the
 * "Key Tether USD is missing in the map" startup crash). The backing repairs its rows when opened.
 */
class TronValueTagRepairTest {
    private val uuid = UUID.randomUUID()
    private val otherUuid = UUID.randomUUID()
    private val owner = "TXYZopYRdj2D9XRtbG411XZZ3kM5VkAeBf"
    private val partner = "TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t"
    private val txid = "8c8272bfe21310ee77f1b5bc0a4fada8db2138e89fff1477e178855477308a6f"
    private val healthyTxid = "9e87e80ee21310ee77f1b5bc0a4fada8db2138e89fff1477e178855477308a6f"
    private val token = TRC20Token("Tether USDT", "USDT", 6, partner)

    private lateinit var driver: JdbcSqliteDriver
    private lateinit var db: WalletDB

    @Before
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        WalletDB.Schema.create(driver)
        db = WalletDB(driver, accountBackingAdapter, accountContextAdapter,
            deprecatedAccountAdapter, energyServiceFeeLinkAdapter,
            erc20ContextAdapter, ethAccountBackingAdapter, ethContextAdapter,
            feeEstimatorAdapter,
            trc20ContextAdapter, tronAccountBackingAdapter, tronContextAdapter)
    }

    /** Writes a row the way the broken builds did: bypassing the adapters, with the token as tag. */
    private fun insertRawRow(account: UUID, id: String, valueTag: String, feeTag: String, amount: Long, fee: Long) {
        driver.execute(null, """INSERT INTO AccountBacking(txid, uuid, currency, blockNumber, timestamp, raw, value, fee, confirmations)
            VALUES ('$id', '$account', 'tron.main', 100, 1700000000, '',
                    '{"Asset":"$valueTag","Value":$amount}', '{"Asset":"$feeTag","Value":$fee}', 1)""", 0)
        driver.execute(null, """INSERT INTO TronAccountBacking(txid, uuid, fromAddress, toAddress, hasTokenTransfers, tronTxType)
            VALUES ('$id', '$account', '$owner', '$partner', 1, 'TRC20_TRANSFER')""", 0)
    }

    private fun open(account: UUID = uuid) = TronAccountBacking(db, account, token, TronMain) { 200 }

    @Test
    fun repairsTokenTaggedRowsOnOpen() {
        insertRawRow(uuid, txid, valueTag = "Tether USD", feeTag = "Tether USD", amount = 5_000_000L, fee = 1_500_000L)
        insertRawRow(uuid, healthyTxid, valueTag = "tron.main", feeTag = "tron.main", amount = 7L, fee = 3L)

        val history = open().getTransactionSummaries(0, 10, owner)
        assertEquals(2, history.size)
        val repaired = history.first { it.id.contentEquals(hexToBytes(txid)) }
        // The amount survives and is shown in the account's currency; the fee is zero TRX
        assertEquals(Value.valueOf(token, BigInteger.valueOf(5_000_000L)), repaired.value)
        assertEquals(Value.zeroValue(TronMain), repaired.fee)
        val healthy = history.first { it.id.contentEquals(hexToBytes(healthyTxid)) }
        assertEquals(Value.valueOf(token, BigInteger.valueOf(7L)), healthy.value)
        assertEquals(Value.valueOf(TronMain, BigInteger.valueOf(3L)), healthy.fee)
    }

    @Test
    fun repairsOnlyTheOpenedAccount() {
        insertRawRow(otherUuid, txid, valueTag = "Tether USD", feeTag = "tron.main", amount = 1L, fee = 0L)

        open(uuid)

        val raw = driver.executeQuery(null, "SELECT value FROM AccountBacking WHERE txid = '$txid'", { c ->
            c.next().value; app.cash.sqldelight.db.QueryResult.Value(c.getString(0))
        }, 0).value
        assertEquals("""{"Asset":"Tether USD","Value":1}""", raw)
        assertEquals(1, open(otherUuid).getTransactionSummaries(0, 10, owner).size)
    }

    private fun hexToBytes(hex: String) = com.mrd.bitlib.util.HexUtils.toBytes(hex)
}
