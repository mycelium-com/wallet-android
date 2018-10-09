package com.mycelium.wallet.coinapult

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.google.common.base.Splitter
import com.mycelium.wallet.persistence.MetadataStorage
import com.mycelium.wapi.wallet.coinapult.CoinapultUtils
import com.mycelium.wapi.wallet.coinapult.Currency


class CoinapultSQLiteHelper(context: Context, val metadataStorage: MetadataStorage
                            , val addressByteArray: ByteArray)
    : SQLiteOpenHelper(context, "coinapultmanagerbacking.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE coinapultcontext (id TEXT PRIMARY KEY"
                + ", address BLOB, archived INTEGER"
                + ", currency TEXT" + ");")

        /**
         * import accounts from old place
         */
        val currencies = Splitter.on(",").split(metadataStorage.coinapultCurrencies)
        currencies.forEach {
            Currency.all[it]?.let { currency ->
                val id = CoinapultUtils.getGuidForAsset(currency, addressByteArray)

            }
        }
    }

    override fun onUpgrade(p0: SQLiteDatabase?, p1: Int, p2: Int) {
    }

}