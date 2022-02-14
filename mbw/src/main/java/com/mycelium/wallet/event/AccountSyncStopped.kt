package com.mycelium.wallet.event

import com.mycelium.wapi.wallet.WalletAccount

data class AccountSyncStopped(val walletAccount: WalletAccount<*>)