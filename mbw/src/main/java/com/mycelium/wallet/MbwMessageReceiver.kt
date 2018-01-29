package com.mycelium.wallet

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.common.base.CharMatcher
import com.mrd.bitlib.model.NetworkParameters.NetworkType.*
import com.mrd.bitlib.model.OutPoint
import com.mrd.bitlib.model.ScriptOutput
import com.mrd.bitlib.model.Transaction
import com.mrd.bitlib.model.TransactionOutput
import com.mrd.bitlib.util.Sha256Hash
import com.mycelium.modularizationtools.ModuleMessageReceiver
import com.mycelium.spvmodule.IntentContract
import com.mycelium.wallet.WalletApplication.getSpvModuleName
import com.mycelium.wallet.activity.modern.ModernMain
import com.mycelium.wallet.event.SpvSyncChanged
import com.mycelium.wallet.persistence.MetadataStorage
import com.mycelium.wapi.model.TransactionEx
import com.mycelium.wapi.wallet.AesKeyCipher
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.WalletAccount.Type.BCHBIP44
import com.mycelium.wapi.wallet.WalletAccount.Type.BCHSINGLEADDRESS
import com.mycelium.wapi.wallet.WalletManager
import com.mycelium.wapi.wallet.bip44.Bip44Account
import com.mycelium.wapi.wallet.bip44.Bip44BCHAccount
import com.mycelium.wapi.wallet.currency.CurrencyValue
import com.mycelium.wapi.wallet.single.SingleAddressAccount
import com.squareup.otto.Bus
import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.core.TransactionConfidence.ConfidenceType.*
import org.bitcoinj.crypto.ChildNumber
import org.bitcoinj.crypto.DeterministicKey
import org.bitcoinj.crypto.HDKeyDerivation
import java.math.BigDecimal
import java.nio.ByteBuffer
import java.util.*

class MbwMessageReceiver(private val context: Context) : ModuleMessageReceiver {
    private val eventBus: Bus = MbwManager.getInstance(context).eventBus

    override fun onMessage(callingPackageName: String, intent: Intent) {
        when (callingPackageName) {
            getSpvModuleName(BCHBIP44) -> onMessageFromSpvModuleBch(intent)
            else -> Log.e(TAG, "Ignoring unexpected package $callingPackageName calling with intent $intent.")
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun onMessageFromSpvModuleBch(intent: Intent) {
        val walletManager = MbwManager.getInstance(context).getWalletManager(false)
        when (intent.action) {
            "com.mycelium.wallet.receivedTransactions" -> {
                val network = MbwEnvironment.verifyEnvironment(context).network
                val protocolId = when (network.networkType) {
                    PRODNET -> "main"
                    REGTEST -> "regtest"
                    TESTNET -> "test"
                    else -> ""
                }
                val networkBJ = org.bitcoinj.core.NetworkParameters.fromPmtProtocolID(protocolId)
                val transactionsBytes = intent.getSerializableExtra(IntentContract.TRANSACTIONS) as Array<ByteArray>
                val fundingOutputs = bitcoinJ2Bitlib(intent.getSerializableExtra(IntentContract.CONNECTED_OUTPUTS)
                        as Map<String, ByteArray>, networkBJ!!) //Funding outputs

                // Unspent Transaction Output (UTXO)
                val utxoSet = (intent.getSerializableExtra(IntentContract.UTXOS) as Map<String, ByteArray>).keys.map {
                    val parts = it.split(":")
                    val hash = Sha256Hash.fromString(parts[0])
                    val index = parts[1].toInt()
                    OutPoint(hash, index)
                }.toSet()

                if (transactionsBytes.isEmpty()) {
                    Log.d(TAG, "onMessage: received an empty transaction notification")
                    return
                }
                var satoshisReceived = 0L
                var satoshisSent = 0L
                try {
                    for (confTransactionBytes in transactionsBytes) {
                        val transactionAccounts = HashSet<WalletAccount>()
                        val transactionBytesBuffer = ByteBuffer.wrap(confTransactionBytes)
                        val blockHeight = transactionBytesBuffer.int
                        val transactionBytes = ByteArray(transactionBytesBuffer.capacity() - (4 + 8))
                        //Filling up transactionBytes.
                        transactionBytesBuffer.get(transactionBytes, 0, transactionBytes.size)

                        val updateAtTime = transactionBytesBuffer.long

                        val transaction = Transaction.fromBytes(transactionBytes)

                        val connectedOutputs = HashMap<OutPoint, TransactionOutput>()

                        for (input in transaction.inputs) {
                            val connectedOutput = fundingOutputs[input.outPoint] ?: // skip this
                                    continue
                            connectedOutputs.put(input.outPoint, connectedOutput)
                            val address = connectedOutput.script.getAddress(network)

                            val optionalAccount = walletManager.getAccountByAddress(address)
                            if (optionalAccount.isPresent) {
                                val account = walletManager.getAccount(optionalAccount.get())
                                //TODO - uncomment account.storeAddressOldestActivityTime(address, updateAtTime / 1000)
                                if (account.getTransaction(transaction.hash) == null) {
                                    // The transaction is new and relevant for the account.
                                    // We found spending from the account.
                                    satoshisSent += connectedOutput.value
                                    //Should we update lookahead of adresses / Accounts that needs to be look at
                                    //by SPV module ?
                                    transactionAccounts.add(account)
                                    Log.d(TAG, "new transaction $transaction is sending ${connectedOutput.value}sat from ${input.outPoint} of Account ${account.getId()}")
                                }
                            }
                        }

                        for (output in transaction.outputs) {
                            //Transaction received and Change from transaction.
                            val address = output.script.getAddress(network)
                            val optionalAccount = walletManager.getAccountByAddress(address)
                            if (optionalAccount.isPresent) {
                                val account = walletManager.getAccount(optionalAccount.get())
                                //TODO - uncomment account.storeAddressOldestActivityTime(address, updateAtTime / 1000)
                                if (account.getTransaction(transaction.hash) == null) {
                                    // The transaction is new and relevant for the account.
                                    // We found spending from the account.
                                    satoshisReceived += output.value
                                    transactionAccounts.add(account)
                                    Log.d(TAG, "new transaction $transaction is sending ${output.value}sat to ${output.script.getAddress(network)}")
                                }
                            }
                        }
                        for (account in transactionAccounts) {
                            when (blockHeight) {
                                DEAD.value -> Log.e(TAG, "transaction is dead")
                                IN_CONFLICT.value -> Log.e(TAG, "transaction is in conflict")
                                UNKNOWN.value, PENDING.value -> {
                                    //TODO - uncomment account.notifyNewTransactionDiscovered(
                                    //        TransactionEx.fromUnconfirmedTransaction(transactionBytes),
                                    //        connectedOutputs, utxoSet, false)
                                    if(account is Bip44Account) {
                                        createNextAccount(account, walletManager, false)
                                    }
                                }
                                else -> {
                                    val txBJ = org.bitcoinj.core.Transaction(networkBJ, transactionBytes)
                                    val txid = Sha256Hash.fromString(txBJ.hash.toString())
                                    txBJ.updateTime = Date(updateAtTime)
                                    val time = (txBJ.updateTime.time / 1000L).toInt()
                                    val tEx = TransactionEx(txid, blockHeight, time, transactionBytes)
                                    //TODO - uncomment account.notifyNewTransactionDiscovered(tEx, connectedOutputs, utxoSet, false)
                                    // Need to launch synchronisation after we notified of a new transaction
                                    // discovered and updated the lookahead of address to observe when using SPV
                                    // module.

                                    // But before that we might need to create the next account if it does not exist.
                                    if(account is Bip44Account) {
                                        createNextAccount(account, walletManager, false)
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Transaction.TransactionParsingException) {
                    Log.e(TAG, e.message, e)
                }
            }
            "com.mycelium.wallet.notifySatoshisReceived" -> {
                val satoshisReceived = intent.getLongExtra(IntentContract.SATOSHIS_RECEIVED, 0L)
                val satoshisSent = intent.getLongExtra(IntentContract.SATOSHIS_SENT, 0L)
                val mds = MbwManager.getInstance(context).metadataStorage
                val accountsIndex = intent.getIntArrayExtra(IntentContract.ACCOUNTS_INDEX)
                val walletAccounts = mutableListOf<WalletAccount>()
                for(accountIndex in accountsIndex) {
                    walletManager.activeAccounts.filterTo(walletAccounts) {
                        it is Bip44Account && it.accountIndex == accountIndex
                    }
                }
                notifySatoshisReceived(satoshisReceived, satoshisSent, mds, walletAccounts)
            }
            "com.mycelium.wallet.notifySatoshisReceivedSingleAddress" -> {
                val satoshisReceived = intent.getLongExtra(IntentContract.SATOSHIS_RECEIVED, 0L)
                val satoshisSent = intent.getLongExtra(IntentContract.SATOSHIS_SENT, 0L)
                val mds = MbwManager.getInstance(context).metadataStorage
                val guid = intent.getStringExtra(IntentContract.SINGLE_ADDRESS_ACCOUNT_GUID)
                val singleAddressAccount = walletManager.getAccount(UUID.fromString(guid))
                notifySatoshisReceived(satoshisReceived, satoshisSent, mds, listOf(singleAddressAccount))
            }

            "com.mycelium.wallet.blockchainState" -> {
                val bestChainDate = intent.getLongExtra("best_chain_date", 0L)
                val bestChainHeight = intent.getIntExtra("best_chain_height", 0)
                val chainDownloadPercentDone = intent.getIntExtra("chain_download_percent_done", 0)
                // val replaying = intent.getBooleanExtra("replaying", true)
                // val impediments = intent.getStringArrayExtra("impediment")
                walletManager.activeAccounts
                        .filterIsInstance<Bip44BCHAccount?>()
                        .forEach {
                            it!!.blockChainHeight = bestChainHeight
                        }
                // Defines a Handler object that's attached to the UI thread
                val runnable = Runnable {
                    eventBus.post(SpvSyncChanged(Date(bestChainDate), bestChainHeight.toLong(), chainDownloadPercentDone))
                }
                Handler(Looper.getMainLooper()).post(runnable)
            }

            "com.mycelium.wallet.requestPrivateExtendedKeyCoinTypeToMBW" -> {
                val mbwManager = MbwManager.getInstance(context)
                val accountIndex = intent.getIntExtra(IntentContract.ACCOUNT_INDEX_EXTRA, -1)
                Log.d(TAG, "com.mycelium.wallet.requestPrivateExtendedKeyCoinTypeToMBW, " +
                        "accountIndex = $accountIndex")
                if (accountIndex == -19) {
                    Log.e(TAG, "Account Index required!")
                    return
                }
                val masterSeed = mbwManager.getWalletManager(false).getMasterSeed(AesKeyCipher.defaultKeyCipher())

                val masterDeterministicKey : DeterministicKey = HDKeyDerivation.createMasterPrivateKey(masterSeed.bip32Seed)
                val bip44LevelDeterministicKey = HDKeyDerivation.deriveChildKey(
                        masterDeterministicKey, ChildNumber(44, true))
                val coinType = if (mbwManager.network.isTestnet) {
                    1 //Bitcoin Testnet https://github.com/satoshilabs/slips/blob/master/slip-0044.md
                } else {
                    0 //Bitcoin Mainnet https://github.com/satoshilabs/slips/blob/master/slip-0044.md
                }
                val cointypeLevelDeterministicKey =
                        HDKeyDerivation.deriveChildKey(bip44LevelDeterministicKey, ChildNumber(coinType, true))
                val networkParameters = NetworkParameters.fromID(if (mbwManager.network.isTestnet) {
                    NetworkParameters.ID_TESTNET
                } else {
                    NetworkParameters.ID_MAINNET
                })!!
                //val bip39PassphraseList : ArrayList<String> = ArrayList(masterSeed.bip39WordList)
                val service = IntentContract.RequestPrivateExtendedKeyCoinTypeToSPV.createIntent(
                        accountIndex,
                        cointypeLevelDeterministicKey.serializePrivB58(networkParameters),
                        1504664986L) //TODO Change value after test. Nelson
                WalletApplication.sendToSpv(service, BCHBIP44)
            }
            "com.mycelium.wallet.requestSingleAddressPrivateKeyToMBW" -> {
                val _mbwManager = MbwManager.getInstance(context)
                val accountGuid = intent.getStringExtra(IntentContract.SINGLE_ADDRESS_ACCOUNT_GUID)
                Log.d(TAG, "com.mycelium.wallet.requestSingleAddressPrivateKeyToMBW, guid = $accountGuid")
                var account =_mbwManager.getWalletManager(false).getAccount(UUID.fromString(accountGuid)) as? SingleAddressAccount
                if (account == null) {
                    //This is a way to not to pass information that this is a cold storage to BCH module and back
                    account =_mbwManager.getWalletManager(true).getAccount(UUID.fromString(accountGuid)) as SingleAddressAccount
                }

                val privateKey = account.getPrivateKey(AesKeyCipher.defaultKeyCipher())
                if (privateKey == null) {
                    Log.w(TAG, "MbwMessageReceiver.onMessageFromSpvModuleBch, " +
                            "com.mycelium.wallet.requestSingleAddressPrivateKeyToMBW, " +
                            "privateKey must not be null.")
                    return;
                }
                val service = IntentContract.RequestSingleAddressPrivateKeyToSPV.createIntent(accountGuid, privateKey.privateKeyBytes)
                WalletApplication.sendToSpv(service, BCHSINGLEADDRESS)
            }
            null -> Log.w(TAG, "onMessage failed. No action defined.")
            else -> Log.e(TAG, "onMessage failed. Unknown action ${intent.action}")
        }
    }

    private fun createNextAccount(account: Bip44Account, walletManager: WalletManager,
                                  archived: Boolean) {
        if(account.hasHadActivity()
                && !walletManager.doesBip44AccountExists(account.accountIndex + 1)) {
            val newAccountUUID = walletManager.createArchivedGapFiller(AesKeyCipher.defaultKeyCipher(),
                    account.accountIndex + 1, archived)
            MbwManager.getInstance(context).metadataStorage
                    .storeAccountLabel(newAccountUUID, "Account " + (account.accountIndex + 2 /** account index is zero based */))
            walletManager.startSynchronization()
        }
    }

    private fun notifySatoshisReceived(satoshisReceived: Long, satoshisSent: Long, mds: MetadataStorage,
                                       affectedAccounts: Collection<WalletAccount>) {
        val builder = Notification.Builder(context)
                // TODO: bitcoin icon
                .setSmallIcon(R.drawable.holo_dark_ic_action_new_usd_account)
                .setContentTitle(context.getString(R.string.app_name))
        var contentText = "";
        for (account in AccountManager.getActiveAccounts().values) {
            if (account.currencyBasedBalance.receiving.value.compareTo(BigDecimal.ZERO) > 0) {
                contentText += buildLine(R.string.receiving, account.currencyBasedBalance.receiving
                        , mds.getLabelByAccount(account.id))
            }
            if(account.currencyBasedBalance.sending.value.compareTo(BigDecimal.ZERO) > 0) {
                contentText += buildLine(R.string.sending, account.currencyBasedBalance.sending
                        , mds.getLabelByAccount(account.id))
            }
        }
        //something wrong if contentText empty, so shouldn't show anything for avoid crash or not correct work
        if(contentText.isNotEmpty()) {
            contentText = contentText.substring(0, contentText.length - 1)
            val isBigText = CharMatcher.`is`('\n').countIn(contentText) > 0
            val contentTextSmall = if (isBigText) contentText.substring(0, contentText.indexOf('\n')) + "..." else contentText
            val contentTextBig = contentText
            if(isBigText) {
                builder.setStyle(Notification.BigTextStyle().bigText(contentTextBig))
            }
            builder.setContentText(contentTextSmall)

                    .setContentIntent(PendingIntent.getActivity(context, 0, Intent(context, ModernMain::class.java), 0))
                    .setWhen(System.currentTimeMillis())
            //TODO - return sound .setSound(Uri.parse("android.resource://${context.packageName}/${R.raw.coins_received}"))
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(TRANSACTION_NOTIFICATION_ID, builder.build())
        }
    }

    private fun buildLine(action: Int, value: CurrencyValue, label:String): String {
        return context.getString(action, "${value.value.toPlainString()} ${value.currency} ($label)\n")
    }

    private fun bitcoinJ2Bitlib(bitcoinJConnectedOutputs: Map<String, ByteArray>, networkBJ: NetworkParameters): Map<OutPoint, TransactionOutput> {
        val connectedOutputs = HashMap<OutPoint, TransactionOutput>(bitcoinJConnectedOutputs.size)
        for (id in bitcoinJConnectedOutputs.keys) {
            val parts = id.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val hash = Sha256Hash.fromString(parts[0])
            val index = Integer.parseInt(parts[1])
            val outPoint = OutPoint(hash, index)
            val bitcoinJTransactionOutput = org.bitcoinj.core.TransactionOutput(networkBJ, null, bitcoinJConnectedOutputs[id], 0)
            val value = bitcoinJTransactionOutput.value.longValue()
            val scriptBytes = bitcoinJTransactionOutput.scriptBytes
            val transactionOutput = TransactionOutput(value, ScriptOutput.fromScriptBytes(scriptBytes))
            connectedOutputs.put(outPoint, transactionOutput)
        }
        return connectedOutputs
    }

    companion object {
        private val TAG = MbwMessageReceiver::class.java.canonicalName
        @JvmStatic val TRANSACTION_NOTIFICATION_ID = -553794088
    }
}
