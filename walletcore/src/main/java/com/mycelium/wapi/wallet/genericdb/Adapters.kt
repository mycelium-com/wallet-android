package com.mycelium.wapi.wallet.genericdb

import app.cash.sqldelight.ColumnAdapter
import app.cash.sqldelight.adapter.primitive.IntColumnAdapter
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.google.common.base.Preconditions
import com.google.gson.GsonBuilder
import com.mrd.bitlib.crypto.BipDerivationType
import com.mrd.bitlib.model.AddressType
import com.mrd.bitlib.model.OutPoint
import com.mrd.bitlib.util.Sha256Hash
import com.mycelium.generated.wallet.database.AccountBacking
import com.mycelium.generated.wallet.database.AccountContext
import com.mycelium.generated.wallet.database.DeprecatedAccount
import com.mycelium.generated.wallet.database.Erc20Context
import com.mycelium.generated.wallet.database.EthAccountBacking
import com.mycelium.generated.wallet.database.EthContext
import com.mycelium.generated.wallet.database.FeeEstimation
import com.mycelium.generated.wallet.database.EnergyServiceFeeLink
import com.mycelium.generated.wallet.database.Trc20Context
import com.mycelium.generated.wallet.database.TronAccountBacking
import com.mycelium.generated.wallet.database.TronContext
import com.mycelium.wapi.wallet.AccountIndexesContext
import com.mycelium.wapi.wallet.coins.AssetInfo
import com.mycelium.wapi.wallet.coins.Balance
import com.mycelium.wapi.wallet.coins.COINS
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.coins.Value
import java.math.BigDecimal
import java.math.BigInteger
import java.util.UUID


object Adapters {
    val mapper = ObjectMapper()

    val uuidAdapter = object : ColumnAdapter<UUID, String> {
        override fun decode(databaseValue: String) = databaseValue.let(UUID::fromString)

        override fun encode(value: UUID) = value.toString()
    }

    val cryptoCurrencyAdapter = object : ColumnAdapter<CryptoCurrency, String> {
        override fun decode(databaseValue: String): CryptoCurrency =
                databaseValue.let(COINS::get)
                        ?: throw IllegalArgumentException("Unknown currency type $databaseValue")

        override fun encode(value: CryptoCurrency): String = value.id
    }

    val assetAdapter = object : ColumnAdapter<AssetInfo, String> {
        override fun decode(databaseValue: String): CryptoCurrency =
                databaseValue.let(COINS::get)
                        ?: throw IllegalArgumentException("Unknown currency type $databaseValue")

        override fun encode(value: AssetInfo): String = value.id
    }

    val balanceAdapter = object : ColumnAdapter<Balance, String> {
        override fun decode(databaseValue: String): Balance {
            val rootNode = mapper.readTree(databaseValue)
            val childNodes = rootNode.get("Balance") as ArrayNode
            val asset = rootNode.get("Asset").asText()

            val confirmed = Value.valueOf(COINS.getValue(asset), childNodes[0].bigIntegerValue())
            val pendingReceiving = Value.valueOf(COINS.getValue(asset), childNodes[1].bigIntegerValue())
            val pendingSending = Value.valueOf(COINS.getValue(asset), childNodes[2].bigIntegerValue())
            val pendingChange = Value.valueOf(COINS.getValue(asset), childNodes[3].bigIntegerValue())

            return Balance(confirmed, pendingReceiving, pendingSending, pendingChange)
        }

        override fun encode(value: Balance): String {
            val rootNode = mapper.createObjectNode()
            val childNodes = mapper.createArrayNode()

            childNodes.add(value.confirmed.value)
            childNodes.add(value.pendingReceiving.value)
            childNodes.add(value.pendingSending.value)
            childNodes.add(value.pendingChange.value)
            rootNode.set<ArrayNode>("Balance", childNodes)
            rootNode.put("Asset", value.confirmed.type.id)
            return rootNode.toString()
        }
    }

    val valueAdapter = object : ColumnAdapter<Value, String> {
        override fun decode(databaseValue: String): Value {
            val rootNode = mapper.readTree(databaseValue)
            val asset = COINS.getValue(rootNode["Asset"].asText())
            val value = rootNode["Value"].bigIntegerValue()
            return Value.valueOf(asset, value)
        }

        override fun encode(value: Value): String {
            val rootNode = mapper.createObjectNode()
            rootNode.put("Asset", assetAdapter.encode(value.type))
            rootNode.put("Value", value.value)
            return rootNode.toString()
        }
    }

    val bigIntAdapter = object : ColumnAdapter<BigInteger, String> {
        override fun decode(databaseValue: String): BigInteger {
            return BigInteger(databaseValue)
        }

        override fun encode(value: BigInteger): String {
            return value.toString()
        }
    }

    val bigDecimalAdapter = object : ColumnAdapter<BigDecimal, String> {
        override fun decode(databaseValue: String): BigDecimal {
            return BigDecimal(databaseValue)
        }

        override fun encode(value: BigDecimal): String {
            return value.toString()
        }
    }

    val listAdapter = object : ColumnAdapter<List<String>, String> {
        override fun decode(databaseValue: String): List<String> {
            val type = mapper.typeFactory.constructCollectionType(ArrayList::class.java, String::class.java)
            return mapper.readValue(databaseValue, type)
        }

        override fun encode(value: List<String>): String =
                mapper.writeValueAsString(value)
    }

    val sha256Adapter = object : ColumnAdapter<Sha256Hash, ByteArray> {
        override fun decode(databaseValue: ByteArray): Sha256Hash = Sha256Hash(databaseValue)

        override fun encode(value: Sha256Hash): ByteArray = value.bytes
    }
    val outPointAdapter = object : ColumnAdapter<OutPoint, ByteArray> {
        override fun decode(databaseValue: ByteArray): OutPoint {
            Preconditions.checkArgument(databaseValue.size == 34)
            val hash = Sha256Hash.copyOf(databaseValue, 0)
            val index: Int = (databaseValue[32].toInt() and 0xFF) or ((databaseValue[33].toInt() and 0xFF) shl 8)
            return OutPoint(hash, index)
        }

        override fun encode(value: OutPoint): ByteArray {
            val bytes = ByteArray(34)
            System.arraycopy(value.txid.bytes, 0, bytes, 0, Sha256Hash.HASH_LENGTH)
            bytes[32] = (value.index and 0xFF).toByte()
            bytes[33] = ((value.index shr 8) and 0xFF).toByte()
            return bytes
        }
    }

    val indexContextsAdapter = object : ColumnAdapter<Map<BipDerivationType, AccountIndexesContext>, String> {
        override fun decode(databaseValue: String): Map<BipDerivationType, AccountIndexesContext> {
            val type = mapper.typeFactory.constructMapType(HashMap::class.java, BipDerivationType::class.java, AccountIndexesContext::class.java)
            return mapper.readValue(databaseValue, type)
        }

        override fun encode(value: Map<BipDerivationType, AccountIndexesContext>): String =
                mapper.writeValueAsString(value)
    }

    val addressTypeAdapter = object : ColumnAdapter<AddressType, String> {
        override fun decode(databaseValue: String): AddressType = AddressType.valueOf(databaseValue)

        override fun encode(value: AddressType): String = value.toString()
    }
}

val accountBackingAdapter = AccountBacking.Adapter(Adapters.uuidAdapter, Adapters.cryptoCurrencyAdapter,
    IntColumnAdapter, Adapters.valueAdapter, Adapters.valueAdapter, IntColumnAdapter)

val ethAccountBackingAdapter = EthAccountBacking.Adapter(Adapters.uuidAdapter, Adapters.bigIntAdapter,
        Adapters.bigIntAdapter, Adapters.bigIntAdapter, Adapters.bigIntAdapter, Adapters.valueAdapter)

val deprecatedAccountAdapter = DeprecatedAccount.Adapter(Adapters.uuidAdapter, IntColumnAdapter,
    IntColumnAdapter)

val accountContextAdapter = AccountContext.Adapter(Adapters.uuidAdapter, Adapters.cryptoCurrencyAdapter,
        Adapters.balanceAdapter, IntColumnAdapter)

val ethContextAdapter = EthContext.Adapter(Adapters.uuidAdapter, Adapters.bigIntAdapter,
    Adapters.listAdapter, IntColumnAdapter)

val erc20ContextAdapter = Erc20Context.Adapter(Adapters.uuidAdapter, Adapters.bigIntAdapter,
    IntColumnAdapter, Adapters.uuidAdapter)

val tronAccountBackingAdapter = TronAccountBacking.Adapter(Adapters.uuidAdapter, Adapters.bigIntAdapter,
    Adapters.bigIntAdapter, Adapters.bigIntAdapter)

val tronContextAdapter = TronContext.Adapter(Adapters.uuidAdapter, Adapters.listAdapter, IntColumnAdapter)

val trc20ContextAdapter = Trc20Context.Adapter(Adapters.uuidAdapter, IntColumnAdapter, Adapters.uuidAdapter)

val energyServiceFeeLinkAdapter = EnergyServiceFeeLink.Adapter(Adapters.uuidAdapter)

val feeEstimatorAdapter = FeeEstimation.Adapter(Adapters.assetAdapter,
        Adapters.valueAdapter, Adapters.valueAdapter, Adapters.valueAdapter, Adapters.valueAdapter, LongColumnAdapter, IntColumnAdapter)
