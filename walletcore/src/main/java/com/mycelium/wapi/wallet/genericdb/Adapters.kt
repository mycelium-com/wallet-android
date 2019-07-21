package com.mycelium.wapi.wallet.genericdb

import com.fasterxml.jackson.databind.JsonNode
import com.google.api.client.json.JsonObjectParser
import com.mycelium.generated.wallet.database.AccountContext
import com.mycelium.wapi.wallet.coins.Balance
import com.mycelium.wapi.wallet.coins.COINS
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.squareup.sqldelight.ColumnAdapter
import java.util.*
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.mycelium.wapi.wallet.coins.Value


object Adapters {

    val uuidAdapter = object : ColumnAdapter<UUID, String> {
        override fun decode(databaseValue: String) = databaseValue.let(UUID::fromString)

        override fun encode(value: UUID) = value.toString()
    }

    val cryptoCurrencyAdapter = object : ColumnAdapter<CryptoCurrency, String> {
        override fun decode(databaseValue: String): CryptoCurrency =
                databaseValue.let(COINS::get) ?: throw IllegalArgumentException("Unknown currency type $databaseValue")

        override fun encode(value: CryptoCurrency): String = value.id
    }

    val balanceAdapter = object : ColumnAdapter<Balance, String> {
        override fun decode(databaseValue: String): Balance {
            val mapper = ObjectMapper()

            val rootNode = mapper.readTree(databaseValue)
            val childNodes = rootNode.get("Balance") as ArrayNode
            val asset = rootNode.get("Asset").asText()

            val confirmed = Value(COINS[asset], childNodes[0].asLong())
            val pendingReceiving = Value(COINS[asset], childNodes[1].asLong())
            val pendingSending = Value(COINS[asset], childNodes[2].asLong())
            val pendingChange = Value(COINS[asset], childNodes[3].asLong())

            return Balance(confirmed, pendingReceiving, pendingSending, pendingChange)
        }


        override fun encode(value: Balance): String {
            val mapper = ObjectMapper()

            val rootNode = mapper.createObjectNode()
            val childNodes = mapper.createArrayNode()

            childNodes.add(value.confirmed.getValue())
            childNodes.add(value.pendingReceiving.getValue())
            childNodes.add(value.pendingSending.getValue())
            childNodes.add(value.pendingChange.getValue())
            rootNode.set("Balance", childNodes)
            rootNode.put("Asset", value.confirmed.type.id)
            return rootNode.toString()
        }
    }
}

val accountContextAdapter = AccountContext.Adapter(Adapters.uuidAdapter, Adapters.cryptoCurrencyAdapter, Adapters.balanceAdapter)