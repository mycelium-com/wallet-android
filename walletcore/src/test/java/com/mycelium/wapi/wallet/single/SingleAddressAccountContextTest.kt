package com.mycelium.wapi.wallet.single

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.mrd.bitlib.model.AddressType
import com.mrd.bitlib.model.BitcoinAddress
import com.mycelium.wapi.wallet.btc.single.SingleAddressAccountContext
import org.junit.Assert
import org.junit.Test
import java.util.*

class SingleAddressAccountContextTest {
    private val gson = GsonBuilder().create()

    @Test
    fun testAddressMapSerialization() {
        val actual = gson.toJson(context.addresses.values.map { it.toString() }.toList())
        Assert.assertEquals(serialized, actual)
    }

    @Test
    fun testAddressMapDeserialization() {
        val type = object : TypeToken<Collection<String?>?>() {}.type
        val addressStringsList: Collection<String> = gson.fromJson<Collection<String>>(serialized, type)
        Assert.assertEquals(3, addressStringsList.size)
        addressStringsList.forEach {
            Assert.assertNotNull(BitcoinAddress.fromString(it))
        }
    }

    companion object {
        private val uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440000")

        private val addressMap = mapOf(
                AddressType.P2PKH to BitcoinAddress.fromString("1AKDDsfTh8uY4X3ppy1m7jw1fVMBSMkzjP"),
                AddressType.P2SH_P2WPKH to BitcoinAddress.fromString("34nSkinWC9rDDJiUY438qQN1JHmGqBHGW7"),
                AddressType.P2WPKH to BitcoinAddress.fromString("bc1q6s8n723dztd7ch2uy8xa76ux766gfwg05ydlt6"))

        private val context = SingleAddressAccountContext(uuid, addressMap, false, 0, AddressType.P2SH_P2WPKH)

        private const val serialized =
            """["1AKDDsfTh8uY4X3ppy1m7jw1fVMBSMkzjP","34nSkinWC9rDDJiUY438qQN1JHmGqBHGW7","bc1q6s8n723dztd7ch2uy8xa76ux766gfwg05ydlt6"]"""
    }
}