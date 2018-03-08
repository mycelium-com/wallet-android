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
import com.mrd.bitlib.StandardTransactionBuilder
import com.mrd.bitlib.model.Address
import com.mycelium.modularizationtools.CommunicationManager
import com.mycelium.modularizationtools.ModuleMessageReceiver
import com.mycelium.modularizationtools.model.Module
import com.mycelium.spvmodule.IntentContract
import com.mycelium.wallet.WalletApplication.getSpvModuleName
import com.mycelium.wallet.activity.modern.ModernMain
import com.mycelium.wallet.event.SpvSendFundsResult
import com.mycelium.wallet.event.SpvSyncChanged
import com.mycelium.wallet.persistence.MetadataStorage
import com.mycelium.wapi.wallet.AesKeyCipher
import com.mycelium.wapi.wallet.KeyCipher
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.WalletAccount.Type.BCHBIP44
import com.mycelium.wapi.wallet.WalletAccount.Type.BCHSINGLEADDRESS
import com.mycelium.wapi.wallet.WalletManager
import com.mycelium.wapi.wallet.bip44.Bip44Account
import com.mycelium.wapi.wallet.bip44.Bip44BCHAccount
import com.mycelium.wapi.wallet.currency.CurrencyValue
import com.mycelium.wapi.wallet.single.SingleAddressAccount
import com.squareup.otto.Bus
import org.bitcoinj.core.*
import org.bitcoinj.crypto.ChildNumber
import org.bitcoinj.crypto.DeterministicKey
import org.bitcoinj.crypto.HDKeyDerivation
import org.bitcoinj.signers.LocalTransactionSigner
import org.bitcoinj.signers.TransactionSigner
import org.bitcoinj.wallet.KeyBag
import org.bitcoinj.wallet.KeyChainGroup
import org.bitcoinj.wallet.RedeemData
import java.math.BigDecimal
import java.util.*
import kotlin.collections.ArrayList

class MbwMessageReceiver(private val context: Context) : ModuleMessageReceiver {
    private val eventBus: Bus = MbwManager.getInstance(context).eventBus

    override fun onMessage(callingPackageName: String, intent: Intent) {
        when (callingPackageName) {
            getSpvModuleName(BCHBIP44) -> onMessageFromSpvModuleBch(intent, getModule(callingPackageName))
            else -> Log.e(TAG, "Ignoring unexpected package $callingPackageName calling with intent $intent.")
        }
    }

    private fun getModule(packageName: String): Module? {
        CommunicationManager.getInstance(context).pairedModules.forEach {
            if (it.modulePackage == packageName) {
                return it
            }
        }
        return null
    }

    @Suppress("UNCHECKED_CAST")
    private fun onMessageFromSpvModuleBch(intent: Intent, module: Module?) {
        val walletManager = MbwManager.getInstance(context).getWalletManager(false)
        when (intent.action) {
            "com.mycelium.wallet.notifySatoshisReceived" -> {
                val accountsIndex = intent.getIntArrayExtra(IntentContract.ACCOUNTS_INDEX)
                val walletAccounts = mutableListOf<WalletAccount>()
                for(accountIndex in accountsIndex) {
                    walletManager.activeAccounts.filterTo(walletAccounts) {
                        it is Bip44BCHAccount && it.accountIndex == accountIndex
                    }
                }
                notifySatoshisReceived()
            }
            "com.mycelium.wallet.notifySatoshisReceivedSingleAddress" -> {
                notifySatoshisReceived()
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
                Handler(Looper.getMainLooper()).post {
                    eventBus.post(SpvSyncChanged(module, Date(bestChainDate), bestChainHeight.toLong(), chainDownloadPercentDone))
                }
            }

            "com.mycelium.wallet.requestAccountLevelKeysToMBW" -> {
                val mbwManager = MbwManager.getInstance(context)
                val accountIndexes = intent.getIntArrayExtra(IntentContract.ACCOUNT_INDEXES_EXTRA)
                val creationTimeSeconds = intent.getLongExtra(IntentContract.CREATIONTIMESECONDS, 0)
                Log.d(TAG, "com.mycelium.wallet.requestAccountLevelKeysToMBW, " +
                        "accountIndexes.size = ${accountIndexes.size}")
                if (accountIndexes == null) {
                    Log.e(TAG, "Account Indexes required!")
                    return
                }
                val accountLevelKeys: MutableList<String> = mutableListOf()
                val accountIndexesIterator = accountIndexes.iterator()
                while (accountIndexesIterator.hasNext()) {
                    val accountIndex = accountIndexesIterator.nextInt()
                    val masterSeed = mbwManager.getWalletManager(false)
                            .getMasterSeed(AesKeyCipher.defaultKeyCipher())
                    val masterDeterministicKey : DeterministicKey = HDKeyDerivation.createMasterPrivateKey(masterSeed.bip32Seed)
                    masterDeterministicKey.creationTimeSeconds
                    val bip44LevelDeterministicKey = HDKeyDerivation.deriveChildKey(
                            masterDeterministicKey, ChildNumber(44, true),
                            creationTimeSeconds)
                    val coinType = if (mbwManager.network.isTestnet) {
                        1 //Bitcoin Testnet https://github.com/satoshilabs/slips/blob/master/slip-0044.md
                    } else {
                        0 //Bitcoin Mainnet https://github.com/satoshilabs/slips/blob/master/slip-0044.md
                    }
                    val cointypeLevelDeterministicKey =
                            HDKeyDerivation.deriveChildKey(bip44LevelDeterministicKey,
                                    ChildNumber(coinType, true), creationTimeSeconds)
                    val networkParameters = NetworkParameters.fromID(if (mbwManager.network.isTestnet) {
                        NetworkParameters.ID_TESTNET
                    } else {
                        NetworkParameters.ID_MAINNET
                    })!!

                    val accountLevelKey = HDKeyDerivation.deriveChildKey(cointypeLevelDeterministicKey,
                            ChildNumber(accountIndex, true), creationTimeSeconds)
                    accountLevelKeys.add(accountLevelKey.serializePubB58(networkParameters))
                }

                //val bip39PassphraseList : ArrayList<String> = ArrayList(masterSeed.bip39WordList)
                val service = IntentContract.RequestAccountLevelKeysToSPV.createIntent(
                        ArrayList(accountIndexes.toList()),
                        ArrayList(accountLevelKeys.toList()),
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
                    return
                }
                val service = IntentContract.RequestSingleAddressPrivateKeyToSPV.createIntent(accountGuid, privateKey.privateKeyBytes)
                WalletApplication.sendToSpv(service, BCHSINGLEADDRESS)
            }
            "com.mycelium.wallet.notifyBroadcastTransactionBroadcastCompleted" -> {
                val operationId = intent.getStringExtra(IntentContract.OPERATION_ID)
                val txHash = intent.getStringExtra(IntentContract.TRANSACTION_HASH)
                val isSuccess = intent.getBooleanExtra(IntentContract.IS_SUCCESS, false)
                val message = intent.getStringExtra(IntentContract.MESSAGE)
                Handler(Looper.getMainLooper()).post {
                    eventBus.post(SpvSendFundsResult(operationId, txHash, isSuccess, message))
                }
            }
            "com.mycelium.wallet.sendUnsignedTransactionToMbw" -> {
                val accountIndex = intent.getIntExtra(IntentContract.ACCOUNT_INDEX_EXTRA, -1)
                val transactionBytes = intent.getByteArrayExtra(IntentContract.TRANSACTION_BYTES)
                val mbwManager = MbwManager.getInstance(context)
                val networkParameters = NetworkParameters.fromID(if (mbwManager.network.isTestnet) {
                    NetworkParameters.ID_TESTNET
                } else {
                    NetworkParameters.ID_MAINNET
                })!!
                val transaction = Transaction(networkParameters, transactionBytes)
                val account = mbwManager.getWalletManager(false).getBip44Account(accountIndex) as Bip44Account
                val privateKey = account.getPrivateKeyForAddress(Address.fromString(transaction.inputs.get(0).fromAddress.toString()),
                        AesKeyCipher.defaultKeyCipher())
                if (privateKey == null) {
                    Log.w(TAG, "MbwMessageReceiver.onMessageFromSpvModuleBch, " +
                            "com.mycelium.wallet.sendUnsignedTransactionToMbw, " +
                            "privateKey must not be null.")
                    return
                }
                val deterministicKey = DeterministicKey
                        .deserializeB58(privateKey.getBase58EncodedPrivateKey(mbwManager.network), networkParameters)
                // Sign the transaction
                val proposedTransaction = TransactionSigner.ProposedTransaction(transaction)
                val signer = LocalTransactionSigner()
                signer.signInputs(proposedTransaction, KeyChainGroup(networkParameters, deterministicKey, false))
                val service = IntentContract.SendSignedTransactionToSPV.createIntent(accountIndex,
                        proposedTransaction.partialTx.bitcoinSerialize())
                WalletApplication.sendToSpv(service, BCHBIP44)
            }
            "com.mycelium.wallet.sendUnsignedTransactionToMbwSingleAddress" -> {
                val accountGuid = intent.getStringExtra(IntentContract.SINGLE_ADDRESS_ACCOUNT_GUID)
                val transactionHash = intent.getStringExtra(IntentContract.TRANSACTION_HASH)
                val mbwManager = MbwManager.getInstance(context)
                val networkParameters = NetworkParameters.fromID(if (mbwManager.network.isTestnet) {
                    NetworkParameters.ID_TESTNET
                } else {
                    NetworkParameters.ID_MAINNET
                })!!


                val transaction = Transaction(networkParameters, Sha256Hash.wrap(transactionHash).bytes)
                val account = mbwManager.getWalletManager(false).getAccount(UUID.fromString(accountGuid)) as SingleAddressAccount

                val privateKeyBase58 = account.getPrivateKey(AesKeyCipher.defaultKeyCipher()).getBase58EncodedPrivateKey(mbwManager.network)
                val keyList = ArrayList<ECKey>()
                keyList.add(DumpedPrivateKey.fromBase58(networkParameters, privateKeyBase58).getKey())

                val group = KeyChainGroup(networkParameters)
                group.importKeys(keyList)

                val proposedTransaction = TransactionSigner.ProposedTransaction(transaction)
                val signer = LocalTransactionSigner()
                signer.signInputs(proposedTransaction, group)

                //TODO create intent to send signed transaction
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

    private fun notifySatoshisReceived() {
        val mds = MbwManager.getInstance(context).metadataStorage
        val builder = Notification.Builder(context)
                // TODO: bitcoin icon
                .setSmallIcon(R.drawable.holo_dark_ic_action_new_usd_account)
                .setContentTitle(context.getString(R.string.app_name))
        var contentText = "";
        for (account in AccountManager.getBCHBip44Accounts().values +
                AccountManager.getBCHSingleAddressAccounts().values) {
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

    private fun buildLine(action: Int, value: CurrencyValue, label:String) =
            context.getString(action, "${value.value.toPlainString()} ${value.currency} ($label)\n")

    companion object {
        private val TAG = MbwMessageReceiver::class.java.canonicalName
        @JvmStatic val TRANSACTION_NOTIFICATION_ID = -553794088
    }
}
