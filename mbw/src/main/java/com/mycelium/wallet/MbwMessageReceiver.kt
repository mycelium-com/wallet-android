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
import org.bitcoinj.crypto.ChildNumber
import org.bitcoinj.crypto.DeterministicKey
import org.bitcoinj.crypto.HDKeyDerivation
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
                        0) //TODO Don't commit an evil value close to releasing the prodnet version. maybe do some BuildConfig.DEBUG ? 1504664986L: 0L
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
