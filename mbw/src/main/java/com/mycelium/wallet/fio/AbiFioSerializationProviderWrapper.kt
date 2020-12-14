package com.mycelium.wallet.fio

import com.mycelium.wapi.wallet.fio.IAbiFioSerializationProviderWrapper
import fiofoundation.io.androidfioserializationprovider.AbiFIOSerializationProvider

class AbiFioSerializationProviderWrapper : IAbiFioSerializationProviderWrapper {
    override fun getAbiFioSerializationProvider() = AbiFIOSerializationProvider()
}