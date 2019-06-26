package com.mycelium.wapi.wallet.bch.single

import com.mycelium.wapi.wallet.*

/**
 * Get bitcoin single account list
 *
 * @return list of accounts
 */
fun WalletManager.getBCHSingleAddressAccounts() = getAccounts().filter { it is SingleAddressBCHAccount && it.isVisible }
