package com.mycelium.wallet.persistence

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [AccountContext::class], version = 1)
public abstract class AccountsDB : RoomDatabase() {
    abstract fun contextDao(): AccountContextDAO

    companion object {
        @Volatile
        private var INSTANCE: AccountsDB? = null

        @JvmStatic
        fun getDatabase(context: Context): AccountsDB {
            return INSTANCE ?: synchronized(this) {
                if (INSTANCE == null) {
                    // Create database here
                    INSTANCE = Room.databaseBuilder(
                            context.applicationContext,
                            AccountsDB::class.java,
                            "Accounts_database"
                    ).build()
                }
                INSTANCE!!
            }
        }
    }
}