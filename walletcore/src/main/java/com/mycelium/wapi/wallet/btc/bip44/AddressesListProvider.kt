package com.mycelium.wapi.wallet.btc.bip44

import com.mrd.bitlib.model.BitcoinAddress

interface AddressesListProvider<T : BitcoinAddress> {
    fun addressesList(): List<T>
}
