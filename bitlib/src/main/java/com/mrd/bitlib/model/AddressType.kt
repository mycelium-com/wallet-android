package com.mrd.bitlib.model

import java.io.Serializable

enum class AddressType : Serializable {
    P2PKH, // Legacy
    //P2WPKH, // TODO
    P2SH_P2WPKH, // Supported
    //P2PK, // Not supported
    //P2SH, // Not supported, use P2SH_P2WPKH
    //P2WSH, // Not supported
    //P2SH_P2WSH // Not supported
}