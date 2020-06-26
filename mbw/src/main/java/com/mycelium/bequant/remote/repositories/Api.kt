package com.mycelium.bequant.remote.repositories

object Api {
    val accountApi = lazy { AccountApiRepository() }
    val publicApi = lazy { PublicApiRespository() }
}