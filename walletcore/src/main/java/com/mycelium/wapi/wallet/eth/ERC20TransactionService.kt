package com.mycelium.wapi.wallet.eth

import com.mycelium.net.HttpsEndpoint

class ERC20TransactionService(address: String, transactionServiceEndpoints: List<HttpsEndpoint>,
                              private val contractAddress: String)
    : AbstractTransactionService(address, transactionServiceEndpoints) {

    override fun getTransactions() = fetchTransactions().filter { tx -> tx.getTokenTransfer(contractAddress) != null }
}