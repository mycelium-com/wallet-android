package com.mrd.bitlib.model

class ScriptInputP2WPKH(scriptBytes: ByteArray) : ScriptInput(scriptBytes) {
    /**
     * If isNested is true, than script is P2SH-P2WPKH, else it's simple P2WPKH
     */
    var isNested = false
}