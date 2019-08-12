package com.mycelium.wallet

object SQLUtil {
    val MAX_SQL_PARAMS: Int = 999
    // To prevent limit of SQL variables one can specify in a single execSQL query
    // max sql params (https://raw.githubusercontent.com/android/platform_external_sqlite/master/dist/sqlite3.c) is 999
    // https://stackoverflow.com/questions/15312590/what-is-the-limit-of-sql-variables-one-can-specify-in-a-single-execsql-query
    // paramsCount - number of  (?,?,?...) symbols
    fun <T> doChunked(source: Collection<T>, paramsCount: Int, block: (List<T>) -> Unit ) {
        source.chunked(MAX_SQL_PARAMS / paramsCount).forEach {
            block(it)
        }
    }
}