package com.mycelium.wapi.wallet.fio.coins

import com.mycelium.wapi.wallet.Address
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.fio.FioAddress
import com.mycelium.wapi.wallet.fio.FioAddressData
import com.mycelium.wapi.wallet.fio.FioAddressSubtype
import fiofoundation.io.fiosdk.isFioAddress
import fiofoundation.io.fiosdk.isFioPublicKey

fun String.isFioActor(): Boolean =
        isNotEmpty()
                && length == 12
                && Regex("^[.a-z1-5]+$").matchEntire(this) != null

fun String.isFioDomain(): Boolean =
        isNotEmpty()
                && length in 1..62
                && Regex("^[a-zA-Z0-9](?:(?!-{2,}))[a-zA-Z0-9-]*(?:(?<!-))$").matchEntire(this) != null

abstract class FIOToken(id: String, name: String) : CryptoCurrency(id, name, "FIO", 9, 2, false) {
    override fun parseAddress(addressString: String?): Address? {
        return when {
            addressString == null -> null
            addressString.isFioPublicKey() -> FioAddress(this, FioAddressData(addressString))
            addressString.isFioAddress() -> FioAddress(this, FioAddressData(addressString),
                    FioAddressSubtype.ADDRESS)
            addressString.isFioActor() -> FioAddress(this, FioAddressData(addressString),
                    FioAddressSubtype.ACTOR)
            else -> null
        }
    }
}