package com.mycelium.wapi.wallet.fio

import fiofoundation.io.fiosdk.models.fionetworkprovider.FIORequestContent

class FioGroup(var status: Type, val children: MutableList<FIORequestContent>) {
    enum class Type(private val s: String) {
        SENT("PAID FIO REQUESTS"),
        PENDING("PENDING FIO REQUESTS");

        override fun toString(): String {
            return s
        }
    }
}