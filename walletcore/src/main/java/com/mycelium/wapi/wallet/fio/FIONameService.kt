package com.mycelium.wapi.wallet.fio

import com.fasterxml.jackson.annotation.JsonProperty
import com.mycelium.wapi.wallet.WalletAccount
import java.io.Serializable
import java.util.*

data class RegisteredFIOName(@JsonProperty("name") val name: String,
                             @JsonProperty("expireDate") val expireDate: Date) : Serializable

data class FIODomain(@JsonProperty("domain") val domain: String,
                     @JsonProperty("expireDate") val expireDate: Date,
                     @JsonProperty("isPublic") val isPublic: Boolean) : Serializable

interface FIONameService {
    fun getAllFIONames(): List<RegisteredFIOName>

    fun getConnectedAccounts(registeredFioName: RegisteredFIOName): List<WalletAccount<*>>

    fun connectAccounts(registeredFioName: RegisteredFIOName, accounts: List<WalletAccount<*>>)

    fun disconnectAccounts(registeredFioName: RegisteredFIOName, accounts: List<WalletAccount<*>>)
}

interface FIODomainService {
    fun getAllFIODomains(): List<FIODomain>

    fun getFIONames(domain: FIODomain): List<RegisteredFIOName>
}