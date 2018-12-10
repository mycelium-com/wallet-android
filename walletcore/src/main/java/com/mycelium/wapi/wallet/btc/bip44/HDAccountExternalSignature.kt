package com.mycelium.wapi.wallet.btc.bip44

import com.mrd.bitlib.UnsignedTransaction
import com.mrd.bitlib.crypto.BipDerivationType
import com.mrd.bitlib.model.NetworkParameters
import com.mrd.bitlib.model.Transaction
import com.mycelium.wapi.api.Wapi
import com.mycelium.wapi.wallet.*
import com.mycelium.wapi.wallet.btc.Bip44AccountBacking
import com.mycelium.wapi.wallet.btc.ChangeAddressMode;

class HDAccountExternalSignature(
        context: HDAccountContext,
        keyManagerMap: MutableMap<BipDerivationType, HDAccountKeyManager>,
        network: NetworkParameters,
        backing: Bip44AccountBacking,
        wapi: Wapi,
        private val sigProvider: ExternalSignatureProvider,
        changeAddressModeReference: Reference<ChangeAddressMode>
) : HDAccount(context, keyManagerMap, network, backing, wapi, changeAddressModeReference) {

    @Override
    fun getBIP44AccountType() = sigProvider.biP44AccountType

    @Throws(KeyCipher.InvalidKeyCipher::class)
    override fun signTransaction(unsigned: UnsignedTransaction, cipher: KeyCipher): Transaction? {
        checkNotArchived()
        if (!isValidEncryptionKey(cipher)) {
            throw KeyCipher.InvalidKeyCipher()
        }

        // Get the signatures from the external signature provider
        return sigProvider.getSignedTransaction(unsigned, this)
    }

    override fun canSpend() = true

    override fun getExportData(cipher: KeyCipher): ExportableAccount.Data {
        // we dont have a private key we can export, always set it as absent
        val publicDataMap = keyManagerMap.keys.map { derivationType ->
            derivationType to (keyManagerMap[derivationType]!!.publicAccountRoot
                    .serialize(_network, derivationType))
        }.toMap()
        return ExportableAccount.Data(null, publicDataMap)
    }
}
