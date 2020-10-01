package com.mycelium.wallet.activity.fio

import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.fio.FIODomain
import com.mycelium.wapi.wallet.fio.FIODomainService
import com.mycelium.wapi.wallet.fio.FIOName
import com.mycelium.wapi.wallet.fio.FIONameService
import java.util.*


object FakeService : FIODomainService, FIONameService {

    val domains = mapOf<FIODomain, List<FIOName>>(
            FIODomain("my-own-domain", Date()) to listOf(
                    FIOName("name1@my-own-domain", FIODomain("my-own-domain", Date()), Date()),
                    FIOName("name2@my-own-domain", FIODomain("my-own-domain", Date()), Date())),
            FIODomain("some-domain", Date()) to listOf(
                    FIOName("name1@some-domain", FIODomain("some-domain", Date()), Date()),
                    FIOName("name2@some-domain", FIODomain("some-domain", Date()), Date()),
                    FIOName("name3@some-domain", FIODomain("some-domain", Date()), Date()))
    )

    override fun getAllFIONames(): List<FIOName> {
        TODO("Not yet implemented")
    }

    override fun getConnectedAccounts(fioName: FIOName): List<WalletAccount<*>> {
        TODO("Not yet implemented")
    }

    override fun connectAccounts(fioName: FIOName, accounts: List<WalletAccount<*>>) {
        TODO("Not yet implemented")
    }

    override fun disconnectAccounts(fioName: FIOName, accounts: List<WalletAccount<*>>) {
        TODO("Not yet implemented")
    }

    override fun getAllFIODomains(): List<FIODomain> =
            domains.keys.toList()

    override fun getFIONames(domain: FIODomain): List<FIOName> =
            domains[domain] ?: listOf()
}