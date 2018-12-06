package com.mycelium.wallet.extsig.common

import com.mrd.bitlib.UnsignedTransaction
import com.mrd.bitlib.model.NetworkParameters
import com.mycelium.wapi.wallet.btc.bip44.HDAccount


/**
 * Change would appear on screen of hardware device if derivation path of change contains different purpose when inputs,
 * change really exists, or inputs are from different purpose accounts.
 */
fun showChange(unsigned: UnsignedTransaction, network: NetworkParameters, account: HDAccount): Boolean {
    val fundingOutputAddressesTypes = unsigned.fundingOutputs
            .groupBy { it.script.getAddress(network).type }
            .keys

    val changeAddressType = unsigned.outputs.asSequence()
            .map { it.script.getAddress(network) }
            .filter { account.isOwnInternalAddress(it) }
            .map { it.type }
            .firstOrNull()

    // If there's a change and there are different input derivation types or input type is different with change type.
    return changeAddressType != null &&
            ((fundingOutputAddressesTypes.size > 1) || fundingOutputAddressesTypes.first() != changeAddressType)
}