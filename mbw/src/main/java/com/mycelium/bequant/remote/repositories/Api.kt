package com.mycelium.bequant.remote.repositories

object Api {
    val accountRepository by lazy { AccountApiRepository() }
    val publicRepository by lazy { PublicApiRepository() }
    val kycRepository by lazy { KYCRepository() }
    val signRepository by lazy { SignRepository() }
    val tradingRepository by lazy { TradingApiRepository() }
    val userRepository by lazy { UserRepository() }
}