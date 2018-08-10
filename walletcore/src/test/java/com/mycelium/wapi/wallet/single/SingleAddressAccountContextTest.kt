package com.mycelium.wapi.wallet.single

import com.mrd.bitlib.model.Address
import com.mrd.bitlib.model.AddressType
import org.junit.Test


class SingleAddressAccountContextTest {
    val expectedMap = mapOf(AddressType.P2PKH to Address.fromString(""))
    @Test
    fun getAddresses() {
        TODO("Serialization and basic format should be tested as it's stored in database in plain fornat")
    }
}