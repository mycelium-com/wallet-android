package com.mycelium.wapi.wallet.bch.bip44

import com.mrd.bitlib.crypto.BipDerivationType
import com.mrd.bitlib.model.NetworkParameters
import com.mycelium.wapi.api.Wapi
import com.mycelium.wapi.wallet.Address
import com.mycelium.wapi.wallet.Fee
import com.mycelium.wapi.wallet.Transaction
import com.mycelium.wapi.wallet.TransactionData
import com.mycelium.wapi.wallet.btc.*
import com.mycelium.wapi.wallet.btc.bip44.HDAccount
import com.mycelium.wapi.wallet.btc.bip44.HDAccountContext
import com.mycelium.wapi.wallet.btc.bip44.HDAccountKeyManager
import com.mycelium.wapi.wallet.coins.Value

open class Bip44BCHAccount(
        context: HDAccountContext,
        keyManagerMap: MutableMap<BipDerivationType, HDAccountKeyManager>,
        network: NetworkParameters, backing: Bip44BtcAccountBacking, wapi: Wapi) :
        HDAccount(context, keyManagerMap, network, backing, wapi, Reference(ChangeAddressMode.NONE)) {
    override fun canSign(): Boolean = false
}