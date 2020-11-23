package com.mycelium.wapi.wallet.fio

import com.google.common.base.Optional
import com.mrd.bitlib.crypto.BipDerivationType
import com.mrd.bitlib.crypto.InMemoryPrivateKey
import com.mrd.bitlib.util.HexUtils
import com.mycelium.wapi.wallet.*
import com.mycelium.wapi.wallet.btc.FeePerKbFee
import com.mycelium.wapi.wallet.coins.Balance
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.exceptions.InsufficientFundsException
import com.mycelium.wapi.wallet.fio.FioModule.Companion.DEFAULT_BUNDLED_TXS_NUM
import com.mycelium.wapi.wallet.fio.coins.FIOToken
import fiofoundation.io.fiosdk.FIOSDK
import fiofoundation.io.fiosdk.enums.FioDomainVisiblity
import fiofoundation.io.fiosdk.errors.FIOError
import fiofoundation.io.fiosdk.errors.fionetworkprovider.GetPendingFIORequestsError
import fiofoundation.io.fiosdk.errors.fionetworkprovider.GetSentFIORequestsError
import fiofoundation.io.fiosdk.models.TokenPublicAddress
import fiofoundation.io.fiosdk.models.fionetworkprovider.FIOApiEndPoints
import fiofoundation.io.fiosdk.models.fionetworkprovider.FIORequestContent
import fiofoundation.io.fiosdk.models.fionetworkprovider.SentFIORequestContent
import fiofoundation.io.fiosdk.models.fionetworkprovider.response.PushTransactionResponse
import fiofoundation.io.fiosdk.utilities.Utils
import java.math.BigInteger
import java.text.SimpleDateFormat
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger

class FioAccount(private val fioBlockchainService: FioBlockchainService,
                 private val accountContext: FioAccountContext,
                 private val backing: FioAccountBacking,
                 private val accountListener: AccountListener?,
                 private val privkeyString: String? = null, // null if it's read-only account
                 val walletManager: WalletManager,
                 address: FioAddress? = null) : WalletAccount<FioAddress>, ExportableAccount {
    private val logger: Logger = Logger.getLogger(FioAccount::class.simpleName)
    private val receivingAddress = privkeyString?.let { FioAddress(coinType, FioAddressData(FIOSDK.derivedPublicKey(it))) }
            ?: address!!
    private val balanceService by lazy {
        FioBalanceService(coinType as FIOToken, receivingAddress.toString())
    }

    var registeredFIONames: MutableList<RegisteredFIOName> = accountContext.registeredFIONames?.toMutableList()
            ?: mutableListOf()

    var registeredFIODomains: MutableList<FIODomain> = accountContext.registeredFIODomains?.toMutableList()
            ?: mutableListOf()

    private fun addRegisteredName(nameToAdd: RegisteredFIOName) {
        registeredFIONames.add(nameToAdd)
        accountContext.registeredFIONames = registeredFIONames
    }

    private fun addRegisteredDomain(domain: FIODomain) {
        registeredFIODomains.add(domain)
        accountContext.registeredFIODomains = registeredFIODomains
    }

    @Volatile
    private var syncing = false

    val accountIndex: Int
        get() = accountContext.accountIndex

    fun hasHadActivity() = accountContext.actionSequenceNumber != BigInteger.ZERO

    /**
     * @return expiration date in format "yyyy-MM-dd'T'HH:mm:ss"
     */
    fun registerFIOAddress(fioAddress: String): String? =
            getFioSdk()!!.registerFioAddress(fioAddress, receivingAddress.toString(),
                    getFeeByEndpoint(FIOApiEndPoints.FeeEndPoint.RegisterFioAddress)).getActionTraceResponse()?.expiration?.also {
                addRegisteredName(RegisteredFIOName(fioAddress, convertToDate(it), DEFAULT_BUNDLED_TXS_NUM))
            }

    /**
     * @return expiration date in format "yyyy-MM-dd'T'HH:mm:ss"
     */
    fun registerFIODomain(fioDomain: String): String? =
            getFioSdk()!!.registerFioDomain(fioDomain, receivingAddress.toString(),
                    getFeeByEndpoint(FIOApiEndPoints.FeeEndPoint.RegisterFioDomain)).getActionTraceResponse()?.expiration?.also {
                addRegisteredDomain(FIODomain(fioDomain, convertToDate(it), false))
            }

    fun renewFIOAddress(fioAddress: String): String? =
            getFioSdk()!!.renewFioAddress(fioAddress, getFeeByEndpoint(FIOApiEndPoints.FeeEndPoint.RenewFioAddress))
                    .getActionTraceResponse()?.expiration?.also { expirationDate ->
                        val oldName = registeredFIONames.first { it.name == fioAddress }
                        oldName.expireDate = convertToDate(expirationDate)
                        oldName.bundledTxsNum += DEFAULT_BUNDLED_TXS_NUM
                        accountContext.registeredFIONames = registeredFIONames
                    }

    fun renewFIODomain(fioDomain: String): String? =
            getFioSdk()!!.renewFioDomain(fioDomain, getFeeByEndpoint(FIOApiEndPoints.FeeEndPoint.RenewFioDomain))
                    .getActionTraceResponse()?.expiration?.also { expirationDate ->
                        val oldDomain = registeredFIODomains.first { it.domain == fioDomain }
                        oldDomain.expireDate = convertToDate(expirationDate)
                        accountContext.registeredFIODomains = registeredFIODomains
                    }

    @ExperimentalUnsignedTypes
    fun setDomainVisibility(fioDomain: String, isPublic: Boolean): PushTransactionResponse.ActionTraceResponse? {
        val fiosdk = getFioSdk()
        val fee = fiosdk!!.getFee(FIOApiEndPoints.FeeEndPoint.SetDomainVisibility).fee

        return fiosdk.setFioDomainVisibility(fioDomain, if (isPublic) FioDomainVisiblity.PUBLIC else FioDomainVisiblity.PRIVATE,
                fee).getActionTraceResponse()
    }

    @ExperimentalUnsignedTypes
    fun addPubAddress(fioAddress: String, publicAddresses: List<TokenPublicAddress>): Boolean {
        return try {
            val fiosdk = getFioSdk()
            val actionTraceResponse = fiosdk!!.addPublicAddresses(fioAddress, publicAddresses,
                    fiosdk.getFeeForAddPublicAddress(fioAddress).fee).getActionTraceResponse()
            actionTraceResponse != null && actionTraceResponse.status == "OK"
        } catch (e: FIOError) {
            logger.log(Level.SEVERE, "Add pub address exception", e)
            false
        }
    }

    @ExperimentalUnsignedTypes
    fun recordObtData(fioRequestId: BigInteger, payerFioAddress: String, payeeFioAddress: String,
                      payerTokenPublicAddress: String, payeeTokenPublicAddress: String, amount: Double,
                      chainCode: String, tokenCode: String, obtId: String, memo: String): Boolean {
        val fiosdk = getFioSdk()
        val actionTraceResponse = fiosdk!!.recordObtData(fioRequestId = fioRequestId,
                                                        payerFioAddress = payerFioAddress,
                                                        payeeFioAddress = payeeFioAddress,
                                                        payerTokenPublicAddress = payerTokenPublicAddress,
                                                        payeeTokenPublicAddress = payeeTokenPublicAddress,
                                                        amount = amount,
                                                        chainCode = chainCode,
                                                        tokenCode = tokenCode,
                                                        obtId = obtId,
                                                        maxFee = fiosdk.getFeeForRecordObtData(payerFioAddress).fee,
                                                        memo = memo).getActionTraceResponse()
        return actionTraceResponse != null && actionTraceResponse.status == "sent_to_blockchain"
    }

    private fun getFioNames(): List<RegisteredFIOName> = try {
        FioBlockchainService.getFioNames(receivingAddress.toString())?.fio_addresses?.map {
            val bundledTxsNum = FioBlockchainService.getBundledTxsNum(it.fio_address) ?: DEFAULT_BUNDLED_TXS_NUM
            RegisteredFIOName(it.fio_address, convertToDate(it.expiration), bundledTxsNum)
        } ?: emptyList()
    } catch (e: Exception) {
        emptyList()
    }

    private fun getFioDomains(): List<FIODomain> = try {
        FioBlockchainService.getFioNames(receivingAddress.toString())?.fio_domains?.map {
            FIODomain(it.fio_domain, convertToDate(it.expiration), it.isPublic != 0)
        } ?: emptyList()
    } catch (e: Exception) {
        emptyList()
    }

    private fun convertToDate(fioDateStr: String): Date {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("GMT")
        return sdf.parse(fioDateStr)
    }

    override fun setAllowZeroConfSpending(b: Boolean) {
        // TODO("Not yet implemented")
    }

    override fun createTx(address: Address, amount: Value, fee: Fee, data: TransactionData?): Transaction {
        if (amount > calculateMaxSpendableAmount((fee as FeePerKbFee).feePerKb, address as FioAddress)) {
            throw InsufficientFundsException(Throwable("Invalid amount"))
        }

        return FioTransaction(coinType, address.toString(), amount, fee)
    }

    override fun signTx(request: Transaction?, keyCipher: KeyCipher?) {
    }

    override fun broadcastTx(tx: Transaction?): BroadcastResult {
        val fioTx = tx as FioTransaction
        return try {
            val response = getFioSdk()!!.transferTokens(fioTx.toAddress, fioTx.value.value, fioTx.fee.feePerKb.value)
            val actionTraceResponse = response.getActionTraceResponse()
            if (actionTraceResponse != null && actionTraceResponse.status == "OK") {
                tx.txId = HexUtils.toBytes(response.transactionId)
                backing.putTransaction(-1, System.currentTimeMillis() / 1000, response.transactionId, "",
                        receivingAddress.toString(), fioTx.toAddress, fioTx.value, 0,
                        fioTx.fee.feePerKb, if (fioTx.toAddress == receivingAddress.toString()) -fioTx.fee.feePerKb else
                    -(fioTx.value + fioTx.fee.feePerKb))
                BroadcastResult(BroadcastResultType.SUCCESS)
            } else {
                BroadcastResult("Status: ${actionTraceResponse?.status}", BroadcastResultType.REJECT_INVALID_TX_PARAMS)
            }
        } catch (e: FIOError) {
            e.printStackTrace()
            BroadcastResult(e.toJson(), BroadcastResultType.REJECT_INVALID_TX_PARAMS)
        } catch (e: Exception) {
            e.printStackTrace()
            BroadcastResult(e.message, BroadcastResultType.REJECT_INVALID_TX_PARAMS)
        }
    }

    override fun getReceiveAddress(): Address = receivingAddress

    override fun getCoinType(): CryptoCurrency = accountContext.currency

    override fun getBasedOnCoinType(): CryptoCurrency = coinType

    override fun getAccountBalance(): Balance = accountContext.balance

    override fun isMineAddress(address: Address?): Boolean = address == receiveAddress

    override fun isExchangeable(): Boolean = true

    override fun getTx(transactionId: ByteArray?): Transaction {
        TODO("Not yet implemented")
    }

    override fun getTxSummary(transactionId: ByteArray?): TransactionSummary =
            backing.getTransactionSummary(HexUtils.toHex(transactionId), receiveAddress.toString())!!

    fun getRequestsGroups() = backing.getRequestsGroups()


    fun rejectFundsRequest(fioRequestId: BigInteger, fioName: String): PushTransactionResponse.ActionTraceResponse? {
        val fiosdk = getFioSdk()
        return fiosdk!!.rejectFundsRequest(fioRequestId, fiosdk.getFeeForRejectFundsRequest(fioName).fee).getActionTraceResponse()
    }

    fun requestFunds(payerFioAddress: String, payeeFioAddress: String,
                     payeeTokenPublicAddress: String, amount: Double, memo: String,
                     chainCode: String, tokenCode: String, maxFee: BigInteger,
                     technologyPartnerId: String = "") =
            getFioSdk()!!.requestFunds(payerFioAddress, payeeFioAddress,
                    payeeTokenPublicAddress, amount, chainCode, tokenCode, memo, maxFee, technologyPartnerId)

    override fun getTransactionSummaries(offset: Int, limit: Int) =
            backing.getTransactionSummaries(offset.toLong(), limit.toLong())

    override fun getTransactionsSince(receivingSince: Long): MutableList<TransactionSummary> {
        return mutableListOf()
    }

    override fun getUnspentOutputViewModels(): MutableList<OutputViewModel> {
        return mutableListOf()
    }

    override fun getLabel(): String = accountContext.accountName

    override fun setLabel(label: String?) {
        label?.let {
            accountContext.accountName = it
        }
    }

    override fun isSpendingUnconfirmed(tx: Transaction?): Boolean = false

    override fun synchronize(mode: SyncMode?): Boolean {
        syncing = true
        syncFioRequests()
        syncFioOBT()
        syncFioAddresses()
        syncFioDomains()
        updateBlockHeight()
        syncTransactions()
        updateMappings()
        try {
            val fioBalance = balanceService.getBalance()
            val newBalance = Balance(Value.valueOf(coinType, fioBalance),
                    Value.zeroValue(coinType), Value.zeroValue(coinType), Value.zeroValue(coinType))
            if (newBalance != accountContext.balance) {
                accountContext.balance = newBalance
                accountListener?.balanceUpdated(this)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            logger.log(Level.SEVERE, "update balance exception: ${e.message}")
        }
        syncing = false
        return true
    }

    private fun updateMappings() {
         val fioNameMappings = accountContext.registeredFIONames?.map { fioName ->
             fioName.name to FioBlockchainService
                     .getPubkeysByFioName(fioName.name).map {
                         "${it.chainCode}-${it.tokenCode}" to it.publicAddress
                     }.toMap()
         }?.toMap()
                 ?: return

        walletManager.getAllActiveAccounts().forEach { account ->
            val chainCode = account.basedOnCoinType.symbol.toUpperCase(Locale.US)
            val tokenCode = account.coinType.symbol.toUpperCase(Locale.US)
            accountContext.registeredFIONames?.forEach { fioName ->
                val publicAddress = account.coinType.parseAddress(fioNameMappings[fioName.name]!!["$chainCode-$tokenCode"])
                if (account.isMineAddress(publicAddress)) {
                    backing.insertOrUpdateMapping(fioName.name, publicAddress.toString(), chainCode,
                            tokenCode, account.id)
                }
            }
        }
    }

    private fun renewPendingFioRequests(pendingFioRequests: List<FIORequestContent>) {
        backing.deletePendingRequests()
        backing.putReceivedRequests(pendingFioRequests)
    }

    private fun renewSentFioRequests(sentFioRequests: List<FIORequestContent>) {
        backing.deleteSentRequests()
        backing.putSentRequests(sentFioRequests as List<SentFIORequestContent>)
    }

    private fun syncFioRequests() {
        val fiosdk = getFioSdk()
        try {
            val pendingFioRequests = fiosdk?.getPendingFioRequests() ?: emptyList()
            logger.log(Level.INFO, "Received ${pendingFioRequests.size} pending requests")
            renewPendingFioRequests(pendingFioRequests)
        } catch (ex: FIOError) {
            if (ex.cause is GetPendingFIORequestsError) {
                val cause = ex.cause as GetPendingFIORequestsError
                if (cause.responseError?.code == 404) {
                    logger.log(Level.INFO, "Received 0 pending requests")
                    renewPendingFioRequests(emptyList())
                }
            } else {
                logger.log(Level.SEVERE, "Update FIO requests exception: ${ex.message}", ex)
            }
        }

        try {
            val sentFioRequests = fiosdk?.getSentFioRequests() ?: emptyList()
            renewSentFioRequests(sentFioRequests)
            logger.log(Level.INFO, "Received ${sentFioRequests.size} sent requests")
        } catch (ex: FIOError) {
            if (ex.cause is GetSentFIORequestsError) {
                val cause = ex.cause as GetSentFIORequestsError
                if (cause.responseError?.code == 404) {
                    logger.log(Level.INFO, "Received 0 sent requests")
                    renewSentFioRequests(emptyList())
                }
            } else {
                logger.log(Level.SEVERE, "Update FIO requests exception: ${ex.message}", ex)
            }
        }

    }

    private fun syncFioOBT() {
        // we don't sync obt records for read-only accounts yet
        if (privkeyString == null) return

        try {
            val obtList = fioBlockchainService.getObtData(receivingAddress.toString(), privkeyString!!)
            logger.log(Level.INFO, "Received OBT list with ${obtList.size} items")
            backing.putOBT(obtList)
        } catch (ex: Exception) {
            logger.log(Level.SEVERE, "Update OBT transactions exception: ${ex.message}", ex)
        }
    }

    private fun syncFioAddresses() {
        val fioNames = getFioNames()
        fioNames.forEach {
            addOrUpdateRegisteredName(it)
        }
    }

    private fun addOrUpdateRegisteredName(nameToAdd: RegisteredFIOName) {
        if (nameToAdd.name in registeredFIONames.map { it.name }) {
            updateRegisteredName(nameToAdd)
        } else {
            addRegisteredName(nameToAdd)
        }
    }

    private fun updateRegisteredName(newName: RegisteredFIOName) {
        val oldName = registeredFIONames.first { it.name == newName.name }
        oldName.expireDate = newName.expireDate
        oldName.bundledTxsNum = newName.bundledTxsNum
        accountContext.registeredFIONames = registeredFIONames
    }

    private fun syncFioDomains() {
        val fioDomains = getFioDomains()
        fioDomains.forEach {
            addOrUpdateRegisteredDomain(it)
        }
    }

    private fun addOrUpdateRegisteredDomain(domainToAdd: FIODomain) {
        if (domainToAdd.domain in registeredFIODomains.map { it.domain }) {
            updateRegisteredDomain(domainToAdd)
        } else {
            addRegisteredDomain(domainToAdd)
        }
    }

    private fun updateRegisteredDomain(newDomain: FIODomain) {
        val oldDomain = registeredFIODomains.first { it.domain == newDomain.domain }
        oldDomain.expireDate = newDomain.expireDate
        oldDomain.isPublic = newDomain.isPublic
        accountContext.registeredFIODomains = registeredFIODomains
    }

    private fun updateBlockHeight() {
        accountContext.blockHeight = fioBlockchainService.getLatestBlock()?.toInt()
                ?: accountContext.blockHeight
    }

    private fun syncTransactions() {
        fioBlockchainService.getTransactions(receivingAddress.toString(), accountContext.blockHeight.toBigInteger()).forEach {
            try {
                backing.putTransaction(it.blockNumber.toInt(), it.timestamp, it.txid, "",
                        it.fromAddress, it.toAddress, it.sum,
                        kotlin.math.max(accountContext.blockHeight - it.blockNumber.toInt(), 0),
                        it.fee, it.transferred, it.memo)
            } catch (e: Exception) {
                e.printStackTrace()
                logger.log(Level.INFO, "asdaf syncTransactions exception: ${e.message}")
            }
        }

        accountContext.actionSequenceNumber =
                fioBlockchainService.getAccountActionSeqNumber(Utils.generateActor(receivingAddress.toString()))
                        ?: accountContext.actionSequenceNumber
    }

    override fun getBlockChainHeight(): Int = accountContext.blockHeight

    override fun canSpend(): Boolean = privkeyString != null

    override fun canSign(): Boolean = false

    override fun isSyncing(): Boolean = syncing

    override fun isArchived(): Boolean = accountContext.archived

    override fun isActive(): Boolean = !isArchived

    override fun archiveAccount() {
        accountContext.archived = true
        dropCachedData()
    }

    override fun activateAccount() {
        accountContext.archived = false
        dropCachedData()
    }

    override fun dropCachedData() {
        accountContext.balance = Balance.getZeroBalance(coinType)
        accountContext.actionSequenceNumber = BigInteger.ZERO
    }

    override fun isVisible(): Boolean = true

    override fun isDerivedFromInternalMasterseed(): Boolean = privkeyString != null

    override fun getId(): UUID = accountContext.uuid

    override fun broadcastOutgoingTransactions(): Boolean = true

    override fun removeAllQueuedTransactions() {
    }

    override fun calculateMaxSpendableAmount(minerFeePerKilobyte: Value?, destinationAddress: FioAddress?): Value {
        val spendableWithFee = accountBalance.spendable - (minerFeePerKilobyte
                ?: Value.zeroValue(coinType))
        return if (spendableWithFee.isNegative()) Value.zeroValue(coinType) else spendableWithFee
    }

    override fun getSyncTotalRetrievedTransactions(): Int = 0

    override fun getTypicalEstimatedTransactionSize(): Int = 0

    override fun getPrivateKey(cipher: KeyCipher?): InMemoryPrivateKey {
        TODO("Not yet implemented")
    }

    override fun getDummyAddress(): FioAddress = FioAddress(coinType, FioAddressData(""))

    override fun getDummyAddress(subType: String?): FioAddress = dummyAddress

    override fun getDependentAccounts(): MutableList<WalletAccount<Address>> {
        return mutableListOf()
    }

    override fun queueTransaction(transaction: Transaction) {
        TODO("Not yet implemented")
    }

    fun getTransferTokensFee() = getFioSdk()!!.getFee(FIOApiEndPoints.FeeEndPoint.TransferTokens).fee

    fun getFeeByEndpoint(endpoint: FIOApiEndPoints.FeeEndPoint) = getFioSdk()!!.getFee(endpoint).fee

    override fun getExportData(cipher: KeyCipher): ExportableAccount.Data =
            ExportableAccount.Data(Optional.fromNullable(privkeyString),
                    mutableMapOf<BipDerivationType, String>().apply {
                        this[BipDerivationType.BIP44] = receivingAddress.toString()
                    })

    private fun getFioSdk(): FIOSDK? = privkeyString?.let { fioBlockchainService.getFioSdk(it) }
}

data class RecordObtData(
        var payerFioAddress: String,
        var payeeFioAddress: String,
        var payerTokenPublicAddress: String,
        var payeeTokenPublicAddress: String,
        var amount: Double,
        var chainCode: String,
        var tokenCode: String,
        var obtId: String,
        var memo: String)