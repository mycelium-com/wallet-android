package com.mycelium.wapi.wallet.fio

import com.mrd.bitlib.crypto.InMemoryPrivateKey
import com.mycelium.wapi.wallet.manager.Config

class FIOMasterseedConfig : Config

class FIOAddressConfig(val address: FioAddress) : Config

class FIOPrivateKeyConfig(val privkey: InMemoryPrivateKey) : Config