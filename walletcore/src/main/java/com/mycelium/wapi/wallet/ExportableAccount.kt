package com.mycelium.wapi.wallet


import com.google.common.base.Optional
import com.mrd.bitlib.crypto.BipDerivationType

import java.io.Serializable

interface ExportableAccount {
    class Data : Serializable {
        val privateData: Optional<String>
        var privateDataMap: Map<BipDerivationType, String>? = null
        val publicData: Optional<String>
        var publicDataMap: Map<BipDerivationType, String>? = null

        constructor(privateData: Optional<String>, publicData: Optional<String>) {
            this.privateData = privateData
            this.publicData = publicData
        }

        constructor(privateDataMap: Map<BipDerivationType, String>?, publicDataMap: Map<BipDerivationType, String>?) {
            this.privateData = Optional.fromNullable(privateDataMap?.get(privateDataMap.keys.iterator().next()))
            this.privateDataMap = privateDataMap
            this.publicData = Optional.fromNullable(publicDataMap?.get(publicDataMap.keys.iterator().next()))
            this.publicDataMap = publicDataMap
        }
    }

    fun getExportData(cipher: KeyCipher): Data
}
