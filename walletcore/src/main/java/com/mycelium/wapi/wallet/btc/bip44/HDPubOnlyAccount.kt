package com.mycelium.wapi.wallet.btc.bip44

import com.mrd.bitlib.crypto.BipDerivationType
import com.mrd.bitlib.model.NetworkParameters
import com.mycelium.wapi.api.Wapi
import com.mycelium.wapi.wallet.btc.Bip44AccountBacking
import com.mycelium.wapi.wallet.Reference
import com.mycelium.wapi.wallet.bip44.ChangeAddressMode


open class HDPubOnlyAccount(
        context: HDAccountContext,
        keyManagerMap: Map<BipDerivationType, HDAccountKeyManager>,
        network: NetworkParameters,
        backing: Bip44AccountBacking,
        wapi: Wapi
) : HDAccount(context, keyManagerMap, network, backing, wapi, Reference(ChangeAddressMode.NONE)) {

    override fun canSpend(): Boolean {
        return false
    }
}
