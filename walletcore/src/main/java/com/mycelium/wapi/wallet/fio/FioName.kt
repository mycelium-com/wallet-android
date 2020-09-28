package com.mycelium.wapi.wallet.fio

data class FioName(val name: String, val domain: String) {
    init {
        if ((name + domain).contains("@")) {
            throw IllegalArgumentException("name=$name, domain=$domain is not a valid FioName.")
        }
    }
}