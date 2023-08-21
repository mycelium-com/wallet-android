package com.mrd.bitlib.model

/* Copyright (c) 2018 Coinomi Ltd
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
import org.junit.Assert.*
import org.junit.Test
import java.util.*

/**
 * Test from this PR https://github.com/sipa/bech32/pull/40
 */
class Bech32Test {
    @Test
    fun validChecksum() {
        for (valid in VALID_CHECKSUM) {
            val dec = Bech32.decode(valid)
            var recode = Bech32.encode(dec)
            assertEquals("Failed to roundtrip '$valid' -> '$recode'",
                    valid.toLowerCase(Locale.ROOT), recode.toLowerCase(Locale.ROOT))
            // Test encoding with an uppercase HRP
            recode = Bech32.encode(Bech32.Encoding.BECH32, dec.hrp.toUpperCase(Locale.ROOT), dec.values)
            assertEquals("Failed to roundtrip '$valid' -> '$recode'",
                    valid.toLowerCase(Locale.ROOT), recode.toLowerCase(Locale.ROOT))
        }
    }

    @Test
    fun invalidChecksum() {
        for (invalid in INVALID_CHECKSUM) {
            try {
                Bech32.decode(invalid)
                fail("Parsed an invalid code: '$invalid'")
            } catch (e: Bech32.Bech32Exception) {
                /* expected */
            }

        }
    }

    @Test
    @Throws(Bech32.Bech32Exception::class, SegwitAddress.SegwitAddressException::class)
    fun validAddress() {
        for (valid in VALID_ADDRESS) {
            assertValidAddress(valid, false)
            assertValidAddress(valid, true)
        }
    }

    @Throws(SegwitAddress.SegwitAddressException::class)
    private fun assertValidAddress(valid: AddressData, hrpUppercase: Boolean) {
        var hrp = if (hrpUppercase) "BC" else "bc"
        var dec: SegwitAddress
        try {
            dec = SegwitAddress.decode(hrp, valid.address)
        } catch (e: SegwitAddress.SegwitAddressException) {
            hrp = if (hrpUppercase) "TB" else "tb"
            dec = SegwitAddress.decode(hrp, valid.address)
        }

        val spk = SegwitAddress.getScriptBytes(dec)
        assertArrayEquals("decode produces wrong result: '${valid.address}'",
                valid.scriptPubKey, spk)
        var recode = ""
        try {
            recode = SegwitAddress.encode(hrp, dec.version.toInt(), dec.program)
        } catch (e: SegwitAddress.SegwitAddressException) {
            e.printStackTrace()
        }
        assertEquals("encode roundtrip fails: '${valid.address.toLowerCase(Locale.ROOT)}' -> '$recode'",
                valid.address.toLowerCase(Locale.ROOT), recode)
    }

    @Test
    fun invalidAddress() {
        for (invalid in INVALID_ADDRESS) {
            try {
                SegwitAddress.decode("bc", invalid)
                fail("Parsed an invalid address: '$invalid'")
            } catch (e: SegwitAddress.SegwitAddressException) { /* expected */
            }

            try {
                SegwitAddress.decode("tb", invalid)
                fail("Parsed an invalid address: '$invalid'")
            } catch (e: SegwitAddress.SegwitAddressException) { /* expected */
            }

        }
    }

    @Test
    fun invalidAddressEncoding() {
        for (invalid in INVALID_ADDRESS_ENC) {
            try {
                val code = SegwitAddress.encode(invalid.hrp, invalid.version, ByteArray(invalid.program_length))
                fail("Encode succeeds on invalid '$code'")
            } catch (e: SegwitAddress.SegwitAddressException) { /* expected */
            }

        }
    }

    @Test
    @Throws(Bech32.Bech32Exception::class)
    fun invalidHrp() {
        val program = ByteArray(20)
        for (invalidHrp in INVALID_HRP_ENC) {
            try {
                val code = SegwitAddress.encode(invalidHrp, 0, program)
                fail("Encode succeeds on invalid '$code'")
            } catch (e: SegwitAddress.SegwitAddressException) { /* expected */
            }

        }
    }

    private class AddressData internal constructor(internal val address: String, scriptPubKeyHex: String) {
        internal val scriptPubKey: ByteArray

        init {
            // Convert hex to bytes, does minimal error checking
            val len = scriptPubKeyHex.length
            scriptPubKey = ByteArray(len / 2)
            var i = 0
            while (i < len) {
                scriptPubKey[i / 2] = ((Character.digit(scriptPubKeyHex[i], 16) shl 4) + Character.digit(scriptPubKeyHex[i + 1], 16)).toByte()
                i += 2
            }
        }
    }

    private class InvalidAddressData internal constructor(internal val hrp: String, internal val version: Int, internal val program_length: Int)

    companion object {
        // test vectors
        // https://github.com/bitcoin/bitcoin/blob/723f1c669f9b6babfcb789b7b10bb98d1341da60/test/functional/rpc_validateaddress.py#L45

        private val VALID_CHECKSUM = arrayOf("A12UEL5L", "an83characterlonghumanreadablepartthatcontainsthenumber1andtheexcludedcharactersbio1tt5tgs", "abcdef1qpzry9x8gf2tvdw0s3jn54khce6mua7lmqqqxw", "11qqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqc8247j", "split1checkupstagehandshakeupstreamerranterredcaperred2y9e3w")
        private val INVALID_CHECKSUM = arrayOf(" 1nwldj5", String(charArrayOf(0x7f.toChar())) + "1axkwrx", "an84characterslonghumanreadablepartthatcontainsthenumber1andtheexcludedcharactersbio1569pvx", "pzry9x0s0muk", "1pzry9x0s0muk", "x1b4n0q5v", "li1dgmt3", "de1lg7wt" + String(charArrayOf(0xff.toChar())))

        private val VALID_ADDRESS = arrayOf(
            AddressData(
                "BC1QW508D6QEJXTDG4Y5R3ZARVARY0C5XW7KV8F3T4",
                "0014751e76e8199196d454941c45d1b3a323f1433bd6"
            ),
            AddressData(
                "tb1qrp33g0q5c5txsp9arysrx4k6zdkfs4nce4xj0gdcccefvpysxf3q0sl5k7",
                "00201863143c14c5166804bd19203356da136c985678cd4d27a1b8c6329604903262"
            ),
            AddressData("BC1SW50QA3JX3S", "6002751e"),
            AddressData(
                "bc1zw508d6qejxtdg4y5r3zarvaryvg6kdaj",
                "5210751e76e8199196d454941c45d1b3a323"
            ),
            AddressData(
                "tb1qqqqqp399et2xygdj5xreqhjjvcmzhxw4aywxecjdzew6hylgvsesrxh6hy",
                "0020000000c4a5cad46221b2a187905e5266362b99d5e91c6ce24d165dab93e86433"
            ),
            AddressData(
                "bc1qrp33g0q5c5txsp9arysrx4k6zdkfs4nce4xj0gdcccefvpysxf3qccfmv3",
                "00201863143c14c5166804bd19203356da136c985678cd4d27a1b8c6329604903262"
            ),
            AddressData(
                "bc1pw508d6qejxtdg4y5r3zarvary0c5xw7kw508d6qejxtdg4y5r3zarvary0c5xw7kt5nd6y",
                "5128751e76e8199196d454941c45d1b3a323f1433bd6751e76e8199196d454941c45d1b3a323f1433bd6"
            ),
//            AddressData("BC1SW50QGDZ25J", "6002751e"),
//            AddressData(
//                "bc1zw508d6qejxtdg4y5r3zarvaryvaxxpcs",
//                "5210751e76e8199196d454941c45d1b3a323"
//            ),
            AddressData(
                "bc1qqqqqp399et2xygdj5xreqhjjvcmzhxw4aywxecjdzew6hylgvses5wp4dt",
                "0020000000c4a5cad46221b2a187905e5266362b99d5e91c6ce24d165dab93e86433",
            ),
            AddressData(
                "bc1pqqqqp399et2xygdj5xreqhjjvcmzhxw4aywxecjdzew6hylgvses7epu4h",
                "5120000000c4a5cad46221b2a187905e5266362b99d5e91c6ce24d165dab93e86433",
            ),
            AddressData(
                "bc1p0xlxvlhemja6c4dqv22uapctqupfhlxm9h8z3k2e72q4k9hcz7vqzk5jj0",
                "512079be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798",
            )
        )

        private val INVALID_ADDRESS = arrayOf(
            "tc1qw508d6qejxtdg4y5r3zarvary0c5xw7kg3g4ty",
            "bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t5",
            "BC13W508D6QEJXTDG4Y5R3ZARVARY0C5XW7KN40WF2",
            "bc1rw5uspcuh",
            "bc10w508d6qejxtdg4y5r3zarvary0c5xw7kw508d6qejxtdg4y5r3zarvary0c5xw7kw5rljs90",
            "BC1QR508D6QEJXTDG4Y5R3ZARVARYV98GJ9P",
            "tb1qrp33g0q5c5txsp9arysrx4k6zdkfs4nce4xj0gdcccefvpysxf3q0sL5k7",
            "BC1QW508D6QEJXTDG4Y5R3ZARVARY0C5XW7KV8F3t4", // new
            "bc1zw508d6qejxtdg4y5r3zarvaryvqyzf3du",
            "tb1qrp33g0q5c5txsp9arysrx4k6zdkfs4nce4xj0gdcccefvpysxf3pjxtptv",
            "bc1gmk9yu",
            // taproot
            "tc1p0xlxvlhemja6c4dqv22uapctqupfhlxm9h8z3k2e72q4k9hcz7vq5zuyut",
//            "bc1p0xlxvlhemja6c4dqv22uapctqupfhlxm9h8z3k2e72q4k9hcz7vqh2y7hd",
//            "tb1z0xlxvlhemja6c4dqv22uapctqupfhlxm9h8z3k2e72q4k9hcz7vqglt7rf",
//            "BC1S0XLXVLHEMJA6C4DQV22UAPCTQUPFHLXM9H8Z3K2E72Q4K9HCZ7VQ54WELL",
//            "bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kemeawh",
//            "tb1q0xlxvlhemja6c4dqv22uapctqupfhlxm9h8z3k2e72q4k9hcz7vq24jc47",
            "bc1p38j9r5y49hruaue7wxjce0updqjuyyx0kh56v8s25huc6995vvpql3jow4",
            "BC130XLXVLHEMJA6C4DQV22UAPCTQUPFHLXM9H8Z3K2E72Q4K9HCZ7VQ7ZWS8R",
            "bc1pw5dgrnzv",
            "bc1p0xlxvlhemja6c4dqv22uapctqupfhlxm9h8z3k2e72q4k9hcz7v8n0nx0muaewav253zgeav",
            "BC1QR508D6QEJXTDG4Y5R3ZARVARYV98GJ9P",
            "tb1p0xlxvlhemja6c4dqv22uapctqupfhlxm9h8z3k2e72q4k9hcz7vq47Zagq",
            "bc1p0xlxvlhemja6c4dqv22uapctqupfhlxm9h8z3k2e72q4k9hcz7v07qwwzcrf",
            "tb1p0xlxvlhemja6c4dqv22uapctqupfhlxm9h8z3k2e72q4k9hcz7vpggkg4j",
            "bc1gmk9yu"
        )
        private val INVALID_ADDRESS_ENC = arrayOf(InvalidAddressData("bc", 0, 21), InvalidAddressData("bc", 17, 32), InvalidAddressData("bc", 1, 1), InvalidAddressData("bc", 16, 41))
        private val INVALID_HRP_ENC = arrayOf("café", "μπίτκοιν", "бит", "コイン")
    }
}