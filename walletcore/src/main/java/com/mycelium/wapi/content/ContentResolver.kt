package com.mycelium.wapi.content


class ContentResolver {
    private val uriParsers = mutableListOf<UriParser>()

    fun add(parser: UriParser) {
        uriParsers.add(parser)
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

    interface UriParser {
        fun parse(content: String): GenericAssetUri?
    }
}