package com.mycelium.bequant.remote.repositories

object Api {
    val accountApi by lazy { AccountApiRepository() }
    val publicApi by lazy { PublicApiRespository() }
    val apiRepository by lazy { ApiRepository() }
    val kycRepository by lazy { KYCRepository() }
    val signRepository by lazy { SignRepository() }
}