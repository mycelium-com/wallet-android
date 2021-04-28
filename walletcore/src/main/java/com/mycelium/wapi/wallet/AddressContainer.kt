package com.mycelium.wapi.wallet

import com.mrd.bitlib.model.AddressType


interface AddressContainer {
    val availableAddressTypes: List<AddressType>

    fun setDefaultAddressType(addressType: AddressType)
}