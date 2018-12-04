package com.mrd.bitlib

import com.mrd.bitlib.crypto.IPublicKeyRing
import com.mrd.bitlib.model.NetworkParameters
import com.mrd.bitlib.model.TransactionOutput
import com.mrd.bitlib.model.UnspentTransactionOutput

class PopBuilder(network: NetworkParameters) : StandardTransactionBuilder(network) {

    class UnsignedPop constructor(outputs: List<TransactionOutput>, funding: List<UnspentTransactionOutput>, keyRing: IPublicKeyRing, network: NetworkParameters) :
            UnsignedTransaction(outputs, funding, keyRing, network, MAX_LOCK_TIME, POP_SEQUENCE_NUMBER) {

        companion object {
            const val MAX_LOCK_TIME = 499999999
            private const val POP_SEQUENCE_NUMBER = 0
        }
    }

    fun createUnsignedPop(outputs: List<TransactionOutput>, funding: List<UnspentTransactionOutput>,
                          keyRing: IPublicKeyRing, network: NetworkParameters): UnsignedPop {

        return UnsignedPop(outputs, funding, keyRing, network)
    }
}
