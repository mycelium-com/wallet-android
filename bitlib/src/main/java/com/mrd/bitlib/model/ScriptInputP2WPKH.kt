package com.mrd.bitlib.model

class ScriptInputP2WPKH(scriptBytes: ByteArray) : ScriptInput(scriptBytes) {
    var isNested = false
}