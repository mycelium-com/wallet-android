package com.mycelium.wallet.coinapult

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper


class CoinapultSQLiteHelper(context: Context, val migrate: () -> Unit)
    : SQLiteOpenHelper(context, "coinapultmanagerbacking.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE coinapultcontext (id TEXT PRIMARY KEY"
                + ", address BLOB, archived INTEGER"
                + ", currency TEXT" + ");")

        migrate.invoke();
    }

    override fun onUpgrade(p0: SQLiteDatabase?, p1: Int, p2: Int) {
    }

}