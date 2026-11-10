package com.mycelium.wapi.wallet.tron

import com.mrd.bitlib.bitcoinj.Base58
import com.mrd.bitlib.model.hdpath.HdKeyPath
import com.mrd.bitlib.util.HexUtils
import com.mycelium.wapi.wallet.Address
import com.mycelium.wapi.wallet.coins.CryptoCurrency

/**
 * TRON base58check address ("T..."): 0x41 prefix + 20 bytes of keccak256(pubkey)[12..32) + 4 byte checksum.
 */
class TronAddress(cryptoCurrency: CryptoCurrency, val addressString: String) : Address {
    override val coinType = cryptoCurrency

    override fun getSubType() = "Tron"

    /** Raw 21-byte on-chain form (0x41 + 20 bytes). */
    override fun getBytes(): ByteArray = decode(addressString)
        ?: throw IllegalArgumentException("Invalid TRON address $addressString")

    /** 20-byte EVM-style address (without the 0x41 prefix). */
    val evmBytes: ByteArray
        get() = getBytes().copyOfRange(1, DECODED_SIZE)

    /** 41-prefixed hex form used by the TronGrid HTTP API when visible=false. */
    val hexString: String
        get() = HexUtils.toHex(getBytes())

    override fun toString() = addressString

    override fun equals(other: Any?): Boolean {
        if (other !is TronAddress) return false
        return addressString == other.addressString
    }

    override fun hashCode(): Int = addressString.hashCode()

    private var _bip32Path: HdKeyPath? = null
    override fun getBip32Path(): HdKeyPath? = _bip32Path
    override fun setBip32Path(bip32Path: HdKeyPath?) {
        _bip32Path = bip32Path
    }

    companion object {
        const val ADDRESS_PREFIX = 0x41.toByte()
        const val DECODED_SIZE = 21

        fun getDummyAddress(cryptoCurrency: CryptoCurrency) =
            TronAddress(cryptoCurrency, "TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t")

        /** base58check-decodes [address]; null when the checksum/prefix/length is wrong. */
        fun decode(address: String): ByteArray? {
            val decoded = try {
                Base58.decodeChecked(address)
            } catch (e: Exception) {
                null
            } ?: return null
            return if (decoded.size == DECODED_SIZE && decoded[0] == ADDRESS_PREFIX) decoded else null
        }

        /** Encodes the raw 21-byte (0x41 + 20) form to the base58check "T..." string. */
        fun encode(raw21: ByteArray): String {
            require(raw21.size == DECODED_SIZE && raw21[0] == ADDRESS_PREFIX) { "Invalid raw TRON address" }
            return Base58.encodeWithChecksum(raw21)
        }

        /** Encodes a 41-prefixed hex string (as returned by TronGrid) to base58; "" stays "". */
        fun fromHex(hex: String): String {
            if (hex.isBlank()) return ""
            val clean = hex.removePrefix("0x")
            val bytes = try {
                HexUtils.toBytes(clean)
            } catch (e: Exception) {
                return hex
            }
            return if (bytes.size == DECODED_SIZE && bytes[0] == ADDRESS_PREFIX) encode(bytes) else hex
        }

        fun isValid(address: String): Boolean = decode(address) != null
    }
}
