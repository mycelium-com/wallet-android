package com.mycelium.wapi.wallet.eth

import com.mycelium.net.HttpsEndpoint

class EthTransactionService(address: String, transactionServiceEndpoints: List<HttpsEndpoint>)
    : AbstractTransactionService(address, transactionServiceEndpoints) {

    override fun getTransactions() = fetchTransactions().filter { it.tokenTransfers.isEmpty() }
} 