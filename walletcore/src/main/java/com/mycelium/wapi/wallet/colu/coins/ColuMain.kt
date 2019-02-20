package com.mycelium.wapi.wallet.colu.coins

import com.mrd.bitlib.model.Address
import com.mrd.bitlib.model.AddressType
import com.mycelium.wapi.wallet.GenericAddress
import com.mycelium.wapi.wallet.btc.BtcAddress
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.coins.SoftDustPolicy
import com.mycelium.wapi.wallet.coins.families.BitcoinBasedCryptoCurrency
import com.mycelium.wapi.wallet.exceptions.AddressMalformedException


abstract class ColuMain : BitcoinBasedCryptoCurrency() {

    init {
//        id = "colu.main"

        addressHeader = 0
        p2shHeader = 5
        acceptableAddressCodes = intArrayOf(addressHeader, p2shHeader)
        spendableCoinbaseDepth = 100
        dumpedPrivateKeyHeader = 128

        feeValue = value(12000)
        minNonDust = value(5460)
        softDustLimit = value(1000000) // 0.01 BTC
        softDustPolicy = SoftDustPolicy.AT_LEAST_BASE_FEE_IF_SOFT_DUST_TXO_PRESENT
        signedMessageHeader = CryptoCurrency.toBytes("Colu Signed Message:\n")
    }

    override fun getName(): String = name

    override fun getSymbol(): String = symbol

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is ColuMain) return false
        return id == other.id
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }

    @Throws(AddressMalformedException::class)
    override fun parseAddress(addressString: String): GenericAddress? {
        val address = Address.fromString(addressString) ?: return null

        if (address.type === AddressType.P2WPKH)
            throw AddressMalformedException("Address $addressString is malformed")

        return BtcAddress(this, address)
    }
}
