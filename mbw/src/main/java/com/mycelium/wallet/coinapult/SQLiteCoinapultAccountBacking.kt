package com.mycelium.wallet.coinapult

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.text.TextUtils
import android.util.Log
import com.mrd.bitlib.model.OutPoint
import com.mrd.bitlib.util.HexUtils
import com.mrd.bitlib.util.Sha256Hash
import com.mycelium.wallet.persistence.SQLiteQueryWithBlobs
import com.mycelium.wallet.persistence.SQLiteQueryWithBlobs.uuidToBytes
import com.mycelium.wapi.model.TransactionEx
import com.mycelium.wapi.model.TransactionOutputEx
import com.mycelium.wapi.wallet.AccountBacking
import com.mycelium.wapi.wallet.FeeEstimationsGeneric
import com.mycelium.wapi.wallet.coinapult.CoinapultTransaction
import java.io.*
import java.util.*


class SQLiteCoinapultAccountBacking(id: UUID, val database: SQLiteDatabase) : AccountBacking<CoinapultTransaction> {
    private val txTableName = "tx${HexUtils.toHex(uuidToBytes(id))}"

    private val deleteTx = database.compileStatement("DELETE FROM $txTableName WHERE id = ?");
    private val dropDataTx = database.compileStatement("DELETE FROM $txTableName");

    override fun putTransactions(transactions: List<CoinapultTransaction>?) {
        if (transactions == null) {
            return
        }
        database.beginTransaction()
        try {
            dropDataTx.execute()
            if(transactions.isNotEmpty()) {
                val updateQuery = "INSERT OR REPLACE INTO $txTableName VALUES " + TextUtils.join(",", Collections.nCopies(transactions.size, " (?,?,?,?,?) "))
                val updateStatement = database.compileStatement(updateQuery)
                var i = 0
                transactions.forEach { transaction ->
                    val index = i * 5
                    updateStatement.bindBlob(index + 1, transaction.id!!.bytes)
                    updateStatement.bindBlob(index + 2, transaction.id!!.bytes)
                    updateStatement.bindLong(index + 3, (if (transaction.height == -1) Integer.MAX_VALUE else transaction.height).toLong())
                    updateStatement.bindLong(index + 4, transaction.time)

                    var txData: ByteArray? = null
                    val bos = ByteArrayOutputStream()
                    try {
                        val out = ObjectOutputStream(bos)
                        out.writeObject(transaction)
                        out.flush()
                        txData = bos.toByteArray()
                    } catch (e: IOException) {
                        Log.e("colu accountBacking", "", e)
                    } finally {
                        try {
                            bos.close()
                        } catch (ignore: IOException) {
                        }

                    }
                    updateStatement.bindBlob(index + 5, txData)
                    i++
                }
                updateStatement.executeInsert()
            }
            //            for (TransactionEx transaction : transactions) {
            //               putReferencedOutputs(transaction.binary);
            //            }
            database.setTransactionSuccessful()
        } finally {
            database.endTransaction()
        }

    }

    override fun beginTransaction() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun setTransactionSuccessful() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun endTransaction() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun clear() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getAllUnspentOutputs(): MutableCollection<TransactionOutputEx> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getUnspentOutput(outPoint: OutPoint?): TransactionOutputEx {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun deleteUnspentOutput(outPoint: OutPoint?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun putUnspentOutput(output: TransactionOutputEx?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun putParentTransactionOuputs(outputsList: MutableList<TransactionOutputEx>?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun putParentTransactionOutput(output: TransactionOutputEx?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getParentTransactionOutput(outPoint: OutPoint?): TransactionOutputEx {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun hasParentTransactionOutput(outPoint: OutPoint?): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun putTransaction(transaction: TransactionEx?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun putTransactions(transactions: MutableCollection<out TransactionEx>?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getTransaction(hash: Sha256Hash?): TransactionEx {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


    override fun getTransactionHistory(offset: Int, limit: Int): MutableList<TransactionEx> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getTransactionsSince(since: Long): MutableList<TransactionEx> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getUnconfirmedTransactions(): MutableCollection<TransactionEx> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getYoungTransactions(maxConfirmations: Int, blockChainHeight: Int): MutableCollection<TransactionEx> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun hasTransaction(txid: Sha256Hash?): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun putOutgoingTransaction(txid: Sha256Hash?, rawTransaction: ByteArray?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getOutgoingTransactions(): MutableMap<Sha256Hash, ByteArray> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isOutgoingTransaction(txid: Sha256Hash?): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun removeOutgoingTransaction(txid: Sha256Hash?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun deleteTxRefersParentTransaction(txId: Sha256Hash?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getTransactionsReferencingOutPoint(outPoint: OutPoint?): MutableCollection<Sha256Hash> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun putTxRefersParentTransaction(txId: Sha256Hash?, refersOutputs: MutableList<OutPoint>?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun putFeeEstimation(feeEstimation: FeeEstimationsGeneric?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getTx(hash: Sha256Hash?): CoinapultTransaction? {
        var result: CoinapultTransaction? = null
        var cursor: Cursor? = null
        try {
            val blobQuery = SQLiteQueryWithBlobs(database)
            blobQuery.bindBlob(1, hash?.bytes)
            cursor = blobQuery.query(false, txTableName, arrayOf("hash", "height", "time", "binary")
                    , "id = ?", null, null, null, null, null)
            if (cursor!!.moveToNext()) {
                val bis = ByteArrayInputStream(cursor.getBlob(4))
                var inputStream: ObjectInput? = null
                try {
                    inputStream = ObjectInputStream(bis)
                    result = inputStream.readObject() as CoinapultTransaction
                } catch (e: IOException) {
                    e.printStackTrace()
                } catch (e: ClassNotFoundException) {
                    e.printStackTrace()
                } finally {
                    try {
                        inputStream?.close()
                    } catch (ignore: IOException) {
                    }
                }
            }
        } finally {
            cursor?.close()
        }
        return result
    }

    override fun getTransactions(offset: Int, limit: Int): List<CoinapultTransaction> {
        var cursor: Cursor? = null
        val result = mutableListOf<CoinapultTransaction>()
        try {
            cursor = database.rawQuery("SELECT id, hash, height, time, binary FROM " + txTableName
                    + " ORDER BY height desc limit ? offset ?",
                    arrayOf(Integer.toString(limit), Integer.toString(offset)))
            while (cursor!!.moveToNext()) {
                val txid = Sha256Hash(cursor.getBlob(0))
                val hash = Sha256Hash(cursor.getBlob(1))
                var tex: CoinapultTransaction? = null
                val bis = ByteArrayInputStream(cursor.getBlob(4))
                var inputStream: ObjectInput? = null
                try {
                    inputStream = ObjectInputStream(bis)
                    tex = inputStream.readObject() as CoinapultTransaction
                } catch (e: IOException) {
                    e.printStackTrace()
                } catch (e: ClassNotFoundException) {
                    e.printStackTrace()
                } finally {
                    try {
                        inputStream?.close()
                    } catch (ignore: IOException) {
                    }
                }
                tex?.let { result.add(tex) }
            }
        } finally {
            cursor?.close()
        }
        return result
    }

    override fun deleteTransaction(hash: Sha256Hash?) {
        deleteTx.bindBlob(1, hash?.bytes)
        deleteTx.execute()
    }

}