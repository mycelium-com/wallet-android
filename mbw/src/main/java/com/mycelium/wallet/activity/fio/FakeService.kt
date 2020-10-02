package com.mycelium.wallet.activity.fio

import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.fio.FIODomain
import com.mycelium.wapi.wallet.fio.FIODomainService
import com.mycelium.wapi.wallet.fio.RegisteredFIOName
import com.mycelium.wapi.wallet.fio.FIONameService
import java.util.*


object FakeService : FIODomainService, FIONameService {

    val domains = mapOf(
            FIODomain("my-own-domain", Date(), false) to listOf(
                    RegisteredFIOName("name1@my-own-domain", Date()),
                    RegisteredFIOName("name2@my-own-domain", Date())),
            FIODomain("some-domain", Date(), false) to listOf(
                    RegisteredFIOName("name1@some-domain", Date()),
                    RegisteredFIOName("name2@some-domain", Date()),
                    RegisteredFIOName("name3@some-domain", Date()))
    )

    override fun getAllFIODomains(): List<FIODomain> =
            domains.keys.toList()

    override fun getFIONames(domain: FIODomain): List<FIOName> =
            domains[domain] ?: listOf()



    override fun getAllFIONames(): List<RegisteredFIOName> {
        TODO("Not yet implemented")
    }

    override fun getConnectedAccounts(registeredFioName: RegisteredFIOName): List<WalletAccount<*>> {
        TODO("Not yet implemented")
    }

    override fun connectAccounts(registeredFioName: RegisteredFIOName, accounts: List<WalletAccount<*>>) {
        TODO("Not yet implemented")
    }

    override fun disconnectAccounts(registeredFioName: RegisteredFIOName, accounts: List<WalletAccount<*>>) {
        TODO("Not yet implemented")
    }

    override fun getAllFIODomains(): List<FIODomain> =
            domains.keys.toList()

    override fun getFIONames(domain: FIODomain): List<RegisteredFIOName> =
            domains[domain] ?: listOf()
}