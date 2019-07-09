package com.mycelium.wapi.wallet.manager

/**
 * Interface define class as data class for {@link com.mycelium.wapi.wallet.WalletManager#createAccounts(Config)} method,
 * what depending from extended instance creates wallet account.
 * Extended config class may contains private key, public key, address, etc
 * {@link com.mycelium.wapi.wallet.manager.WalletModule} handle this interface and create instance of WalletAccount
 */
interface Config
