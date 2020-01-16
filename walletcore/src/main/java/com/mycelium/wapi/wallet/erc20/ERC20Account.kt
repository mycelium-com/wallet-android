package com.mycelium.wapi.wallet.erc20

import com.mrd.bitlib.crypto.InMemoryPrivateKey
import com.mrd.bitlib.util.HexUtils
import com.mycelium.net.ServerEndpointType
import com.mycelium.net.ServerEndpoints
import com.mycelium.wapi.wallet.*
import com.mycelium.wapi.wallet.coins.Balance
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.erc20.coins.ERC20Token
import com.mycelium.wapi.wallet.eth.EthAddress
import org.web3j.crypto.Credentials
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
import java.math.BigInteger
import java.util.*

class ERC20Account(private val token: ERC20Token,
                   private val credentials: Credentials? = null,
                   web3jServices: List<HttpService>) : WalletAccount<EthAddress> {
    private val endpoints = ServerEndpoints(web3jServices.toTypedArray()).apply {
        setAllowedEndpointTypes(ServerEndpointType.ALL)
    }
    val receivingAddress = credentials?.let { EthAddress(coinType, it.address) }
    var client: Web3j = buildCurrentEndpoint()
    private fun buildCurrentEndpoint() = Web3j.build(endpoints.currentEndpoint)

    override fun setAllowZeroConfSpending(b: Boolean) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createTx(address: GenericAddress?, amount: Value?, fee: GenericFee?): GenericTransaction {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun signTx(request: GenericTransaction?, keyCipher: KeyCipher?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun broadcastTx(tx: GenericTransaction?): BroadcastResult {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getReceiveAddress() = receivingAddress

    override fun getCoinType() = token // later: accountContext.currency

    override fun getBasedOnCoinType() = coinType

    override fun getAccountBalance(): Balance {
        // gasPrice and gasLimit constants are from https://github.com/web3j/web3j/blob/5001c05f6165d24a3df95760ea8ed8343faf46c4/core/src/main/java/org/web3j/tx/gas/DefaultGasProvider.java
        val erc20Contract = StandardToken.load(token.contractAddress, client, credentials, BigInteger.valueOf(4_100_000_000L), BigInteger.valueOf(9_000_000))
        val result = erc20Contract.balanceOf(receivingAddress!!.addressString).sendAsync()
        return Balance(Value.valueOf(coinType, result.get()), Value.zeroValue(coinType), Value.zeroValue(coinType), Value.zeroValue(coinType))
    }

    override fun isMineAddress(address: GenericAddress?) = address == receivingAddress

    override fun isExchangeable() = true

    override fun getTx(transactionId: ByteArray?): GenericTransaction {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getTxSummary(transactionId: ByteArray?): GenericTransactionSummary {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getTransactionSummaries(offset: Int, limit: Int) = emptyList<GenericTransactionSummary>()

    override fun getTransactionsSince(receivingSince: Long): MutableList<GenericTransactionSummary> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getUnspentOutputViewModels(): MutableList<GenericOutputViewModel> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getLabel(): String = token.name

    override fun setLabel(label: String?) {
    }

    override fun isSpendingUnconfirmed(tx: GenericTransaction?): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun synchronize(mode: SyncMode?): Boolean {
        return try {
            accountBalance
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun getBlockChainHeight(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun canSpend() = true

    override fun isSyncing() = false // TODO implement

    override fun isArchived() = false

    override fun isActive() = true

    override fun archiveAccount() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun activateAccount() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun dropCachedData() {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isVisible() = true

    override fun isDerivedFromInternalMasterseed() = false

    override fun getId(): UUID = UUID.nameUUIDFromBytes(HexUtils.toBytes(token.contractAddress.substring(2)))

    override fun broadcastOutgoingTransactions() = true // TODO implement

    override fun removeAllQueuedTransactions() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun calculateMaxSpendableAmount(minerFeePerKilobyte: Value?, destinationAddress: EthAddress?): Value {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getSyncTotalRetrievedTransactions() = 0 // TODO implement

    override fun getTypicalEstimatedTransactionSize(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getPrivateKey(cipher: KeyCipher?): InMemoryPrivateKey {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getDummyAddress(): EthAddress {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getDummyAddress(subType: String?): EthAddress {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getDependentAccounts() = emptyList<WalletAccount<GenericAddress>>()

    override fun queueTransaction(transaction: GenericTransaction) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}