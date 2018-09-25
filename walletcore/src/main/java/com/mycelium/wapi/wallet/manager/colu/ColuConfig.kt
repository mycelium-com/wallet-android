package com.mycelium.wapi.wallet.manager.colu

import com.mrd.bitlib.crypto.InMemoryPrivateKey
import com.mrd.bitlib.crypto.PublicKey
import com.mycelium.wapi.wallet.colu.coins.ColuMain
import com.mycelium.wapi.wallet.manager.Config


data class PublicColuConfig(val publicKey: PublicKey, val coinType: ColuMain) : Config {
    override fun getType(): String = "colu_private"
}

data class PrivateColuConfig(val privateKey: InMemoryPrivateKey, val coinType: ColuMain) : Config {
    override fun getType(): String = "colu_public"
}
