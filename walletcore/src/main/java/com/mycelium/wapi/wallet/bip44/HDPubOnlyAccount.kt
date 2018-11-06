package com.mycelium.wapi.wallet.bip44

import com.mrd.bitlib.crypto.BipDerivationType
import com.mrd.bitlib.model.NetworkParameters
import com.mycelium.wapi.api.Wapi
import com.mycelium.wapi.wallet.Bip44AccountBacking
import com.mycelium.wapi.wallet.Reference


open class HDPubOnlyAccount(
        context: HDAccountContext,
        keyManagerMap: MutableMap<BipDerivationType, HDAccountKeyManager>,
        network: NetworkParameters,
        backing: Bip44AccountBacking,
        wapi: Wapi
) : HDAccount(context, keyManagerMap, network, backing, wapi, Reference(ChangeAddressMode.NONE)) {

    override fun canSpend(): Boolean {
        return false
    }
}
