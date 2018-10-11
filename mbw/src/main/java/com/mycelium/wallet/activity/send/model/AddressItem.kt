package com.mycelium.wallet.activity.send.model

import com.mrd.bitlib.model.Address

data class AddressItem(val address: Address?, val addressType: String?, val type: Int)