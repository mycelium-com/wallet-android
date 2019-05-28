package com.mycelium.wapi.wallet.colu

import com.mrd.bitlib.crypto.InMemoryPrivateKey
import com.mycelium.wapi.wallet.KeyCipher
import com.mycelium.wapi.wallet.btc.BtcAddress
import com.mycelium.wapi.wallet.btc.single.AddressSingleConfig
import com.mycelium.wapi.wallet.btc.single.PrivateSingleConfig
import com.mycelium.wapi.wallet.colu.coins.ColuMain

class AddressColuConfig @JvmOverloads constructor(address: BtcAddress, val coinType: ColuMain? = null)
    : AddressSingleConfig(address)

class PrivateColuConfig @JvmOverloads constructor(privateKey: InMemoryPrivateKey, val coinType: ColuMain? = null, cipher: KeyCipher)
    : PrivateSingleConfig(privateKey, cipher)
