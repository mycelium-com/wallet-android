package com.mycelium.wallet.modularisation


import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.database.Cursor
import android.net.Uri
import android.os.Handler
import android.widget.Toast
import com.google.common.base.Optional
import com.mrd.bitlib.model.Address
import com.mrd.bitlib.util.Sha256Hash
import com.mycelium.spvmodule.IntentContract
import com.mycelium.spvmodule.providers.TransactionContract
import com.mycelium.spvmodule.providers.TransactionContract.AccountBalance
import com.mycelium.spvmodule.providers.TransactionContract.CalculateMaxSpendable
import com.mycelium.spvmodule.providers.TransactionContract.CurrentReceiveAddress
import com.mycelium.spvmodule.providers.TransactionContract.EstimateFeeFromTransferrableAmount
import com.mycelium.spvmodule.providers.TransactionContract.GetMaxFundsTransferrable
import com.mycelium.spvmodule.providers.TransactionContract.GetPrivateKeysCount
import com.mycelium.spvmodule.providers.TransactionContract.GetSyncProgress
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.WalletApplication
import com.mycelium.wallet.WalletApplication.getSpvModuleName
import com.mycelium.wallet.modularisation.BCHHelper.*
import com.mycelium.wapi.model.IssuedKeysInfo
import com.mycelium.wapi.model.TransactionDetails
import com.mycelium.wapi.model.TransactionSummary
import com.mycelium.wapi.wallet.ConfirmationRiskProfileLocal
import com.mycelium.wapi.wallet.SpvBalanceFetcher
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.bip44.HDAccount
import com.mycelium.wapi.wallet.currency.CurrencyBasedBalance
import com.mycelium.wapi.wallet.currency.ExactBitcoinCashValue
import com.mycelium.wapi.wallet.currency.ExactCurrencyValue
import java.math.BigDecimal
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.min
import com.mycelium.spvmodule.providers.TransactionContract.TransactionSummary as SpvTxSummary

class SpvBchFetcher(private val context: Context) : SpvBalanceFetcher {
    private val sharedPreference = context.getSharedPreferences("spvbalancefetcher", Context.MODE_PRIVATE)
    @Volatile
    private var syncProgress = 0f
    @Volatile
    private var lastSyncProgressTime = 0L

    override fun retrieveByHdAccountIndex(id: String, accountIndex: Int): CurrencyBasedBalance {
        val uri = AccountBalance.CONTENT_URI(getSpvModuleName(WalletAccount.Type.BCHBIP44)).buildUpon().appendEncodedPath(id).build()
        val selection = AccountBalance.SELECTION_ACCOUNT_INDEX
        return retrieveBalance(uri, selection, "" + accountIndex)
    }

    override fun retrieveByUnrelatedAccountId(id: String): CurrencyBasedBalance {
        val uri = AccountBalance.CONTENT_URI(getSpvModuleName(WalletAccount.Type.BCHSINGLEADDRESS)).buildUpon().appendEncodedPath(id).build()
        val selection = AccountBalance.SELECTION_GUID
        return retrieveBalance(uri, selection, id)
    }

    private fun retrieveBalance(uri: Uri, selection: String, selectionArg: String): CurrencyBasedBalance {
        context.contentResolver.query(uri, null, selection, arrayOf(selectionArg), null).use {
            while (it?.moveToLast() == true) {
                return from(it)
            }
        }
        return CurrencyBasedBalance.ZERO_BITCOIN_CASH_BALANCE
    }

    private fun from(cursor: Cursor): CurrencyBasedBalance {
        // String id = cursor.getString(cursor.getColumnIndex(TransactionContract.AccountBalance._ID));
        val confirmed = cursor.getLong(cursor.getColumnIndex(AccountBalance.CONFIRMED))
        val receiving = cursor.getLong(cursor.getColumnIndex(AccountBalance.RECEIVING))
        val sending = cursor.getLong(cursor.getColumnIndex(AccountBalance.SENDING))

        return CurrencyBasedBalance(ExactBitcoinCashValue.from(confirmed),
                ExactBitcoinCashValue.from(sending), ExactBitcoinCashValue.from(receiving))
    }

    private fun retrieveAddresses(toAddress: String) : List<Address> =
            toAddress.split(",".toRegex()).map { Address.fromString(it) }

    private fun retrieveTransactionSummary(uri: Uri, selection: String, selectionArg: String) =
            retrieveTransactionSummary(uri, selection, arrayOf(selectionArg))

    private fun retrieveTransactionSummary(uri: Uri, selection: String, selectionArgs: Array<String>): List<TransactionSummary> {
        return try {
            val transactionSummariesList = ArrayList<TransactionSummary>()
            context.contentResolver.query(uri, null, selection, selectionArgs, null).use {
                while (it?.moveToNext() == true) {
                    val txSummary = transactionSummaryFrom(it)
                    transactionSummariesList.add(txSummary)
                }
            }
            transactionSummariesList
        } catch (e: Exception) {
            Handler(context.mainLooper).post {
                Toast.makeText(context,
                        context.getString(R.string.transactions_loading_from_module_error), Toast.LENGTH_LONG).show()
            }
            emptyList()
        }
    }

    override fun retrieveTransactionsSummaryByHdAccountIndex(id: String, accountIndex: Int): List<TransactionSummary> {
        val uri = SpvTxSummary.CONTENT_URI(getSpvModuleName(WalletAccount.Type.BCHBIP44))
        val selection = SpvTxSummary.SELECTION_ACCOUNT_INDEX
        return retrieveTransactionSummary(uri, selection, "" + accountIndex)
    }

    override fun retrieveTransactionsSummaryByHdAccountIndex(id: String?, accountIndex: Int, since: Long): List<TransactionSummary> {
        val uri = SpvTxSummary.CONTENT_URI(getSpvModuleName(WalletAccount.Type.BCHBIP44))
        val selection = SpvTxSummary.SELECTION_ACCOUNT_INDEX_SINCE
        return retrieveTransactionSummary(uri, selection, arrayOf("" + accountIndex, "" + since))
    }

    override fun retrieveTransactionsSummaryByHdAccountIndex(id: String, accountIndex: Int,
                                                             offset: Int, limit: Int): List<TransactionSummary> {
        val fullList = retrieveTransactionsSummaryByHdAccountIndex(id, accountIndex)
        return parseTransactionsSummary(fullList, offset, limit)
    }

    override fun retrieveTransactionsSummaryByUnrelatedAccountId(id: String): List<TransactionSummary> {
        val uri = SpvTxSummary.CONTENT_URI(getSpvModuleName(WalletAccount.Type.BCHSINGLEADDRESS))
        val selection = SpvTxSummary.SELECTION_SINGLE_ADDRESS_ACCOUNT_GUID
        return retrieveTransactionSummary(uri, selection, id)
    }

    override fun retrieveTransactionsSummaryByUnrelatedAccountId(id: String, since: Long): List<TransactionSummary> {
        val uri = SpvTxSummary.CONTENT_URI(getSpvModuleName(WalletAccount.Type.BCHSINGLEADDRESS))
        val selection = SpvTxSummary.SELECTION_SINGLE_ADDRESS_ACCOUNT_GUID_SINCE
        return retrieveTransactionSummary(uri, selection, arrayOf(id, "" + since))
    }

    override fun retrieveTransactionsSummaryByUnrelatedAccountId(id: String, offset: Int, limit: Int)
            : List<TransactionSummary> {
        val fullList = retrieveTransactionsSummaryByUnrelatedAccountId(id)
        return parseTransactionsSummary(fullList, offset, limit)
    }

    private fun parseTransactionsSummary(fullList: List<TransactionSummary>, offset: Int, limit: Int): List<TransactionSummary> {
        return if (offset >= fullList.size) {
            Collections.emptyList()
        } else {
            val toIndex = min(offset + limit, fullList.size)
            fullList.subList(offset, toIndex)
        }
    }

    private fun transactionSummaryFrom(cursor: Cursor): TransactionSummary {
        val rawTxId = cursor.getString(cursor.getColumnIndex(TransactionContract.TransactionSummary._ID))
        val txId = Sha256Hash.fromString(rawTxId)
        val rawValue = cursor.getString(cursor.getColumnIndex(TransactionContract.TransactionSummary.VALUE))
        val bdValue = BigDecimal(rawValue)
        val value = ExactCurrencyValue.from(bdValue, "BCH")
        val rawIsIncoming = cursor.getInt(cursor.getColumnIndex(TransactionContract.TransactionSummary.IS_INCOMING))
        val isIncoming = rawIsIncoming == 1
        val time = cursor.getLong(cursor.getColumnIndex(TransactionContract.TransactionSummary.TIME))
        val height = cursor.getInt(cursor.getColumnIndex(TransactionContract.TransactionSummary.HEIGHT))
        val confirmations = cursor.getInt(cursor.getColumnIndex(TransactionContract.TransactionSummary.CONFIRMATIONS))
        val rawIsQueuedOutgoing = cursor.getInt(cursor.getColumnIndex(TransactionContract.TransactionSummary.IS_QUEUED_OUTGOING))
        val isQueuedOutgoing = rawIsQueuedOutgoing == 1

        var confirmationRiskProfile: ConfirmationRiskProfileLocal? = null
        val unconfirmedChainLength = cursor.getInt(cursor.getColumnIndex(TransactionContract.TransactionSummary.CONFIRMATION_RISK_PROFILE_LENGTH))
        if (unconfirmedChainLength > -1) {
            val hasRbfRisk = cursor.getInt(cursor.getColumnIndex(TransactionContract.TransactionSummary.CONFIRMATION_RISK_PROFILE_LENGTH)) == 1
            val isDoubleSpend = cursor.getInt(cursor.getColumnIndex(TransactionContract.TransactionSummary.CONFIRMATION_RISK_PROFILE_LENGTH)) == 1
            confirmationRiskProfile = ConfirmationRiskProfileLocal(unconfirmedChainLength, hasRbfRisk, isDoubleSpend)
        }

        val rawDestinationAddress = cursor.getString(cursor.getColumnIndex(TransactionContract.TransactionSummary.DESTINATION_ADDRESS))
        val destinationAddress =
                if (rawDestinationAddress?.isEmpty() == false) {
                    Optional.of(Address.fromString(rawDestinationAddress))
                } else {
                    Optional.absent()
                }
        val rawToAddresses = cursor.getString(cursor.getColumnIndex(TransactionContract.TransactionSummary.TO_ADDRESSES))
        val toAddresses = rawToAddresses?.split(",")?.map { Address.fromString(it) } ?: ArrayList<Address>()

        return TransactionSummary(txId, value, isIncoming, time, height, confirmations, isQueuedOutgoing,
                confirmationRiskProfile, destinationAddress, toAddresses)
    }

    override fun retrieveTransactionDetails(txid: Sha256Hash): TransactionDetails? {
        var transactionDetails: TransactionDetails? = null
        val mbwManager = MbwManager.getInstance(context)
        val uri = Uri.withAppendedPath(TransactionContract.TransactionDetails.CONTENT_URI(
                WalletApplication.getSpvModuleName(mbwManager.selectedAccount.type)), txid.toHex())
        val selection = TransactionContract.TransactionDetails.SELECTION_ACCOUNT_INDEX
        val account = mbwManager.selectedAccount
        val contentResolver = context.contentResolver
        val selectionArgs = if ((account.type == WalletAccount.Type.BTCBIP44 || account.type == WalletAccount.Type.BCHBIP44)
                && mbwManager.selectedAccount.isDerivedFromInternalMasterseed) {
            val accountIndex = (mbwManager.selectedAccount as HDAccount).accountIndex
            arrayOf(Integer.toString(accountIndex))
        } else {
            val accountId = account.id
            arrayOf(accountId.toString())
        }
        contentResolver.query(uri, null, selection, selectionArgs, null).use {
            if (it?.moveToNext() == true) {
                transactionDetails = transactionDetailsFrom(it)
            }
        }
        return transactionDetails
    }

    private fun transactionDetailsFrom(cursor: Cursor): TransactionDetails {
        val rawTxId = cursor.getString(cursor.getColumnIndex(TransactionContract.TransactionDetails._ID))
        val hash = Sha256Hash.fromString(rawTxId)
        val height = cursor.getInt(cursor.getColumnIndex(TransactionContract.TransactionDetails.HEIGHT))
        val time = cursor.getInt(cursor.getColumnIndex(TransactionContract.TransactionDetails.TIME))
        val rawSize = cursor.getInt(cursor.getColumnIndex(TransactionContract.TransactionDetails.RAW_SIZE))

        val rawInputs = cursor.getString(cursor.getColumnIndex(TransactionContract.TransactionDetails.INPUTS))
        val rawOutputs = cursor.getString(cursor.getColumnIndex(TransactionContract.TransactionDetails.OUTPUTS))

        val inputs = extract(rawInputs)
        val outputs = extract(rawOutputs)
        return TransactionDetails(hash, height, time, inputs, outputs, rawSize)
    }

    private fun extract(data: String?): Array<TransactionDetails.Item> {
        val result = ArrayList<TransactionDetails.Item>()
        data?.split(",")?.forEach {
            val inParts = it.split(" BCH")
            val value = java.lang.Long.valueOf(inParts[0])
            val address = Address.fromString(inParts[1])
            result.add(TransactionDetails.Item(address, value, false))
        } ?: ArrayList<TransactionDetails.Item>()
        return result.toTypedArray()
    }

    override fun requestTransactionsAsync(accountId: Int) {
        val service = IntentContract.ReceiveTransactions.createIntent(accountId)
        WalletApplication.sendToSpv(service, WalletAccount.Type.BCHBIP44)
    }

    override fun requestTransactionsFromUnrelatedAccountAsync(guid: String, accountType : Int) {
        val service = IntentContract.ReceiveTransactionsUnrelated.createIntent(guid, accountType)
        WalletApplication.sendToSpv(service, WalletAccount.Type.BCHSINGLEADDRESS)
    }

    override fun forceCleanCache() {
        val service = IntentContract.ForceCacheClean.createIntent()
        WalletApplication.sendToSpv(service, WalletAccount.Type.BCHBIP44)
    }

    override fun requestHdWalletAccountRemoval(accountIndex: Int) {
        val service = IntentContract.RemoveHdWalletAccount.createIntent(accountIndex)
        WalletApplication.sendToSpv(service, WalletAccount.Type.BCHBIP44)
    }
    override fun requestUnrelatedAccountRemoval(guid: String)  {
        val service = IntentContract.RemoveUnrelatedAccount.createIntent(guid)
        WalletApplication.sendToSpv(service, WalletAccount.Type.BCHSINGLEADDRESS)
    }

    override fun getSyncProgressPercents(): Float {
        // optimization, some time very often getSyncProgressPercents called from ui thread
        if(System.currentTimeMillis() - lastSyncProgressTime < 20000) {
            return syncProgress
        }
        val uri = GetSyncProgress.CONTENT_URI(getSpvModuleName(WalletAccount.Type.BCHBIP44)).buildUpon().build()
        context.contentResolver.query(uri, null, null, null, null).use {
            syncProgress = if (it?.moveToFirst() == true) {
                it.getFloat(0)
            } else {
                0f
            }
            if (syncProgress == 100f) {
                sharedPreference.edit().putBoolean("is_first_sync", false).apply()
            }
            lastSyncProgressTime = System.currentTimeMillis()
            return syncProgress
        }
    }

    override fun isAccountSynced(account: WalletAccount?): Boolean {
        val sharedPreferences = context.getSharedPreferences(BCH_PREFS, MODE_PRIVATE)
        return sharedPreferences.getBoolean(ALREADY_FOUND_ACCOUNT + account!!.id.toString(), false)
    }

    override fun isAccountVisible(account: WalletAccount?): Boolean {
        val sharedPreferences = context.getSharedPreferences(BCH_PREFS, MODE_PRIVATE)
        return sharedPreferences.getBoolean(IS_ACCOUNT_VISIBLE + account!!.id.toString(), false)
    }

    override fun setVisible(account: WalletAccount?) {
        val sharedPreferences = context.getSharedPreferences(BCH_PREFS, MODE_PRIVATE)
        sharedPreferences.edit()
                .putBoolean(IS_ACCOUNT_VISIBLE + account!!.id.toString(), true)
                .apply()
    }

    override fun getCurrentReceiveAddress(accountIndex: Int): Address? {
        val uri = CurrentReceiveAddress.CONTENT_URI(getSpvModuleName(WalletAccount.Type.BCHBIP44)).buildUpon().build()
        val selection = CurrentReceiveAddress.SELECTION_ACCOUNT_INDEX
        context.contentResolver.query(uri, null, selection, arrayOf("" + accountIndex), null).use {
            return if (it?.moveToFirst() == true) {
                val address = it.getString(it.getColumnIndex(CurrentReceiveAddress.ADDRESS))
                Address.fromString(address)
            } else {
                null
            }
        }
    }

    override fun getCurrentReceiveAddressUnrelated(guid: String): Address? {
        val uri = CurrentReceiveAddress.CONTENT_URI(getSpvModuleName(WalletAccount.Type.BCHBIP44)).buildUpon().build()
        val selection = CurrentReceiveAddress.SELECTION_UNRELATED
        context.contentResolver.query(uri, null, selection, arrayOf(guid), null).use {
            return if (it?.moveToFirst() == true) {
                val address = it.getString(it.getColumnIndex(CurrentReceiveAddress.ADDRESS))
                Address.fromString(address)
            } else {
                null
            }
        }
    }

    override fun getPrivateKeysCount(accountIndex: Int): IssuedKeysInfo {
        val uri = GetPrivateKeysCount.CONTENT_URI(getSpvModuleName(WalletAccount.Type.BCHBIP44)).buildUpon().build()
        val selection = GetPrivateKeysCount.SELECTION_ACCOUNT_INDEX
        context.contentResolver.query(uri, null, selection, arrayOf("" + accountIndex), null).use {
            return if (it?.moveToFirst() == true) {
                IssuedKeysInfo(it.getInt(0), it.getInt(1))
            } else {
                IssuedKeysInfo(0, 0)
            }
        }
    }

    override fun getPrivateKeysCountUnrelated(guid: String): IssuedKeysInfo {
        val uri = GetPrivateKeysCount.CONTENT_URI(getSpvModuleName(WalletAccount.Type.BCHBIP44)).buildUpon().build()
        val selection = GetPrivateKeysCount.SELECTION_UNRELATED
        context.contentResolver.query(uri, null, selection, arrayOf(guid), null).use {
            return if (it?.moveToFirst() == true) {
                IssuedKeysInfo(it.getInt(0), it.getInt(1))
            } else {
                IssuedKeysInfo(0, 0)
            }
        }
    }

    override fun calculateMaxSpendableAmount(accountIndex: Int, txFee: String, txFeeFactor: Float): Long {
        val uri = CalculateMaxSpendable.CONTENT_URI(getSpvModuleName(WalletAccount.Type.BCHBIP44)).buildUpon().build()
        val selection = CalculateMaxSpendable.SELECTION_HD

        context.contentResolver.query(uri, null, selection, arrayOf("" + accountIndex, txFee, "" + txFeeFactor), null).use {
            return if (it?.moveToFirst() == true) {
                it.getLong(2)
            } else {
                0
            }
        }

    }

    override fun calculateMaxSpendableAmountUnrelatedAccount(guid: String, txFee: String, txFeeFactor: Float): Long {
        val uri = CalculateMaxSpendable.CONTENT_URI(getSpvModuleName(WalletAccount.Type.BCHSINGLEADDRESS)).buildUpon().build()
        val selection = CalculateMaxSpendable.SELECTION_SA

        context.contentResolver.query(uri, null, selection, arrayOf(guid, txFee, "" + txFeeFactor), null).use {
            return if (it?.moveToFirst() == true) {
                it.getLong(2)
            } else {
                0
            }
        }

    }


    override fun getMaxFundsTransferrable(accountIndex: Int): Long {
        val uri = TransactionContract.GetMaxFundsTransferrable.CONTENT_URI(getSpvModuleName(WalletAccount.Type.BCHBIP44)).buildUpon().build()
        val selection = GetMaxFundsTransferrable.SELECTION_HD

        context.contentResolver.query(uri, null, selection, arrayOf("" + accountIndex), null).use {
            return if (it?.moveToFirst() == true) {
                it.getLong(0)
            } else {
                0
            }
        }
    }

    override fun getMaxFundsTransferrableUnrelatedAccount(guid: String): Long {
        val uri = TransactionContract.GetMaxFundsTransferrable.CONTENT_URI(getSpvModuleName(WalletAccount.Type.BCHSINGLEADDRESS)).buildUpon().build()
        val selection = GetMaxFundsTransferrable.SELECTION_UNRELATED

        context.contentResolver.query(uri, null, selection, arrayOf(guid), null).use {
            return if (it?.moveToFirst() == true) {
                it.getLong(0)
            } else {
                0
            }
        }

    }

    override fun estimateFeeFromTransferrableAmount(accountIndex: Int, amountSatoshis: Long, txFee: String?, txFeeFactor: Float): Long {
        val uri = TransactionContract.EstimateFeeFromTransferrableAmount.CONTENT_URI(getSpvModuleName(WalletAccount.Type.BCHBIP44)).buildUpon().build()
        val selection = EstimateFeeFromTransferrableAmount.SELECTION_HD

        context.contentResolver.query(uri, null, selection, arrayOf("" + accountIndex, txFee, "" + txFeeFactor, "" + amountSatoshis), null).use {
            return if (it?.moveToFirst() == true) {
                it.getLong(0)
            } else {
                0
            }
        }
    }

    override fun estimateFeeFromTransferrableAmountUnrelatedAccount(guid: String?, amountSatoshis: Long, txFee: String?, txFeeFactor: Float): Long {
        val uri = TransactionContract.EstimateFeeFromTransferrableAmount.CONTENT_URI(getSpvModuleName(WalletAccount.Type.BCHSINGLEADDRESS)).buildUpon().build()
        val selection = EstimateFeeFromTransferrableAmount.SELECTION_UNRELATED

        context.contentResolver.query(uri, null, selection, arrayOf(guid, txFee, "" + txFeeFactor, "" + amountSatoshis), null).use {
            return if (it?.moveToFirst() == true) {
                it.getLong(0)
            } else {
                0
            }
        }
    }
}
