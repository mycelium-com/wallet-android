package com.mycelium.wallet.coinapult


import android.database.sqlite.SQLiteDatabase
import com.mycelium.wapi.wallet.CommonAccountBacking
import java.util.*


class SQLiteCoinapultAccountBacking(id: UUID, val database: SQLiteDatabase) : CommonAccountBacking {
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

}