package com.mycelium.wapi.wallet

import com.mrd.bitlib.util.HexUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfigObfuscationTest {
    private val iv = HexUtils.toBytes("000102030405060708090a0b0c0d0e0f")

    @Test
    fun textRoundTrip() {
        val enc = ConfigObfuscation.encodeText("12345678-abcd-4ef0-9876-0123456789ab", iv)
        assertTrue(enc.startsWith("enc2_"))
        assertEquals("12345678-abcd-4ef0-9876-0123456789ab", ConfigObfuscation.decode(enc))
    }

    @Test
    fun bytesRoundTrip() {
        val enc = ConfigObfuscation.encodeBytes(HexUtils.toBytes("deadbeef00"), iv)
        assertTrue(enc.startsWith("enc1_"))
        assertEquals("deadbeef00", ConfigObfuscation.decode(enc))
    }

    @Test
    fun deterministicWithFixedIv() {
        // openssl enc -aes-128-ctr -K a8105c3c8b75556a9099b8dcab9cc133 -iv 000102…0f -nosalt <<< "hello"
        assertEquals("enc2_000102030405060708090a0b0c0d0e0f" + "a36387b511", ConfigObfuscation.encodeText("hello", iv))
    }

    @Test
    fun plainAndBrokenValuesPassThrough() {
        assertNull(ConfigObfuscation.decode(null))
        assertEquals("", ConfigObfuscation.decode(""))
        assertEquals("plain-key", ConfigObfuscation.decode("plain-key"))
        assertEquals("enc2_zz", ConfigObfuscation.decode("enc2_zz"))          // not hex
        assertEquals("enc2_0011", ConfigObfuscation.decode("enc2_0011"))      // shorter than IV
    }

    @Test
    fun compatibleWithWalletKmpConfigValue() {
        // TRON-PRO-API-KEY value from WalletKMP wallet-config-prodnet.json; must decode to a UUID-shaped key
        val kmp = "enc2_9eb1e5bd16543bb79ebed62e65ff94f9d16dca9f8c3d8837780ff13b6e292d7f0301d06ba9016089e558a37164ffceaa8c9d312a"
        val key = ConfigObfuscation.decode(kmp)!!
        assertTrue(key, Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}").matches(key))
    }
}
