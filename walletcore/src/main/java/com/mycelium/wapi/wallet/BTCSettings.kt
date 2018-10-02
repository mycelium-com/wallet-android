package com.mycelium.wapi.wallet

import com.mrd.bitlib.model.AddressType
import com.mycelium.wapi.wallet.bip44.ChangeAddressMode

data class BTCSettings(
        var defaultAddressType: AddressType,
        var changeAddressMode: ChangeAddressMode
) : CurrencySettings