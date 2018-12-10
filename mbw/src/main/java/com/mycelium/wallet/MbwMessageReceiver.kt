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
import com.mrd.bitlib.crypto.BipDerivationType
import com.mrd.bitlib.model.Address
import com.mycelium.modularizationtools.CommunicationManager
import com.mycelium.modularizationtools.ModuleMessageReceiver
import com.mycelium.modularizationtools.model.Module
import com.mycelium.spvmodule.IntentContract
import com.mycelium.wallet.WalletApplication.getSpvModuleName
import com.mycelium.wallet.activity.modern.ModernMain
import com.mycelium.wallet.event.SpvSendFundsResult
import com.mycelium.wallet.event.SpvSyncChanged
import com.mycelium.wapi.wallet.AesKeyCipher
import com.mycelium.wapi.wallet.ExportableAccount
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.WalletAccount.Type.BCHBIP44
import com.mycelium.wapi.wallet.WalletAccount.Type.BCHSINGLEADDRESS
import com.mycelium.wapi.wallet.bip44.HDAccount
import com.mycelium.wapi.wallet.bip44.Bip44BCHAccount
import com.mycelium.wapi.wallet.currency.CurrencyValue
import com.mycelium.wapi.wallet.single.SingleAddressAccount
import com.subgraph.orchid.encoders.Hex
import org.bitcoinj.core.*
import org.bitcoinj.crypto.ChildNumber
import org.bitcoinj.crypto.DeterministicKey
import org.bitcoinj.crypto.HDKeyDerivation
import org.bitcoinj.signers.LocalTransactionSigner
import org.bitcoinj.signers.TransactionSigner
import org.bitcoinj.wallet.FreeStandingTransactionOutput
import org.bitcoinj.wallet.KeyChainGroup
import java.io.ByteArrayInputStream
import java.math.BigDecimal
import java.util.*
import kotlin.collections.ArrayList

class MbwMessageReceiver(private val context: Context) : ModuleMessageReceiver {
    override fun getIcon() = R.drawable.ic_launcher

    private val eventBus by lazy { MbwManager.getEventBus() }

    override fun onMessage(callingPackageName: String, intent: Intent) {
        when (callingPackageName) {
            getSpvModuleName(BCHBIP44) -> onMessageFromSpvModuleBch(intent, getModule(callingPackageName))
            BuildConfig.appIdGeb -> onMessageFromGebModule(intent)
            else -> Log.e(TAG, "Ignoring unexpected package $callingPackageName calling with intent $intent.")
        }
    }

    private fun getModule(packageName: String): Module? =
            CommunicationManager.getInstance().pairedModules.find { it.modulePackage == packageName }

    private fun onMessageFromGebModule(intent: Intent) {
        val mbwManager = MbwManager.getInstance(context)
        when (intent.action) {
            "com.mycelium.wallet.getMyceliumId" -> {
                val service = IntentContract.MyceliumIdTransfer.createIntent(mbwManager.myceliumId)
                WalletApplication.sendToGeb(service)
            }
            "com.mycelium.wallet.signData" -> {
                val message = intent.getStringExtra(IntentContract.MESSAGE)
                val signature = mbwManager.signMessage(message)
                val service = IntentContract.TransferSignedData.createIntent(message, signature)
                WalletApplication.sendToGeb(service)
            }
        }
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
            "com.mycelium.wallet.notifySatoshisReceivedUnrelated" -> {
                notifySatoshisReceived()
            }

            // MBW receives 'com.mycelium.wallet.getMyceliumId' message
            // when the module requested myceliumId
            "com.mycelium.wallet.getMyceliumId" -> {
                val mbwManager = MbwManager.getInstance(context)
                val service = IntentContract.MyceliumIdTransfer.createIntent(mbwManager.myceliumId)
                WalletApplication.sendToSpv(service, BCHBIP44)
            }

            // MBW receives 'com.mycelium.wallet.signData' message
            // when the module requested MBW to sign the message
            "com.mycelium.wallet.signData" -> {
                val mbwManager = MbwManager.getInstance(context)
                val message = intent.getStringExtra(IntentContract.MESSAGE)
                val signature = mbwManager.signMessage(message)
                val service = IntentContract.TransferSignedData.createIntent(message, signature)
                WalletApplication.sendToSpv(service, BCHBIP44)
            }

            "com.mycelium.wallet.blockchainState" -> {
                val bestChainDate = intent.getLongExtra("best_chain_date", 0L)
                val bestChainHeight = intent.getIntExtra("best_chain_height", 0)
                val chainDownloadPercentDone = intent.getFloatExtra("chain_download_percent_done", 0f)
                // val replaying = intent.getBooleanExtra("replaying", true)
                // val impediments = intent.getStringArrayExtra("impediment")
                walletManager.activeAccounts
                        .filterIsInstance<Bip44BCHAccount?>()
                        .forEach {
                            it!!.blockChainHeight = bestChainHeight
                        }
                // Defines a Handler object that's attached to the UI thread
                Handler(context.mainLooper).post {
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
                        0) //TODO Don't commit an evil value close to releasing the prodnet version. maybe do some BuildConfig.DEBUG ? 1504664986L: 0L
                WalletApplication.sendToSpv(service, BCHBIP44)
            }
            "com.mycelium.wallet.requestPublicKeyUnrelatedToMBW" -> {
                val _mbwManager = MbwManager.getInstance(context)
                val accountGuid = intent.getStringExtra(IntentContract.UNRELATED_ACCOUNT_GUID)
                val accountType = intent.getIntExtra(IntentContract.UNRELATED_ACCOUNT_TYPE, -1)

                when(accountType) {
                    IntentContract.UNRELATED_ACCOUNT_TYPE_HD -> {
                        val account: WalletAccount =_mbwManager.getWalletManager(false).getAccount(UUID.fromString(accountGuid))
                            //This is a way to not to pass information that this is a cold storage to BCH module and back
                            ?: _mbwManager.getWalletManager(true).getAccount(UUID.fromString(accountGuid))

                        //Unrelated HD key
                        if(account !is ExportableAccount) {
                            Log.e(TAG, "Can't handle account ${_mbwManager.metadataStorage.getLabelByAccount(account.id)}")
                            return
                        }
                        try {
                            val publicKeyB58 = account.getExportData(AesKeyCipher.defaultKeyCipher()).publicDataMap!!.get(BipDerivationType.BIP44)
                            val service = IntentContract.SendUnrelatedPublicKeyToSPV.createIntent(accountGuid,
                                    publicKeyB58, accountType)
                            WalletApplication.sendToSpv(service, BCHBIP44)
                        } catch (e: Exception) {
                            Log.e(TAG, "Still can't handle account ${_mbwManager.metadataStorage.getLabelByAccount(account.id)}")
                        }
                    }
                    IntentContract.UNRELATED_ACCOUNT_TYPE_SA -> {
                        Log.d(TAG, "com.mycelium.wallet.requestSingleAddressPrivateKeyToMBW, guid = $accountGuid")
                        val account =_mbwManager.getWalletManager(false).getAccount(UUID.fromString(accountGuid)) as? SingleAddressAccount
                                //This is a way to not to pass information that this is a cold storage to BCH module and back
                                ?: _mbwManager.getWalletManager(true).getAccount(UUID.fromString(accountGuid)) as SingleAddressAccount

                        val service = if (account.publicKey == null) {
                            IntentContract.SendUnrelatedWatchedAddressToSPV.createIntent(accountGuid,
                                    account.address.toString())
                        } else {
                            IntentContract.SendUnrelatedPublicKeyToSPV.createIntent(accountGuid,
                                    account.publicKey.toString(), accountType)
                        }
                        WalletApplication.sendToSpv(service, BCHSINGLEADDRESS)
                    }
                    else -> Log.e(TAG, "Unknown accountType $accountType")
                }
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
                val operationId = intent.getStringExtra(IntentContract.OPERATION_ID)
                val transactionBytes = intent.getByteArrayExtra(IntentContract.TRANSACTION_BYTES)
                val txUTXOsHexList = intent.getStringArrayExtra(IntentContract.CONNECTED_OUTPUTS)
                val accountIndex = intent.getIntExtra(IntentContract.ACCOUNT_INDEX_EXTRA, -1)
                val mbwManager = MbwManager.getInstance(context)
                val account = mbwManager.getWalletManager(false).getBip44BCHAccount(accountIndex) as Bip44BCHAccount
                val networkParameters = NetworkParameters.fromID(if (mbwManager.network.isTestnet) {
                    NetworkParameters.ID_TESTNET
                } else {
                    NetworkParameters.ID_MAINNET
                })!!
                val transaction = Transaction(networkParameters, transactionBytes)
                transaction.clearInputs()

                val keyList = ArrayList<ECKey>()

                for (utxoHex in txUTXOsHexList) {
                    val utxo = UTXO(ByteArrayInputStream(Hex.decode(utxoHex)))
                    val txOutput = FreeStandingTransactionOutput(networkParameters, utxo, utxo.height)
                    val address = txOutput.getAddressFromP2PKHScript(networkParameters)!!.toBase58()
                    val privateKey = account.getPrivateKeyForAddress(Address.fromString(address),
                            AesKeyCipher.defaultKeyCipher())
                    checkNotNull(privateKey)
                    keyList.add(DumpedPrivateKey.fromBase58(networkParameters,
                            privateKey!!.getBase58EncodedPrivateKey(mbwManager.network)).key)
                }

                val signedTransaction = signAndSerialize(networkParameters, keyList, txUTXOsHexList, transaction)

                val service = IntentContract.SendSignedTransactionToSPV.createIntent(operationId, accountIndex,
                        signedTransaction)
                WalletApplication.sendToSpv(service, BCHBIP44)
            }
            "com.mycelium.wallet.sendUnsignedTransactionToMbwUnrelated" -> {
                val operationId = intent.getStringExtra(IntentContract.OPERATION_ID)
                val accountGuid = intent.getStringExtra(IntentContract.UNRELATED_ACCOUNT_GUID)
                val transactionBytes = intent.getByteArrayExtra(IntentContract.TRANSACTION_BYTES)
                val txUTXOsHexList = intent.getStringArrayExtra(IntentContract.CONNECTED_OUTPUTS)
                val mbwManager = MbwManager.getInstance(context)
                val networkParameters = NetworkParameters.fromID(if (mbwManager.network.isTestnet) {
                    NetworkParameters.ID_TESTNET
                } else {
                    NetworkParameters.ID_MAINNET
                })!!
                val transaction = Transaction(networkParameters, transactionBytes)
                transaction.clearInputs()

                val account = mbwManager.getWalletManager(false).getAccount(UUID.fromString(accountGuid))

                val keyList = ArrayList<ECKey>()

                if (account is SingleAddressAccount) {
                    val privateKeyBase58 = account.getPrivateKey(AesKeyCipher.defaultKeyCipher()).getBase58EncodedPrivateKey(mbwManager.network)

                    keyList.add(DumpedPrivateKey.fromBase58(networkParameters, privateKeyBase58).key)
                } else {
                    val bip44Account = account as Bip44BCHAccount

                    for (utxoHex in txUTXOsHexList) {
                        val utxo = UTXO(ByteArrayInputStream(Hex.decode(utxoHex)))
                        val txOutput = FreeStandingTransactionOutput(networkParameters, utxo, utxo.height)
                        val address = txOutput.getAddressFromP2PKHScript(networkParameters)!!.toBase58()
                        val privateKey = bip44Account.getPrivateKeyForAddress(Address.fromString(address),
                                AesKeyCipher.defaultKeyCipher())!!
                        keyList.add(DumpedPrivateKey.fromBase58(networkParameters,
                                privateKey.getBase58EncodedPrivateKey(mbwManager.network)).key)
                    }
                }

                val signedTransaction = signAndSerialize(networkParameters, keyList, txUTXOsHexList, transaction)
                val service = IntentContract.SendSignedTransactionUnrelatedToSPV.createIntent(operationId, accountGuid,
                        signedTransaction)
                WalletApplication.sendToSpv(service, if (account is HDAccount) BCHBIP44 else BCHSINGLEADDRESS)
            }
            null -> Log.w(TAG, "onMessage failed. No action defined.")
            else -> Log.e(TAG, "onMessage failed. Unknown action ${intent.action}")
        }
    }

    private fun signAndSerialize(networkParameters: NetworkParameters, keyList: List<ECKey>, txUTXOsHexList: Array<String>, transaction: Transaction): ByteArray? {
        val group = KeyChainGroup(networkParameters)
        group.importKeys(keyList)

        for (utxoHex in txUTXOsHexList) {
            val utxo = UTXO(ByteArrayInputStream(Hex.decode(utxoHex)))
            val txOutput = FreeStandingTransactionOutput(networkParameters, utxo, utxo.height)
            val outpoint = TransactionOutPoint(networkParameters, txOutput)
            val txInput = TransactionInput(networkParameters, transaction, utxo.script.program, outpoint)
            transaction.addInput(txInput)

            val scriptPubKey = txInput.connectedOutput!!.scriptPubKey
            val redeemData = txInput.getConnectedRedeemData(group)
            txInput.scriptSig = scriptPubKey.createEmptyInputScript(redeemData!!.keys[0], redeemData.redeemScript)
        }

        transaction.shuffleOutputs()
        val proposedTransaction = TransactionSigner.ProposedTransaction(transaction, true)
        val signer = LocalTransactionSigner()
        check(signer.signInputs(proposedTransaction, group))
        return proposedTransaction.partialTx.bitcoinSerialize()
    }

    private fun notifySatoshisReceived() {
        val mds = MbwManager.getInstance(context).metadataStorage
        @SuppressWarnings("deprecation") // the non-deprecated alternative requires min API level 26
        val builder = Notification.Builder(context)
                // TODO: bitcoin icon
                .setSmallIcon(R.drawable.holo_dark_ic_action_new_usd_account)
                .setContentTitle(context.getString(R.string.app_name))
        var contentText = ""
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
