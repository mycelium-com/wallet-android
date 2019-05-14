package com.mycelium.wapi.wallet


import com.google.common.base.Optional
import com.mrd.bitlib.crypto.BipDerivationType

import java.io.Serializable

interface ExportableAccount {
    class Data : Serializable {
        val privateData: Optional<String>
        var privateDataMap: Map<BipDerivationType, String>? = null
        var publicDataMap: Map<BipDerivationType, String>? = null

        constructor(privateData: Optional<String>, publicDataMap: Map<BipDerivationType, String>?) {
            this.privateData = privateData
            this.publicDataMap = publicDataMap
        }

        constructor(privateDataMap: Map<BipDerivationType, String>?, publicDataMap: Map<BipDerivationType, String>?) {
            this.privateData = Optional.fromNullable(privateDataMap?.get(privateDataMap.keys.iterator().next()))
            this.privateDataMap = privateDataMap
            this.publicDataMap = publicDataMap
        }
    }

    fun getExportData(cipher: KeyCipher): Data
}
