package com.mycelium.wallet.activity.receive

import android.app.Activity
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.databinding.DataBindingUtil
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Window
import android.view.WindowManager
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.GetAmountActivity
import com.mycelium.wallet.activity.receive.ReceiveCoinsViewModel.Companion.GET_AMOUNT_RESULT_CODE
import com.mycelium.wallet.coinapult.CoinapultAccount
import com.mycelium.wallet.colu.ColuAccount
import com.mycelium.wallet.databinding.ReceiveCoinsActivityNBinding
import com.mycelium.wallet.databinding.ReceiveCoinsActivityNBtcBinding
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.bip44.Bip44BCHAccount
import com.mycelium.wapi.wallet.bip44.HDAccount
import com.mycelium.wapi.wallet.currency.CurrencyValue
import com.mycelium.wapi.wallet.single.SingleAddressAccount
import com.mycelium.wapi.wallet.single.SingleAddressBCHAccount
import kotlinx.android.synthetic.main.receive_coins_activity_qr.*
import java.util.*

class ReceiveCoinsActivityN : AppCompatActivity() {
    private lateinit var viewModel: ReceiveCoinsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        val mbwManager = MbwManager.getInstance(application)
        val isColdStorage = intent.getBooleanExtra("isColdStorage", false)
        val walletManager = mbwManager.getWalletManager(isColdStorage)
        val account = walletManager.getAccount(intent.getSerializableExtra("accountUuid") as UUID)
        val havePrivateKey = intent.getBooleanExtra("havePrivateKey", false)
        val showIncomingUtxo = intent.getBooleanExtra("showIncomingUtxo", false)
        val viewModelProvider = ViewModelProviders.of(this)

        viewModel = when (account) {
            is SingleAddressAccount, is HDAccount, is CoinapultAccount -> viewModelProvider.get(ReceiveBtcViewModel::class.java)
            is SingleAddressBCHAccount, is Bip44BCHAccount -> viewModelProvider.get(ReceiveBchViewModel::class.java)
            is ColuAccount -> viewModelProvider.get(ReceiveCoCoViewModel::class.java)
            else -> throw NotImplementedError()
        }

        if (!viewModel.isInitialized()) {
            viewModel.init(account, havePrivateKey, showIncomingUtxo)
        }
        activateNfc()

        initDatabinding(account)

        ivQrCode.qrCode = viewModel.getPaymentUri()
    }

    private fun initDatabinding(account: WalletAccount) {
        //Data binding, should be called after everything else
        val receiveCoinsActivityNBinding =
                when (account) {
                    is SingleAddressAccount, is HDAccount ->  {
                        val contentView = DataBindingUtil.setContentView<ReceiveCoinsActivityNBtcBinding>(this, R.layout.receive_coins_activity_n_btc)
                        contentView.viewModel = viewModel as ReceiveBtcViewModel
                        contentView.activity = this
                        contentView
                    }
                    else -> {
                        val contentView = DataBindingUtil.setContentView<ReceiveCoinsActivityNBinding>(this, R.layout.receive_coins_activity_n)
                        contentView.viewModel = viewModel
                        contentView.activity = this
                        contentView
                    }
                }
        receiveCoinsActivityNBinding.setLifecycleOwner(this)
    }

    private fun activateNfc() {
        val nfc = viewModel.getNfc()
        if (nfc?.isNdefPushEnabled == true) {
            nfc.setNdefPushMessageCallback(NfcAdapter.CreateNdefMessageCallback {
                val uriRecord = NdefRecord.createUri(viewModel.getPaymentUri())
                NdefMessage(arrayOf(uriRecord))
            }, this)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == GET_AMOUNT_RESULT_CODE && resultCode == Activity.RESULT_OK) {
            // Get result from address chooser (may be null)
            val amount = data?.getSerializableExtra(GetAmountActivity.AMOUNT) as CurrencyValue
            viewModel.setAmount(amount)
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    companion object {
        @JvmStatic
        @JvmOverloads
        fun callMe(currentActivity: Activity, account: WalletAccount, havePrivateKey: Boolean,
                   showIncomingUtxo: Boolean = false, isColdStorage: Boolean = false) {
            val intent = Intent(currentActivity, ReceiveCoinsActivityN::class.java)
            intent.putExtra("accountUuid", account.id)
            intent.putExtra("havePrivateKey", havePrivateKey)
            intent.putExtra("showIncomingUtxo", showIncomingUtxo)
            intent.putExtra("isColdStorage", isColdStorage)
            currentActivity.startActivity(intent)
        }
    }
}