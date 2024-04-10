package com.mycelium.bequant.remote.repositories

import com.mycelium.wallet.external.changelly2.remote.UserRepository

object Api {
    val accountRepository by lazy { AccountApiRepository() }
    val publicRepository by lazy { PublicApiRepository() }
    val kycRepository by lazy { KYCRepository() }
    val signRepository by lazy { SignRepository() }
    val tradingRepository by lazy { TradingApiRepository() }
}