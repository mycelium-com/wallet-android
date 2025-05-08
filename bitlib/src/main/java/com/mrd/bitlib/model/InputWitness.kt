package com.mrd.bitlib.model

import com.mrd.bitlib.util.ByteWriter

import java.io.Serializable
import java.util.ArrayList
import kotlin.math.min

class InputWitness(val pushCount: Int) : Serializable {
    private val stack = ArrayList<ByteArray>(min(pushCount, MAX_INITIAL_ARRAY_LENGTH))

    fun setStack(i: Int, value: ByteArray) {
        while (i >= stack.size) {
            stack.add(byteArrayOf())
        }
        stack[i] = value
    }

    fun toByteWriter(writer: ByteWriter) {
        writer.putCompactInt(stack.size.toLong())
        stack.forEach { element ->
            writer.putCompactInt(element.size.toLong())
            writer.putBytes(element)
        }
    }

    companion object {
        @JvmField
        val EMPTY = InputWitness(0)
        const val MAX_INITIAL_ARRAY_LENGTH = 20
    }
}
