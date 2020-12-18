package com.mycelium.wapi.wallet.fio

import com.mrd.bitlib.crypto.HdKeyNode
import com.mrd.bitlib.crypto.InMemoryPrivateKey
import com.mycelium.wapi.wallet.manager.Config

class FIOMasterseedConfig : Config

class FIOUnrelatedHDConfig(val hdKeyNodes: List<HdKeyNode>, val legacy: Boolean = false) : Config

class FIOAddressConfig(val address: FioAddress) : Config

class FIOPrivateKeyConfig(val privkey: InMemoryPrivateKey) : Config