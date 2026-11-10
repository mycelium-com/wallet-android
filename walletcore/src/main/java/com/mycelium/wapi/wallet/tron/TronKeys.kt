package com.mycelium.wapi.wallet.tron

import com.mrd.bitlib.util.HexUtils
import org.web3j.crypto.ECKeyPair
import org.web3j.crypto.Hash
import org.web3j.crypto.Keys
import org.web3j.crypto.Sign
import org.web3j.utils.Numeric
import java.math.BigInteger

/**
 * secp256k1 helpers for TRON: address derivation and raw-hash signing, built on the web3j
 * primitives already used for Ethereum (same curve, same keccak address scheme; TRON only
 * changes the encoding: 0x41 prefix + base58check).
 */
object TronKeys {
    /** Base58check "T..." address for [keyPair]. */
    fun addressOf(keyPair: ECKeyPair): String = addressOf(keyPair.publicKey)

    fun addressOf(publicKey: BigInteger): String {
        // Keys.getAddress = last 20 bytes of keccak256(uncompressed pubkey without 0x04)
        val evm = HexUtils.toBytes(Keys.getAddress(publicKey))
        val raw = ByteArray(TronAddress.DECODED_SIZE)
        raw[0] = TronAddress.ADDRESS_PREFIX
        System.arraycopy(evm, 0, raw, 1, 20)
        return TronAddress.encode(raw)
    }

    /**
     * Signs a 32-byte hash (the txID = sha256(raw_data)) and returns the 65-byte
     * r||s||recId signature TRON expects (recId is 0/1; java-tron also accepts 27/28).
     */
    fun sign(hash: ByteArray, keyPair: ECKeyPair): ByteArray {
        require(hash.size == 32) { "TRON signs a 32-byte hash" }
        val sig = Sign.signMessage(hash, keyPair, false)
        val out = ByteArray(65)
        System.arraycopy(sig.r, 0, out, 0, 32)
        System.arraycopy(sig.s, 0, out, 32, 32)
        val v = sig.v[0].toInt() and 0xff
        out[64] = (if (v >= 27) v - 27 else v).toByte()
        return out
    }

    /** TIP-191 message signing (TronLink / TronWeb signMessageV2 format), hex output. */
    fun signMessage(message: String, keyPair: ECKeyPair): String {
        val msgBytes = message.toByteArray(Charsets.UTF_8)
        val prefix = (TIP191_PREFIX + msgBytes.size).toByteArray(Charsets.UTF_8)
        val hash = Hash.sha3(prefix + msgBytes)
        val sig = Sign.signMessage(hash, keyPair, false)
        return Numeric.toHexStringNoPrefix(sig.r) + Numeric.toHexStringNoPrefix(sig.s) +
                HexUtils.toHex(sig.v)
    }

    private const val TIP191_PREFIX = "TRON Signed Message:\n"
}
