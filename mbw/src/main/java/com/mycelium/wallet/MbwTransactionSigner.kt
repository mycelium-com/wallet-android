package com.mycelium.wallet

import org.bitcoinj.core.Sha256Hash
import org.bitcoinj.crypto.ChildNumber
import org.bitcoinj.signers.CustomTransactionSigner

/**
 * Created by Nelson on 09/03/2018.
 */
class MbwTransactionSigner : CustomTransactionSigner() {
    override fun getSignature(sighash: Sha256Hash?, derivationPath: MutableList<ChildNumber>?): SignatureAndKey {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}