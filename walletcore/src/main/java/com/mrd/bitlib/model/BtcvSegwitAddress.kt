package com.mrd.bitlib.model

import com.mycelium.wapi.wallet.Address
import com.mycelium.wapi.wallet.CommonNetworkParameters
import com.mycelium.wapi.wallet.btcvault.BTCVNetworkParameters
import com.mycelium.wapi.wallet.btcvault.BtcvAddress
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import java.io.ByteArrayOutputStream

class BtcvSegwitAddress(coinType: CryptoCurrency, networkParameters: CommonNetworkParameters,
                        var version: Int, val program: ByteArray)
    : BtcvAddress(coinType, program), Address {

    val humanReadablePart = getPrefix(networkParameters.isProdnet())

    init {
        version = (version and 0xff)
        verify(this)
    }

    override fun isValidAddress(network: NetworkParameters): Boolean {
        try {
            verify(this)
        } catch (e: SegwitAddressException) {
            return false
        }
        return humanReadablePart.equals(getPrefix(network.isProdnet), ignoreCase = true)
    }

    override fun getType(): AddressType = AddressType.P2WPKH

    override fun getAllAddressBytes(): ByteArray? {
        val pubkey = ByteArrayOutputStream(40 + 1)
        var v = version
        // OP_0 is encoded as 0x00, but OP_1 through OP_16 are encoded as 0x51 though 0x60
        if (v > 0) {
            v += 0x50
        }
        pubkey.write(v)
        pubkey.write(program, 0, program.size)
        return pubkey.toByteArray()
    }

    override fun getTypeSpecificBytes(): ByteArray = getScriptBytes(this)

    override fun getNetwork(): NetworkParameters? = getNetwork(humanReadablePart)

    companion object {

        fun getPrefix(isProdnet: Boolean) = if (isProdnet) "royale" else "troyale"

        fun getNetwork(prefix: String) =
                if (prefix.equals("royale", ignoreCase = true)) BTCVNetworkParameters.productionNetwork else BTCVNetworkParameters.testNetwork

        /**
         * Runs the SegWit address verification
         *
         * @throws SegwitAddressException on error
         */
        @Throws(SegwitAddress.SegwitAddressException::class)
        fun verify(data: BtcvSegwitAddress) {
            if (data.version > 16) {
                throw SegwitAddressException("Invalid script version")
            }
            if (data.program.size < 2 || data.program.size > 40) {
                throw SegwitAddressException("Invalid length")
            }
            // Check script length for version 0
            if (data.version.toInt() == 0 && data.program.size != 20 && data.program.size != 32) {
                throw SegwitAddressException("Invalid length for address version 0")
            }
        }

        @Throws(SegwitAddressException::class)
        private fun convertBits(`in`: ByteArray, inStart: Int, inLen: Int,
                                fromBits: Int, toBits: Int, pad: Boolean): ByteArray {
            var acc = 0
            var bits = 0
            val out = ByteArrayOutputStream(64)
            val maxv = (1 shl toBits) - 1
            val max_acc = (1 shl fromBits + toBits - 1) - 1
            for (i in 0 until inLen) {
                val value: Int = `in`[i + inStart].toInt() and 0xff
                if (value ushr fromBits != 0) {
                    throw SegwitAddressException(String.format(
                            "Input value '%X' exceeds '%d' bit size", value, fromBits))
                }
                acc = acc shl fromBits or value and max_acc
                bits += fromBits
                while (bits >= toBits) {
                    bits -= toBits
                    out.write(acc ushr bits and maxv)
                }
            }
            if (pad) {
                if (bits > 0) out.write(acc shl toBits - bits and maxv)
            } else if (bits >= fromBits || acc shl toBits - bits and maxv != 0) {
                throw SegwitAddressException("Could not convert bits, invalid padding")
            }
            return out.toByteArray()
        }


        /**
         * Decode a SegWit address.
         */
        @Throws(SegwitAddressException::class)
        fun decode(coinType: CryptoCurrency, address: String?, hrp: String? = null): BtcvSegwitAddress? {
            val dec: Bech32.Bech32Data
            dec = try {
                Bech32.decode(address)
            } catch (e: Bech32.Bech32Exception) {
                throw SegwitAddressException(e)
            }
            if (hrp != null && dec.hrp.compareTo(hrp, ignoreCase = true) != 0) {
                throw SegwitAddressException(String.format(
                        "Human-readable part expected '%s' but found '%s'", hrp, dec.hrp))
            }
            if (dec.values.size < 1) throw SegwitAddressException("Zero data found")
            // Skip the version byte and convert the rest of the decoded bytes
            val conv = convertBits(dec.values, 1, dec.values.size - 1, 5, 8, false)
            val network = if (dec.hrp.equals("royale", ignoreCase = true)) BTCVNetworkParameters.productionNetwork else BTCVNetworkParameters.testNetwork
            return BtcvSegwitAddress(coinType, network, dec.values[0].toInt(), conv)
        }

        fun getScriptBytes(data: BtcvSegwitAddress): ByteArray {
            val pubkey = ByteArrayOutputStream(40 + 1)
            var v = data.version.toInt()
            // OP_0 is encoded as 0x00, but OP_1 through OP_16 are encoded as 0x51 though 0x60
            if (v > 0) {
                v += 0x50
            }
            pubkey.write(v)
            pubkey.write(data.program.size)
            pubkey.write(data.program, 0, data.program.size)
            return pubkey.toByteArray()
        }
    }

    class SegwitAddressException : Exception {
        constructor(e: Exception?) : super(e) {}
        constructor(s: String?) : super(s) {}
    }
}


