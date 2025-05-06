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
import com.mycelium.generated.wallet.database.BTCVAccountBacking
import com.mycelium.generated.wallet.database.BTCVContext
import com.mycelium.generated.wallet.database.BTCVOutgoingTx
import com.mycelium.generated.wallet.database.BTCVPtxo
import com.mycelium.generated.wallet.database.BTCVRefersPtxo
import com.mycelium.generated.wallet.database.BTCVTransaction
import com.mycelium.generated.wallet.database.BTCVUtxo
import com.mycelium.generated.wallet.database.Erc20Context
import com.mycelium.generated.wallet.database.EthAccountBacking
import com.mycelium.generated.wallet.database.EthContext
import com.mycelium.generated.wallet.database.FeeEstimation
import com.mycelium.generated.wallet.database.FioAccountBacking
import com.mycelium.generated.wallet.database.FioContext
import com.mycelium.generated.wallet.database.FioKnownNames
import com.mycelium.generated.wallet.database.FioNameAccountMappings
import com.mycelium.generated.wallet.database.FioOtherBlockchainTransactions
import com.mycelium.generated.wallet.database.FioRequestsReceivedBacking
import com.mycelium.generated.wallet.database.FioRequestsSentBacking
import com.mycelium.wapi.wallet.AccountIndexesContext
import com.mycelium.wapi.wallet.coins.AssetInfo
import com.mycelium.wapi.wallet.coins.Balance
import com.mycelium.wapi.wallet.coins.COINS
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.fio.FIODomain
import com.mycelium.wapi.wallet.fio.FioName
import com.mycelium.wapi.wallet.fio.FioRequestStatus
import com.mycelium.wapi.wallet.fio.RegisteredFIOName
import fiofoundation.io.fiosdk.models.fionetworkprovider.FundsRequestContent
import fiofoundation.io.fiosdk.models.fionetworkprovider.RecordObtDataContent
import java.math.BigDecimal
import java.math.BigInteger
import java.util.UUID


object Adapters {
    val mapper = ObjectMapper()

    val uuidAdapter = object : ColumnAdapter<UUID, String> {
        override fun decode(databaseValue: String) = databaseValue.let(UUID::fromString)

        override fun encode(value: UUID) = value.toString()
    }

    val fioNameAdapter = object : ColumnAdapter<FioName, String> {
        override fun decode(databaseValue: String) = databaseValue.let {
            val nameDomain = it.split("@")
            FioName(nameDomain[0], nameDomain[1])
        }

        override fun encode(value: FioName) = "${value.name}@${value.domain}"
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

    val registeredFioNameAdapter = object : ColumnAdapter<List<RegisteredFIOName>, String> {
        override fun decode(databaseValue: String): List<RegisteredFIOName> {
            val type = mapper.typeFactory.constructCollectionType(ArrayList::class.java, RegisteredFIOName::class.java)
            return mapper.readValue(databaseValue, type)
        }

        override fun encode(value: List<RegisteredFIOName>): String = mapper.writeValueAsString(value)
    }

    val fioDomainAdapter = object : ColumnAdapter<List<FIODomain>, String> {
        override fun decode(databaseValue: String): List<FIODomain> {
            val type = mapper.typeFactory.constructCollectionType(ArrayList::class.java, FIODomain::class.java)
            return mapper.readValue(databaseValue, type)
        }

        override fun encode(value: List<FIODomain>): String = mapper.writeValueAsString(value)
    }

    val fioRequestStatusAdapter = object : ColumnAdapter<FioRequestStatus, String> {
        override fun decode(databaseValue: String) = FioRequestStatus.getStatus(databaseValue)

        override fun encode(value: FioRequestStatus) = value.status
    }

    val fioRequestDeserializedContentAdapter = object : ColumnAdapter<FundsRequestContent, String> {
        override fun decode(databaseValue: String): FundsRequestContent {
            val gson = GsonBuilder().serializeNulls().create()
            return gson.fromJson(databaseValue, FundsRequestContent::class.java)
        }

        override fun encode(value: FundsRequestContent): String = value.toJson()
    }

    val fioRecordObtDataContentAdapter = object : ColumnAdapter<RecordObtDataContent, String> {
        override fun decode(databaseValue: String): RecordObtDataContent {
            val gson = GsonBuilder().serializeNulls().create()
            return gson.fromJson(databaseValue, RecordObtDataContent::class.java)
        }

        override fun encode(value: RecordObtDataContent): String = value.toJson()
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

val fioAccountBackingAdapter = FioAccountBacking.Adapter(Adapters.uuidAdapter, Adapters.valueAdapter)

val accountContextAdapter = AccountContext.Adapter(Adapters.uuidAdapter, Adapters.cryptoCurrencyAdapter,
        Adapters.balanceAdapter, IntColumnAdapter)

val ethContextAdapter = EthContext.Adapter(Adapters.uuidAdapter, Adapters.bigIntAdapter,
    Adapters.listAdapter, IntColumnAdapter)

val erc20ContextAdapter = Erc20Context.Adapter(Adapters.uuidAdapter, Adapters.bigIntAdapter,
    IntColumnAdapter, Adapters.uuidAdapter)

val BTCVAccountBackingAdapter = BTCVAccountBacking.Adapter(Adapters.uuidAdapter)
val BTCVContextAdapter = BTCVContext.Adapter(Adapters.uuidAdapter, IntColumnAdapter,
    Adapters.indexContextsAdapter, IntColumnAdapter, IntColumnAdapter, Adapters.addressTypeAdapter)
val BTCVTransactionAdapter = BTCVTransaction.Adapter(Adapters.sha256Adapter, Adapters.uuidAdapter,
    Adapters.sha256Adapter, IntColumnAdapter, IntColumnAdapter)
val BTCVUtxoAdapter = BTCVUtxo.Adapter(Adapters.outPointAdapter, Adapters.uuidAdapter, IntColumnAdapter)
val BTCVPtxoAdapter = BTCVPtxo.Adapter(Adapters.outPointAdapter, Adapters.uuidAdapter, IntColumnAdapter)
val BTCVRefersPtxoAdapter = BTCVRefersPtxo.Adapter(Adapters.sha256Adapter, Adapters.uuidAdapter, Adapters.outPointAdapter)
val BTCVOutgoingTxAdapter = BTCVOutgoingTx.Adapter(Adapters.sha256Adapter, Adapters.uuidAdapter)

val fioContextAdapter = FioContext.Adapter(Adapters.uuidAdapter, IntColumnAdapter, IntColumnAdapter,
    Adapters.bigIntAdapter, Adapters.registeredFioNameAdapter, Adapters.fioDomainAdapter)

val fioKnownNamesAdapter = FioKnownNames.Adapter(Adapters.fioNameAdapter)

val fioNameAccountMappingsAdapter = FioNameAccountMappings.Adapter(Adapters.uuidAdapter)

val feeEstimatorAdapter = FeeEstimation.Adapter(Adapters.assetAdapter,
        Adapters.valueAdapter, Adapters.valueAdapter, Adapters.valueAdapter, Adapters.valueAdapter, LongColumnAdapter, IntColumnAdapter)

val fioSentRequestsAdapter = FioRequestsSentBacking.Adapter(Adapters.bigIntAdapter, Adapters.uuidAdapter, Adapters.fioRequestDeserializedContentAdapter,
        Adapters.fioRequestStatusAdapter)

val fioReceivedRequestsAdapter = FioRequestsReceivedBacking.Adapter(Adapters.bigIntAdapter, Adapters.uuidAdapter, Adapters.fioRequestDeserializedContentAdapter)

val fioOtherBlockchainTransactionsAdapter = FioOtherBlockchainTransactions.Adapter(Adapters.bigIntAdapter,
        Adapters.fioRecordObtDataContentAdapter)
