package com.mycelium.wapi.wallet.eth

import com.mrd.bitlib.crypto.InMemoryPrivateKey
import com.mrd.bitlib.util.BitUtils
import com.mycelium.wapi.wallet.*
import com.mycelium.wapi.wallet.btc.FeePerKbFee
import com.mycelium.wapi.wallet.coins.Balance
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.eth.coins.EthTest
import com.mycelium.wapi.wallet.exceptions.GenericBuildTransactionException
import com.mycelium.wapi.wallet.exceptions.GenericInsufficientFundsException
import com.mycelium.wapi.wallet.genericdb.AccountContextImpl
import org.web3j.crypto.Credentials
import org.web3j.crypto.ECKeyPair
import org.web3j.crypto.RawTransaction
import org.web3j.crypto.TransactionEncoder
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.infura.InfuraHttpService
import org.web3j.utils.Convert
import org.web3j.utils.Numeric
import java.math.BigInteger
import java.util.*

class EthAccount(private val credentials: Credentials,
                 private val accountContext: AccountContextImpl) : WalletAccount<EthAddress> {

    override fun setAllowZeroConfSpending(b: Boolean) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    @Throws(GenericInsufficientFundsException::class, GenericBuildTransactionException::class)
    override fun createTx(toAddress: GenericAddress, value: Value, gasPrice: GenericFee): GenericTransaction {
        val gasPriceLong = (gasPrice as FeePerKbFee).feePerKb.value
        // check whether account has enough funds
        if (value > calculateMaxSpendableAmount(gasPriceLong, null)) {
            throw GenericInsufficientFundsException(Throwable("Insufficient funds to send " + Convert.fromWei(value.value.toBigDecimal(), Convert.Unit.ETHER) +
                    " ether with gas price " + Convert.fromWei(gasPriceLong.toBigDecimal(), Convert.Unit.GWEI) + " gwei"))
        }

        try {
            val nonce = getNonce(credentials.address)
            val rawTransaction =
                    RawTransaction.createEtherTransaction(nonce, BigInteger.valueOf(gasPrice.feePerKb.value), BigInteger.valueOf(21000), toAddress.toString(), BigInteger.valueOf(value.value))
            val ethTransaction = EthTransaction(coinType, toAddress, value, gasPrice)
            ethTransaction.rawTransaction = rawTransaction
            return ethTransaction
        } catch (e: Exception) {
            throw  GenericBuildTransactionException(Throwable(e.localizedMessage))
        }
    }

    @Throws(Exception::class)
    private fun getNonce(address: String): BigInteger {
        val web3j: Web3j = Web3j.build(InfuraHttpService("https://ropsten.infura.io/WKXR51My1g5Ea8Z5Xh3l"))
        val ethGetTransactionCount = web3j.ethGetTransactionCount(address, DefaultBlockParameterName.LATEST)
                .sendAsync()
                .get()

        return ethGetTransactionCount.transactionCount
    }

    override fun signTx(request: GenericTransaction?, keyCipher: KeyCipher?) {
        val rawTransaction = (request as EthTransaction).rawTransaction
        val signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials)
        val hexValue = Numeric.toHexString(signedMessage)
        request.signedHex = hexValue
    }

    override fun broadcastTx(tx: GenericTransaction?): BroadcastResult {
        val web3j: Web3j = Web3j.build(InfuraHttpService("https://ropsten.infura.io/WKXR51My1g5Ea8Z5Xh3l"))
        val ethSendTransaction = web3j.ethSendRawTransaction((tx as EthTransaction).signedHex).sendAsync().get()
        if (ethSendTransaction.hasError()) {
            return BroadcastResult(ethSendTransaction.error.message, BroadcastResultType.REJECTED)
        }
        return BroadcastResult(BroadcastResultType.SUCCESS)
    }

    override fun getReceiveAddress() = EthAddress(coinType, credentials.address)


    override fun getCoinType() = accountContext.currency

    override fun getBasedOnCoinType() = coinType

    private val ethBalanceService = EthBalanceService(credentials.address)

    override fun getAccountBalance() = accountContext.balance

    override fun isMineAddress(address: GenericAddress) =
            address == EthAddress(coinType, "0x60c2A43Cc69658eC4b02a65A07623D7192166F4e")

    override fun isExchangeable() = true

    override fun getTx(transactionId: ByteArray?): GenericTransaction {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getTxSummary(transactionId: ByteArray?): GenericTransactionSummary {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getTransactionSummaries(offset: Int, limit: Int) = emptyList<GenericTransactionSummary>()

    override fun getTransactionsSince(receivingSince: Long) = emptyList<GenericTransactionSummary>()

    override fun getTransactions(offset: Int, limit: Int) = emptyList<GenericTransaction>()

    override fun getUnspentOutputViewModels(): MutableList<GenericOutputViewModel> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getLabel() = accountContext.accountName

    override fun setLabel(label: String?) {
        accountContext.accountName = label!!
    }

    override fun isSpendingUnconfirmed(tx: GenericTransaction?): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun synchronize(mode: SyncMode?): Boolean {
        val succeed = ethBalanceService.updateBalanceCache()
        if (succeed) {
            val balance = Balance(Value.valueOf(EthTest, ethBalanceService.balance),
                    Value.zeroValue(coinType),
                    Value.zeroValue(coinType),
                    Value.zeroValue(coinType))
            accountContext.balance = balance
        }
        return succeed
    }

    override fun getBlockChainHeight(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun canSpend() = true

    override fun isSyncing() = false

    override fun isArchived() = accountContext.archived

    override fun isActive() = !isArchived

    override fun archiveAccount() {
        accountContext.archived = true
        dropCachedData()
    }

    override fun activateAccount() {
        accountContext.archived = false
        dropCachedData()
    }

    override fun dropCachedData() {
        val balance = Balance(Value.zeroValue(coinType),
                Value.zeroValue(coinType),
                Value.zeroValue(coinType),
                Value.zeroValue(coinType))
        accountContext.balance = balance
    }

    override fun isVisible() = true

    override fun isDerivedFromInternalMasterseed() = true

    override fun getId() = credentials.ecKeyPair.toUUID()

    override fun isSynchronizing() = false

    override fun broadcastOutgoingTransactions() = true

    override fun removeAllQueuedTransactions() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun calculateMaxSpendableAmount(gasPrice: Long, ign: EthAddress?) =
            accountBalance.spendable - Value.valueOf(coinType, gasPrice * 21000)

    override fun getSyncTotalRetrievedTransactions() = 0

    override fun getTypicalEstimatedTransactionSize() = 21000

    override fun getPrivateKey(cipher: KeyCipher?): InMemoryPrivateKey {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getDummyAddress() = EthAddress.getDummyAddress(coinType)

    override fun getDummyAddress(subType: String?): EthAddress = dummyAddress

    override fun getDependentAccounts() = emptyList<WalletAccount<GenericAddress>>()

    override fun queueTransaction(transaction: GenericTransaction) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

fun ECKeyPair.toUUID(): UUID = UUID(BitUtils.uint64ToLong(publicKey.toByteArray(), 8), BitUtils.uint64ToLong(
        publicKey.toByteArray(), 16))
