package com.mycelium.wapi.wallet.btc.single

import com.mrd.bitlib.crypto.InMemoryPrivateKey
import com.mrd.bitlib.crypto.PublicKey
import com.mrd.bitlib.model.Address
import com.mycelium.wapi.wallet.GenericAddress
import com.mycelium.wapi.wallet.KeyCipher
import com.mycelium.wapi.wallet.btc.BtcAddress
import com.mycelium.wapi.wallet.manager.Config

open class AddressSingleConfig(val address: BtcAddress) : Config

open class PublicSingleConfig(val publicKey: PublicKey) : Config

open class PrivateSingleConfig(val privateKey: InMemoryPrivateKey, val cipher: KeyCipher) : Config

