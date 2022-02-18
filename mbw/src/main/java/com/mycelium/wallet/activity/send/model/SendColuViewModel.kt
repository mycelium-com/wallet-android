package com.mycelium.wallet.activity.send.model

import android.app.Activity
import android.app.Application
import android.app.ProgressDialog
import android.util.Log
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import com.mycelium.wallet.activity.modern.Toaster
import com.mycelium.wapi.api.response.Feature
import com.mycelium.wapi.wallet.AesKeyCipher
import com.mycelium.wapi.wallet.SyncMode
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.btc.BtcAddress
import com.mycelium.wapi.wallet.colu.ColuTransaction
import com.mycelium.wapi.wallet.colu.coins.MASSCoin
import com.mycelium.wapi.wallet.colu.coins.MTCoin
import com.mycelium.wapi.wallet.colu.coins.RMCCoin
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

class SendColuViewModel(context: Application) : SendBtcViewModel(context) {
    override fun sendTransaction(activity: Activity) {
        mbwManager.versionManager.showFeatureWarningIfNeeded(activity,
                featureMap[model.account.coinType], true)
        {
            mbwManager.runPinProtectedFunction(activity) {
                // if we have a payment request, check if it is expired
                val sendBtcModel = model as SendBtcModel
                if (sendBtcModel.hasPaymentRequestHandler() && sendBtcModel.paymentRequestExpired()) {
                    Toaster(activity).toast(R.string.payment_request_not_sent_expired, false)
                    return@runPinProtectedFunction
                }

                sendColuTransaction(activity)
            }
        }
    }

    private fun sendColuTransaction(activity: Activity) {
        progressDialog = ProgressDialog(activity).apply {
            setCancelable(false)
            setProgressStyle(ProgressDialog.STYLE_SPINNER)
            setMessage(activity.getString(R.string.sending_assets, model.account.coinType.symbol))
            show()
        }
        Single.just(model.transaction)
                .subscribeOn(Schedulers.computation())
                .flatMapCompletable { transaction ->
                    (transaction as ColuTransaction).fundingAccounts.add(Utils.getLinkedAccount(model.account,
                            mbwManager.getWalletManager(false).getAccounts()) as WalletAccount<BtcAddress>)
                    model.account.signTx(transaction, AesKeyCipher.defaultKeyCipher())
                    return@flatMapCompletable Completable.complete()
                }
                .subscribeOn(Schedulers.io())
                .doOnComplete {
                    model.account.broadcastTx(model.transaction!!)
                    sendFioObtData()
                }
                .doOnError { Log.e(TAG, "", it )}
                .subscribeOn(AndroidSchedulers.mainThread())
                .doOnComplete {
                    progressDialog?.dismiss()
                    mbwManager.getWalletManager(false).startSynchronization(SyncMode.NORMAL,
                            (model.transaction as ColuTransaction).fundingAccounts + model.account)
                    Toaster(activity).toast(R.string.transaction_sent, true)
                    activity.finish()
                }
                .doOnError {
                    progressDialog?.dismiss()
                    Toaster(activity).toast(context.getString(R.string.asset_failed_to_broadcast,
                            model.account.coinType.symbol), true)
                }
                .subscribe()
    }

    private val featureMap = hashMapOf(
            MTCoin to Feature.COLU_PREPARE_OUTGOING_TX,
            MASSCoin to Feature.COLU_PREPARE_OUTGOING_TX,
            RMCCoin to Feature.COLU_PREPARE_OUTGOING_TX
    )

    companion object {
        private const val TAG = "SendColuViewModel"
    }
}