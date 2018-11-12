package com.mycelium.wapi.wallet

import com.mrd.bitlib.model.Transaction
import com.mrd.bitlib.model.TransactionInput


interface InputSigner {
    fun signInput(genericInput: GenericInput, keyCipher: KeyCipher)
}

data class GenericInput(val transaction: Transaction, val transactionInput: TransactionInput
                        , val index: Int)

