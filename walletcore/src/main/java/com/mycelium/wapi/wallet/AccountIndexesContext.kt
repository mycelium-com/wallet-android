package com.mycelium.wapi.wallet

import com.fasterxml.jackson.annotation.JsonProperty
import java.io.Serializable


data class AccountIndexesContext(@JsonProperty("lastExternalIndexWithActivity") var lastExternalIndexWithActivity: Int = 0,
                                 @JsonProperty("lastInternalIndexWithActivity") var lastInternalIndexWithActivity: Int = 0,
                                 @JsonProperty("firstMonitoredInternalIndex") var firstMonitoredInternalIndex: Int = 0)
    : Serializable {
    companion object {
        private const val serialVersionUid = 1L
    }
}