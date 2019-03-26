package com.mycelium.wapi.wallet.bch.bip44

import com.mycelium.wapi.wallet.*


/**
 * Get Bitcoin Cash HD-accounts
 *
 * @return list of accounts
 */
fun WalletManager.getBCHBip44Accounts() = getAccounts().filter { it is Bip44BCHAccount && it.isVisible }