package com.mycelium.wapi.wallet.genericdb

import java.util.*

interface Backing<Context> {
    fun loadAccountContexts(): List<Context>

    fun loadAccountContext(accountId: UUID): Context?

    fun createAccountContext(accountId: UUID, context: Context)

    fun updateAccountContext(accountId: UUID, context: Context)

    fun deleteAccountContext(uuid: UUID)
}