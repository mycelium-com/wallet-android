package com.mycelium.wapi.content

import com.mycelium.wapi.wallet.GenericAddress


class ContentResolver {
    private val uriParsers = mutableListOf<UriParser>()
    private val addressParsers = mutableListOf<AddressParser>()

    fun add(parser: UriParser) {
        uriParsers.add(parser)
    }

    fun add(parser: AddressParser) {
        addressParsers.add(parser)
    }

    fun resolveUri(content: String): GenericAssetUri? {
        var result: GenericAssetUri? = null
        for (parser in uriParsers) {
            result = parser.parse(content)
            if (result != null) {
                break
            }
        }
        return result
    }

    fun resovleAddress(content: String): GenericAddress? {
        var result: GenericAddress? = null
        for (parser in addressParsers) {
            result = parser.parse(content)
            if (result != null) {
                break
            }
        }
        return result
    }
}

interface UriParser {
    fun parse(content: String): GenericAssetUri?
}

interface AddressParser {
    fun parse(content: String): GenericAddress?
}