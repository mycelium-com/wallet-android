package com.mrd.bitlib.model

import com.mrd.bitlib.util.HexUtils
import org.junit.Assert
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

class AddressTypeTest {
    @Test
    fun testSerialisation() {
        val byteStream = ByteArrayOutputStream()
        val addressType = AddressType.P2SH_P2WPKH
        ObjectOutputStream(byteStream).use { objectOutputStream ->
            objectOutputStream.writeObject(addressType)
            Assert.assertEquals(serializedType,
                    HexUtils.toHex(byteStream.toByteArray()))
        }
    }

    @Test
    fun testDeserialization() {
        val addressType = AddressType.P2SH_P2WPKH

        val byteStream = ByteArrayInputStream(serializedTypeBytes)

        ObjectInputStream(byteStream).use { objectInputStream ->
            val addressTypeDecoded = objectInputStream.readObject()
                    as AddressType
            Assert.assertEquals(addressType, addressTypeDecoded)
        }
    }

    companion object {
        private const val serializedType = "aced00057e720020636f6d2e6d72642e6269746c69622e6d6f64656c2e416464726573735479706500000000000000001200007872000e6a6176612e6c616e672e456e756d0000000000000000120000787074000b503253485f503257504b48"
        private val serializedTypeBytes = HexUtils.toBytes(serializedType)
    }

}