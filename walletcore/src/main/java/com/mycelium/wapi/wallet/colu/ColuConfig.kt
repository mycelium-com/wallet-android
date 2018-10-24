package com.mycelium.wapi.wallet.colu

import com.mrd.bitlib.crypto.InMemoryPrivateKey
import com.mrd.bitlib.crypto.PublicKey
import com.mycelium.wapi.wallet.KeyCipher
import com.mycelium.wapi.wallet.btc.single.PrivateSingleConfig
import com.mycelium.wapi.wallet.btc.single.PublicSingleConfig
import com.mycelium.wapi.wallet.colu.coins.ColuMain


class PublicColuConfig(publicKey: PublicKey, val coinType: ColuMain) : PublicSingleConfig(publicKey) {
}

class PrivateColuConfig(privateKey: InMemoryPrivateKey, val coinType: ColuMain, cipher: KeyCipher) : PrivateSingleConfig(privateKey, cipher) {
}
