package com.mycelium.wapi.wallet.manager

import com.mrd.bitlib.crypto.PublicKey
import com.mrd.bitlib.model.AddressType
import com.mrd.bitlib.model.hdpath.HdKeyPath
import com.mycelium.wapi.wallet.Address


interface AddressFactory<ADDRESS>
        where ADDRESS : Address {
    fun getAddress(publicKey: PublicKey, addressType: AddressType): ADDRESS?

    fun bytesToAddress(bytes: ByteArray, path: HdKeyPath? = null): ADDRESS?

    fun addressToBytes(address: ADDRESS): ByteArray
}