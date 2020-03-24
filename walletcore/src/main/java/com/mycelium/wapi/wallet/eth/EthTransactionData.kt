package com.mycelium.wapi.wallet.eth

import com.mycelium.wapi.wallet.GenericTransactionData
import java.math.BigInteger

class EthTransactionData(var nonce: BigInteger? = null,
                         var gasLimit: BigInteger? = null,
                         var inputData: String? = null) : GenericTransactionData