package com.mycelium.wapi.wallet.btc

import com.mrd.bitlib.model.hdpath.HdKeyPath
import com.mycelium.wapi.wallet.GenericAddress

interface BtcAddress : GenericAddress {
    val bip32Path: HdKeyPath
}
