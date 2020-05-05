package com.mycelium.wapi.wallet.eth

import com.mycelium.net.HttpsEndpoint

class EthTransactionService(private val address: String, transactionServiceEndpoints: List<HttpsEndpoint>)
    : AbstractTransactionService(address, transactionServiceEndpoints) {

    override fun getTransactions() = fetchTransactions()
            .filter {
                it.tokenTransfers.isEmpty() ||
                        (it.tokenTransfers.isNotEmpty() && it.tokenTransfers.any { transfer ->
                            isOutgoing(transfer)
                        })
            }

    private fun isOutgoing(transfer: TokenTransfer) =
            transfer.from.equals(address, true) && !transfer.to.equals(address, true) ||
                    transfer.from.equals(address, true) && transfer.to.equals(address, true)
}