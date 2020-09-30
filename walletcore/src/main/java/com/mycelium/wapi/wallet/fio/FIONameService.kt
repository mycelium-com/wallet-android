package com.mycelium.wapi.wallet.fio

import com.mycelium.wapi.wallet.WalletAccount
import java.util.*

data class FIOName(val name: String,
                   val domain: FIODomain,
                   val expireDate: Date)

data class FIODomain(val domain: String, val expireDate: Date)

interface FIONameService {
    fun getAllFIONames(): List<FIOName>

    fun getConnectedAccounts(fioName: FIOName): List<WalletAccount<*>>

    fun connectAccounts(fioName: FIOName, accounts: List<WalletAccount<*>>)

    fun disconnectAccounts(fioName: FIOName, accounts: List<WalletAccount<*>>)
}

interface FIODomainService {
    fun getAllFIODomains(): List<FIODomain>

    fun getFIONames(domain: FIODomain): List<FIOName>
}