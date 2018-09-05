package com.mycelium.wapi.wallet.btc.bip44

import com.google.common.base.Optional
import com.mrd.bitlib.UnsignedTransaction
import com.mrd.bitlib.crypto.BipDerivationType
import com.mrd.bitlib.model.NetworkParameters
import com.mrd.bitlib.model.Transaction
import com.mycelium.wapi.api.Wapi
import com.mycelium.wapi.wallet.btc.Bip44AccountBacking
import com.mycelium.wapi.wallet.ExportableAccount
import com.mycelium.wapi.wallet.KeyCipher

class HDAccountExternalSignature(
        context: HDAccountContext,
        keyManagerMap: Map<BipDerivationType, HDAccountKeyManager>,
        network: NetworkParameters,
        backing: Bip44AccountBacking,
        wapi: Wapi,
        private val sigProvider: ExternalSignatureProvider
) : HDPubOnlyAccount(context, keyManagerMap, network, backing, wapi) {

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
        val pubKey = Optional.of(keyManagerMap[BipDerivationType.BIP44]!!
                .getPublicAccountRoot()
                .serialize(network, BipDerivationType.BIP44)) // TODO FIX SEGWIT
        return ExportableAccount.Data(Optional.absent<String>(), pubKey)
    }
}
