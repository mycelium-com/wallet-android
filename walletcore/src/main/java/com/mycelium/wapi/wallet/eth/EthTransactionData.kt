package com.mycelium.wapi.wallet.eth

import com.mycelium.wapi.wallet.TransactionData
import java.math.BigInteger

class EthTransactionData(var nonce: BigInteger? = null,
                         var gasLimit: BigInteger? = null,
                         var inputData: String? = null,
                         var suggestedGasPrice: BigInteger? = null) : TransactionData