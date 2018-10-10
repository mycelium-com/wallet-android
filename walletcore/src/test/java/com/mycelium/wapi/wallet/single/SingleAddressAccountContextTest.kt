package com.mycelium.wapi.wallet.single

import com.mrd.bitlib.model.Address
import com.mrd.bitlib.model.AddressType
import com.mrd.bitlib.util.HexUtils
import org.junit.Assert
import org.junit.Test
import java.io.*
import java.util.*

class SingleAddressAccountContextTest {
    @Test
    fun testAddressMapSerialization() {
        val byteStream = ByteArrayOutputStream()
        ObjectOutputStream(byteStream).use {
            it.writeObject(context.addresses)
        }
        Assert.assertEquals(serializedAddressMap, HexUtils.toHex(byteStream.toByteArray()))
    }

    @Test
    fun testAddressMapDeserialization() {
        val byteStream = ByteArrayInputStream(serializedAddressMapBytes)
        val addresses = ObjectInputStream(byteStream).use {
            it.readObject()
        }
        Assert.assertEquals(addressMap, addresses)
    }

    companion object {
        private val uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440000")

        private val addressMap = mapOf(
                AddressType.P2PKH to Address.fromString("1AKDDsfTh8uY4X3ppy1m7jw1fVMBSMkzjP"),
                AddressType.P2SH_P2WPKH to Address.fromString("34nSkinWC9rDDJiUY438qQN1JHmGqBHGW7"),
                AddressType.P2WPKH to Address.fromString("bc1q6s8n723dztd7ch2uy8xa76ux766gfwg05ydlt6"))

        private val context = SingleAddressAccountContext(uuid, addressMap, false, 0, AddressType.P2SH_P2WPKH)

        private const val serializedAddressMap = "aced0005737200176a6176612e7574696c2e4c696e6b6564486173684d617034c04e5c106cc0fb0200015a000b6163636573734f72646572787200116a6176612e7574696c2e486173684d61700507dac1c31660d103000246000a6c6f6164466163746f724900097468726573686f6c6478703f40000000000003770800000004000000037e720020636f6d2e6d72642e6269746c69622e6d6f64656c2e416464726573735479706500000000000000001200007872000e6a6176612e6c616e672e456e756d000000000000000012000078707400055032504b487372001c636f6d2e6d72642e6269746c69622e6d6f64656c2e4164647265737300000000000000010200044c00085f616464726573737400124c6a6176612f6c616e672f537472696e673b5b00065f62797465737400025b424c00096269703332506174687400274c636f6d2f6d72642f6269746c69622f6d6f64656c2f6864706174682f48644b6579506174683b4c000a736372697074486173687400204c636f6d2f6d72642f6269746c69622f7574696c2f536861323536486173683b787070757200025b42acf317f8060854e002000078700000001500662ad25db00e7bb38bc04831ae48b4b446d1269870707e71007e000374000b503253485f503257504b487371007e0007707571007e000d000000150521ef2f4b1ea1f9ed09c1128d1ebb61d4729ca7d670707e71007e0003740006503257504b4873720022636f6d2e6d72642e6269746c69622e6d6f64656c2e53656777697441646472657373000000000000000102000342000776657273696f6e4c000368727071007e00085b000770726f6772616d71007e00097871007e0007707571007e000d00000014d40f3f2a2d12dbec5d5c21cddf6b86f6b484b90f707000740002424371007e00177800"
        private val serializedAddressMapBytes = HexUtils.toBytes(serializedAddressMap)
    }
}