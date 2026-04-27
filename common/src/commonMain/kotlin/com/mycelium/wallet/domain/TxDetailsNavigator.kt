package com.mycelium.wallet.domain

interface TxDetailsNavigator {
    fun navigateToTxDetails(accountId: String, txIdHex: String)
}
