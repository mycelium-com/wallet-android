package com.mycelium.wapi.content.fio

import fiofoundation.io.fiosdk.utilities.PrivateKeyUtils.base58Decode
import org.bouncycastle.crypto.digests.RIPEMD160Digest

fun String.isFioPublicKey(): Boolean {
    if (this.isNotEmpty()) {
        val fioRegEx = Regex("^FIO.+$")
        if (fioRegEx.matchEntire(this) != null)
            return checkCheckSum(this)
    }

    return false
}

fun checkCheckSum(string: String): Boolean {
    val key = string.substring(3)
    val base = base58Decode(key)
    val checkSum1 = base.sliceArray(IntRange(base.size - 4, base.size - 1))
    val publicKey = base.sliceArray(IntRange(0, base.size - 5))
    val output = RIPEMD160Digest().inOneGo(publicKey)
    val checkSum2 = output.sliceArray(IntRange(0, 3))
    return checkSum1.contentEquals(checkSum2)
}

fun RIPEMD160Digest.inOneGo(input: ByteArray): ByteArray {
    val output = ByteArray(digestSize)

    update(input, 0, input.size)
    doFinal(output, 0)

    return output
}