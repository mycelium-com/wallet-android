package com.mycelium.wallet

import com.mrd.bitlib.crypto.InMemoryPrivateKey
import org.bitcoinj.core.ECKey
import org.bitcoinj.core.Sha256Hash
import org.bitcoinj.crypto.ChildNumber
import org.bitcoinj.signers.CustomTransactionSigner

/**
 * Created by Nelson on 09/03/2018.
 */
class MbwTransactionSigner(private val privateKey: InMemoryPrivateKey) : CustomTransactionSigner() {
    override fun getSignature(sighash: Sha256Hash?, derivationPath: MutableList<ChildNumber>?): SignatureAndKey {
        return SignatureAndKey(ECKey.ECDSASignature.decodeFromDER(privateKey.privateKeyBytes),
                ECKey.fromPublicOnly(privateKey.publicKey.publicKeyBytes))
    }
}