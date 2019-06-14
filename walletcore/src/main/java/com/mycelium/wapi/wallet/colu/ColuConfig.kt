package com.mycelium.wapi.wallet.colu

import com.mrd.bitlib.crypto.InMemoryPrivateKey
import com.mycelium.wapi.wallet.KeyCipher
import com.mycelium.wapi.wallet.btc.BtcAddress
import com.mycelium.wapi.wallet.colu.coins.ColuMain
import com.mycelium.wapi.wallet.manager.Config

class AddressColuConfig(val address: BtcAddress, val coinType: ColuMain) : Config
class PrivateColuConfig(val privateKey: InMemoryPrivateKey, val coinType: ColuMain, val cipher: KeyCipher) : Config
