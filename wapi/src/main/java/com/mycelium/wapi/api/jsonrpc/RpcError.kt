package com.mycelium.wapi.api.jsonrpc

import java.util.*

class RpcError @JvmOverloads internal constructor(
        private val code: Int,
        private val message: String,
        private val data: Any? = null
) {

    enum class ErrorType constructor(
            private val code: Int
    ) {

        METHOD_NOT_FOUND(METHOD_NOT_FOUND_CODE),
        PARSE_ERROR(PARSE_ERROR_CODE),
        INVALID_REQUEST(INVALID_REQUEST_CODE),
        INVALID_PARAMS(INVALID_PARAMS_CODE),
        INTERNAL_ERROR(INTERNAL_ERROR_CODE);

        fun toMap(data: Any?): MutableMap<String, Any> {
            return HashMap<String, Any>().also {
                it[CODE_KEY] = code
                it[MESSAGE_KEY] = toString().toLowerCase().replace("_".toRegex(), " ")
                if (data != null) {
                    it[DATA_KEY] = data
                }
            }
        }
    }

    override fun toString(): String {
        return "RpcError(code=$code, message='$message', data=$data)"
    }


}