package com.mycelium.wallet.external.changelly2.remote

import com.mycelium.wallet.external.changelly2.remote.UserRepository

object Api {
    val userRepository by lazy { UserRepository() }
}