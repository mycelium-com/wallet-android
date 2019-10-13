package com.mycelium.wallet.activity.send.model

import android.app.Activity
import android.app.AlertDialog
import android.app.Application
import android.app.ProgressDialog
import android.arch.lifecycle.MutableLiveData
import android.content.Intent
import android.databinding.BindingAdapter
import android.graphics.Point
import android.os.AsyncTask
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import android.widget.Toast.makeText
import com.mrd.bitlib.model.AddressType
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import com.mycelium.wallet.activity.StringHandlerActivity
import com.mycelium.wallet.activity.send.SendCoinsActivity
import com.mycelium.wallet.activity.send.adapter.AddressViewAdapter
import com.mycelium.wallet.activity.send.view.SelectableRecyclerView
import com.mycelium.wallet.activity.util.getAssetUri
import com.mycelium.wallet.activity.util.getPrivateKey
import com.mycelium.wallet.content.ResultType
import com.mycelium.wapi.api.response.Feature
import com.mycelium.wapi.content.btc.BitcoinUri
import com.mycelium.wapi.wallet.*
import com.mycelium.wapi.wallet.btc.BtcAddress
import com.mycelium.wapi.wallet.btc.bip44.HDAccountExternalSignature
import com.mycelium.wapi.wallet.coins.GenericAssetInfo
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.colu.ColuAccount
import com.mycelium.wapi.wallet.colu.ColuTransaction
import com.mycelium.wapi.wallet.colu.coins.MASSCoin
import com.mycelium.wapi.wallet.colu.coins.MTCoin
import com.mycelium.wapi.wallet.colu.coins.RMCCoin
import com.mycelium.wapi.wallet.exceptions.GenericTransactionBroadcastException
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.util.*

class SendColuViewModel(context: Application) : SendBtcViewModel(context) {
    override fun sendTransaction(activity: Activity) {
        mbwManager.versionManager.showFeatureWarningIfNeeded(activity,
                featureMap[model.account.coinType], true)
        {
            mbwManager.runPinProtectedFunction(activity) {
                // if we have a payment request, check if it is expired
                val sendBtcModel = model as SendBtcModel
                if (sendBtcModel.hasPaymentRequestHandler() && sendBtcModel.paymentRequestExpired()) {
                    makeText(activity, activity.getString(R.string.payment_request_not_sent_expired), LENGTH_LONG).show()
                    return@runPinProtectedFunction
                }

                sendColuTransaction(activity)
            }
        }
    }

    private fun sendColuTransaction(activity: Activity) {
        val dialog = ProgressDialog(activity)
        dialog.setCancelable(false)
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER)
        dialog.setMessage(activity.getString(R.string.sending_assets, model.account.coinType.symbol))
        progressDialog = dialog
        progressDialog?.show()

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
                    model.account.broadcastTx(model.transaction)
                }
                .doOnError { Log.e(TAG, "", it )}
                .subscribeOn(AndroidSchedulers.mainThread())
                .doOnComplete {
                    progressDialog?.dismiss()
                    mbwManager.getWalletManager(false).startSynchronization(model.account.id)
                    makeText(activity, R.string.transaction_sent, Toast.LENGTH_SHORT).show()
                    activity.finish()
                }
                .doOnError {
                    progressDialog?.dismiss()
                    makeText(activity, context.getString(R.string.asset_failed_to_broadcast,
                            model.account.coinType.symbol), Toast.LENGTH_SHORT).show()
                }
                .subscribe()
    }

    private val featureMap = hashMapOf(
            MTCoin to Feature.COLU_PREPARE_OUTGOING_TX,
            MASSCoin to Feature.COLU_PREPARE_OUTGOING_TX,
            RMCCoin to Feature.COLU_PREPARE_OUTGOING_TX
    )

    companion object {
        private val TAG = "SendColuViewModel"
    }
}