package com.mycelium.wallet

import com.mycelium.wapi.wallet.*
import com.mycelium.wapi.wallet.bch.bip44.Bip44BCHAccount
import com.mycelium.wapi.wallet.bch.single.SingleAddressBCHAccount
import com.mycelium.wapi.wallet.btc.bip44.HDAccount
import com.mycelium.wapi.wallet.btc.single.SingleAddressAccount
import com.mycelium.wapi.wallet.coinapult.CoinapultAccount
import com.mycelium.wapi.wallet.coinapult.Currency
import com.mycelium.wapi.wallet.colu.ColuPubOnlyAccount
import com.mycelium.wapi.wallet.eth.EthAccount

fun WalletManager.getBTCBip44Accounts() = getAccounts().filter { it is HDAccount && it.isVisible }

fun WalletManager.getBCHBip44Accounts() = getAccounts().filter { it is Bip44BCHAccount && it.isVisible }

fun WalletManager.getBTCSingleAddressAccounts() = getAccounts().filter { it is SingleAddressAccount && !Utils.checkIsLinked(it, getAccounts()) }

fun WalletManager.getBCHSingleAddressAccounts() = getAccounts().filter { it is SingleAddressBCHAccount && it.isVisible }

fun WalletManager.getColuAccounts(): List<WalletAccount<*, *>> = getAccounts().filter { it is ColuPubOnlyAccount && it.isVisible && it.isActive }

fun WalletManager.getCoinapultAccounts(): List<WalletAccount<*, *>> = getAccounts().filter { it is CoinapultAccount && it.isVisible && it.isActive }

fun WalletManager.getCoinapultAccount(currency: Currency): WalletAccount<*, *>? = getCoinapultAccounts().find { it.coinType == currency }

fun WalletManager.getEthAccounts(): List<WalletAccount<*, *>> = getAccounts().filter { it is EthAccount && it.isVisible }

/**
 * Get the active BTC HD-accounts managed by the wallet manager, excluding on-the-fly-accounts and single-key accounts
 *
 * @return the list of accounts
 */
fun WalletManager.getActiveHDAccounts(): List<WalletAccount<*, *>> = getAccounts().filter { it is HDAccount && it.isActive }

/**
 * Get the active HD-accounts managed by the wallet manager, excluding on-the-fly-accounts and single-key accounts
 *
 * @return the list of accounts
 */
fun WalletManager.getActiveMasterseedAccounts(): List<WalletAccount<*, *>> = getAccounts().filter { it is HDAccount && it.isDerivedFromInternalMasterseed }