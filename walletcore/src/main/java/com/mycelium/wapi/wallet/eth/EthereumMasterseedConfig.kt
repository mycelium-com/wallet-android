package com.mycelium.wapi.wallet.eth

import com.mycelium.wapi.wallet.manager.Config

class EthereumMasterseedConfig: Config

class EthAddressConfig @JvmOverloads constructor(val address: EthAddress, label: String = "") : Config