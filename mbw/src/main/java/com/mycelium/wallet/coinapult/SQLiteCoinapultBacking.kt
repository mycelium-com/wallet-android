package com.mycelium.wallet.coinapult

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteStatement
import com.mycelium.wallet.persistence.MetadataStorage
import com.mycelium.wallet.persistence.SQLiteQueryWithBlobs
import com.mycelium.wallet.persistence.SQLiteQueryWithBlobs.uuidToBytes
import com.mycelium.wapi.wallet.AccountBacking
import com.mycelium.wapi.wallet.WalletBacking
import com.mycelium.wapi.wallet.btc.BtcAddress
import com.mycelium.wapi.wallet.coinapult.CoinapultAccountContext
import com.mycelium.wapi.wallet.coinapult.CoinapultTransaction
import com.mycelium.wapi.wallet.coinapult.Currency
import java.util.*


class SQLiteCoinapultBacking(val context: Context
                             , val metadataStorage: MetadataStorage
                             , addressByteArray: ByteArray)
    : WalletBacking<CoinapultAccountContext, CoinapultTransaction> {
    val database: SQLiteDatabase

    val insertOrReplaceSingleAddressAccount: SQLiteStatement

    init {
        val helper = CoinapultSQLiteHelper(context, metadataStorage, addressByteArray)
        database = helper.writableDatabase

        insertOrReplaceSingleAddressAccount = database.compileStatement("INSERT OR REPLACE INTO coinapultcontext VALUES (?,?,?,?)")
    }

    override fun createAccountContext(context: CoinapultAccountContext) {
        database.beginTransaction()
        try {
            // Create backing tables
//            var backing: SqliteColuAccountBacking? = _backings.get(context.id)
//            if (backing == null) {
//                createAccountBackingTables(context.id, _database)
//                backing = SqliteColuAccountBacking(context.id, _database)
//                _backings.put(context.id, backing)
//            }

            // Create context
            insertOrReplaceSingleAddressAccount.bindBlob(1, uuidToBytes(context.id))
            insertOrReplaceSingleAddressAccount.bindBlob(2, context.address.allAddressBytes)
            insertOrReplaceSingleAddressAccount.bindLong(3, (if (context.isArchived()) 1 else 0).toLong())
            insertOrReplaceSingleAddressAccount.bindString(4, context.currency.name)
            insertOrReplaceSingleAddressAccount.executeInsert()
            database.setTransactionSuccessful()
        } finally {
            database.endTransaction()
        }
    }

    override fun loadAccountContexts(): List<CoinapultAccountContext> {
        val result = mutableListOf<CoinapultAccountContext>()

        var cursor: Cursor? = null
        try {
            val blobQuery = SQLiteQueryWithBlobs(database)
            cursor = blobQuery.query(false, "coinapultcontext"
                    , arrayOf("id", "address", "archived", "currency"), null
                    , null, null, null, null, null)
            while (cursor.moveToNext()) {
                val id = SQLiteQueryWithBlobs.uuidFromBytes(cursor.getBlob(0))
                val addressBytes = cursor.getBlob(1)
                val isArchived = cursor.getInt(2) == 1
                val currency = Currency.all[cursor.getString(3)]!!
                result.add(CoinapultAccountContext(id, BtcAddress(currency, addressBytes), isArchived, currency))
            }
        } finally {
            cursor?.close()
        }
        return result


    }

    override fun getAccountBacking(accountId: UUID?): AccountBacking<CoinapultTransaction> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun deleteAccountContext(uuid: UUID?) {

    }
}