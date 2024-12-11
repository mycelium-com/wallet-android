package com.mycelium.wapi.wallet.genericdb

import app.cash.sqldelight.ColumnAdapter

object LongColumnAdapter : ColumnAdapter<Long, Long> {
    override fun decode(databaseValue: Long): Long = databaseValue

    override fun encode(value: Long): Long = value
}