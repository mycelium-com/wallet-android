package com.mycelium.wapi.wallet.eth

import com.mycelium.wapi.wallet.TransactionData
import java.math.BigInteger

class EthTransactionData(var nonce: BigInteger? = null,
                         var gasLimit: BigInteger? = null,
                         var inputData: String? = null,
                         var suggestedGasPrice: BigInteger? = null,
                         // Optional explicit EIP-1559 tip. When null, the tip is
                         // recovered from the cached fee snapshot for the chosen
                         // gas price (see EthBlockchainService.priorityFeeFor).
                         var maxPriorityFeePerGas: BigInteger? = null) : TransactionData