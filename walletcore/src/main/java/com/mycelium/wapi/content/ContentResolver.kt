package com.mycelium.wapi.content


class ContentResolver {
    private val uriParsers = mutableListOf<UriParser>()

    fun add(parser: UriParser) {
        uriParsers.add(parser)
    }

    fun resolveUri(content: String): AssetUri? {
        return uriParsers.asSequence().mapNotNull { it.parse(content) }.firstOrNull()
    }
}

interface UriParser {
    fun parse(content: String): AssetUri?
}
