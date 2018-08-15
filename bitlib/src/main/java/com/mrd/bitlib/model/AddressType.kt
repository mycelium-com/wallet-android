package com.mrd.bitlib.model

enum class AddressType {
    P2PK, // Not supported
    P2PKH, // Legacy
    P2SH, // Not supported, use P2SH_P2WPKH
    P2WPKH, // TODO
    P2WSH, // Not supported
    P2SH_P2WPKH, // Supported
    P2SH_P2WSH // Not supported
}