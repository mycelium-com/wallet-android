package com.mycelium.wapi.wallet.eth

import java.util.logging.Level
import java.util.logging.Logger

class EthTransactionService(address: String) : AbstractTransactionService(address) {
    private val logger = Logger.getLogger(this.javaClass.simpleName)

    override fun getTransactions(): List<Tx> {
        val txs = fetchTransactions().filter { it.tokenTransfers.isEmpty() }
        logger.log(Level.INFO, "account: $address. retrieved: ${txs.size} transactions...")
        return txs
    }
}