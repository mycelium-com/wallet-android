package com.mycelium.wapi.wallet.fio

import com.mrd.bitlib.crypto.HdKeyNode
import com.mrd.bitlib.crypto.InMemoryPrivateKey
import com.mycelium.wapi.wallet.manager.Config

class FIOMasterseedConfig : Config

class FIOUnrelatedHDConfig @JvmOverloads constructor(val hdKeyNodes: List<HdKeyNode>, val labelBase: String = "") : Config

class FIOAddressConfig(val address: FioAddress) : Config

class FIOPrivateKeyConfig(val privkey: InMemoryPrivateKey) : Config