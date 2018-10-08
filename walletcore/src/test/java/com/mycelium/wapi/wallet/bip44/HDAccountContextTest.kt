package com.mycelium.wapi.wallet.bip44

import com.mrd.bitlib.crypto.BipDerivationType
import com.mrd.bitlib.util.HexUtils
import org.junit.Assert
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream


class HDAccountContextTest {
    @Test
    fun testIndexesSerialization() {
        val indexesMap = BipDerivationType.values().map { it to AccountIndexesContext(-1, -1, 0) }
                .toMap()

        val byteStream = ByteArrayOutputStream()
        ObjectOutputStream(byteStream).use { objectOutputStream ->
            objectOutputStream.writeObject(indexesMap)
            Assert.assertEquals(serializedMap, HexUtils.toHex(byteStream.toByteArray()))
        }
    }

    @Test
    fun testIndexesDeserialization() {
        val indexesMap = BipDerivationType.values().map { it to AccountIndexesContext(-1, -1, 0) }
                .toMap()

        val byteStream = ByteArrayInputStream(serializedMapBytes)
        var indexesContextMap: Map<BipDerivationType, AccountIndexesContext>? = null
        ObjectInputStream(byteStream).use { objectInputStream ->
            indexesContextMap = objectInputStream.readObject()
                    as Map<BipDerivationType, AccountIndexesContext>
        }
        Assert.assertEquals(indexesMap, indexesContextMap)
    }

    companion object {
        private const val serializedMap = "aced0005737200176a6176612e7574696c2e4c696e6b6564486173684d617034c04e5c106cc0fb0200015a000b6163636573734f72646572787200116a6176612e7574696c2e486173684d61700507dac1c31660d103000246000a6c6f6164466163746f724900097468726573686f6c6478703f40000000000003770800000004000000037e720027636f6d2e6d72642e6269746c69622e63727970746f2e42697044657269766174696f6e5479706500000000000000001200007872000e6a6176612e6c616e672e456e756d00000000000000001200007870740005424950343473720034636f6d2e6d7963656c69756d2e776170692e77616c6c65742e62697034342e4163636f756e74496e6465786573436f6e74657874255afb40713c097e02000349001b66697273744d6f6e69746f726564496e7465726e616c496e64657849001d6c61737445787465726e616c496e64657857697468416374697669747949001d6c617374496e7465726e616c496e646578576974684163746976697479787000000000ffffffffffffffff7e71007e000374000542495034397371007e000700000000ffffffffffffffff7e71007e000374000542495038347371007e000700000000ffffffffffffffff7800"
        private val serializedMapBytes = HexUtils.toBytes(serializedMap)
    }
}