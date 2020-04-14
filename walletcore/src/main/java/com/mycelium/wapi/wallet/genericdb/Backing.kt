package com.mycelium.wapi.wallet.genericdb

import java.util.*

interface Backing<Context> {
    fun loadAccountContexts(): List<Context>

    fun loadAccountContext(accountId: UUID): Context?

    fun createAccountContext(context: Context)

    fun updateAccountContext(context: Context)

    fun deleteAccountContext(uuid: UUID)
}