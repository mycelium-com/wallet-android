package com.mycelium.wallet.activity.send.model

import com.mycelium.wapi.wallet.GenericAddress

data class AddressItem(val address: GenericAddress?, val addressType: String?, val addressTypeLabel: String?, val type: Int)