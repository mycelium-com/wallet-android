package com.mycelium.wapi.wallet.bip44

import com.google.common.base.Optional
import com.mrd.bitlib.UnsignedTransaction
import com.mrd.bitlib.model.NetworkParameters
import com.mrd.bitlib.model.Transaction
import com.mycelium.wapi.api.Wapi
import com.mycelium.wapi.wallet.Bip44AccountBacking
import com.mycelium.wapi.wallet.ExportableAccount
import com.mycelium.wapi.wallet.KeyCipher

class Bip44AccountExternalSignature(
        context: HDAccountContext,
        keyManagerMap: Map<BipDerivationType, HDAccountKeyManager>,
        network: NetworkParameters,
        backing: Bip44AccountBacking,
        wapi: Wapi,
        private val sigProvider: ExternalSignatureProvider
) : Bip44PubOnlyAccount(context, keyManagerMap, network, backing, wapi) {

    @Override
    fun getBIP44AccountType() = sigProvider.biP44AccountType

    @Throws(KeyCipher.InvalidKeyCipher::class)
    override fun signTransaction(unsigned: UnsignedTransaction, cipher: KeyCipher): Transaction {
        checkNotArchived()
        if (!isValidEncryptionKey(cipher)) {
            throw KeyCipher.InvalidKeyCipher()
        }

        // Get the signatures from the external signature provider
        return sigProvider.getSignedTransaction(unsigned, this)
    }

    override fun canSpend(): Boolean {
        return true
    }

    override fun getExportData(cipher: KeyCipher): ExportableAccount.Data {
        // we dont have a private key we can export, always set it as absent
        val pubKey = Optional.of(keyManagerMap[BipDerivationType.BIP44]!!.getPublicAccountRoot().serialize(network)) // TODO FIX SEGWIT
        return ExportableAccount.Data(Optional.absent<String>(), pubKey)
    }
}
