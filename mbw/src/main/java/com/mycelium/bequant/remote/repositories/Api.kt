package com.mycelium.bequant.remote.repositories

object Api {
    val accountApi by lazy { AccountApiRepository() }
    val publicApi by lazy { PublicApiRespository() }
}