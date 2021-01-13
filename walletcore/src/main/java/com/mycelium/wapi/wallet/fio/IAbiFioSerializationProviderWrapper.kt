package com.mycelium.wapi.wallet.fio

import fiofoundation.io.fiosdk.interfaces.ISerializationProvider

interface IAbiFioSerializationProviderWrapper {
    fun getAbiFioSerializationProvider(): ISerializationProvider
}