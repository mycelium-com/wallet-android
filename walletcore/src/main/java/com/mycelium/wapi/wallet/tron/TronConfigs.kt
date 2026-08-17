package com.mycelium.wapi.wallet.tron

import com.mycelium.wapi.wallet.CurrencySettings
import com.mycelium.wapi.wallet.manager.Config

/** Derives the next TRON account from the wallet masterseed (m/44'/195'/n'/0/0). */
class TronMasterseedConfig : Config

/** Watch-only TRON account for a base58 address. */
class TronAddressConfig @JvmOverloads constructor(val address: TronAddress, val label: String = "") : Config

class TronSettings : CurrencySettings
