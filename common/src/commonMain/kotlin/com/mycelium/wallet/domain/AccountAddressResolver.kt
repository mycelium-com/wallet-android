package com.mycelium.wallet.domain

interface AccountAddressResolver {
    fun getAccountLabelForAddress(address: String): String?
    fun getAccountLabelForId(accountId: String): String?
}
