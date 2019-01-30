package com.mycelium.wapi.content

import com.mycelium.wapi.wallet.GenericAddress
import com.mycelium.wapi.wallet.coins.Value
import java.io.Serializable


abstract class GenericAssetUri(val address: GenericAddress?, val value: Value?,
                               val label: String?, val scheme: String?)
    : Serializable {
    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (this === other) return true
        if (other !is GenericAssetUri) return false
        return address == other.address && value == other.value && label == other.label && scheme == other.scheme
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + (address?.hashCode() ?: 0)
        result = 31 * result + (value?.hashCode() ?: 0)
        result = 31 * result + (label?.hashCode() ?: 0)
        result = 31 * result + (scheme?.hashCode() ?: 0)
        return result
    }

    override fun toString() = "GenericAssetUri(address=$address, value=$value, label=$label, scheme=$scheme)"
}

class PrivateKeyUri(val keyString: String, label: String?, scheme: String) : GenericAssetUri(null, null, label, scheme)
