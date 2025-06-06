package com.mycelium.wapi.wallet.btc.bip44

import com.mrd.bitlib.crypto.HdKeyNode
import com.mycelium.wapi.wallet.manager.Config


data class UnrelatedHDAccountConfig(val hdKeyNodes: List<HdKeyNode>) : Config

class AdditionalHDAccountConfig : Config

data class ExternalSignaturesAccountConfig(val hdKeyNodes: List<HdKeyNode>,
                                            val provider: ExternalSignatureProvider,
                                           val accountIndex: Int) : Config

// config for migrate hd accounts to taproot, just addition bip86 private key
class TaprootMigrationHDAccountConfig(val account: HDAccount) : Config