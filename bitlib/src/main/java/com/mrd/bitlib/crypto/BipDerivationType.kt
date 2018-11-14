package com.mrd.bitlib.crypto

import com.mrd.bitlib.model.Address
import com.mrd.bitlib.model.AddressType
import java.io.Serializable

enum class BipDerivationType(val purpose: Byte, val addressType: AddressType) : Serializable {
    BIP44(44, AddressType.P2PKH),
    BIP49(49, AddressType.P2SH_P2WPKH),
    BIP84(84, AddressType.P2WPKH);

    fun getHardenedPurpose() = purpose + 0x80000000.toInt()

    companion object {
        fun getDerivationTypeByAddress(address: Address): BipDerivationType {
            return getDerivationTypeByAddressType(address.type)
        }

        fun getDerivationTypeByAddressType(addressType: AddressType): BipDerivationType {
            return when (addressType) {
                AddressType.P2PKH -> BIP44
                AddressType.P2SH_P2WPKH -> BIP49
                AddressType.P2WPKH -> BIP84
                else -> throw IllegalArgumentException("Unsupported derivation scheme")
            }
        }
    }
}