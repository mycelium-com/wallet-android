package com.mycelium.wallet.activity.receive

import android.app.Activity
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.databinding.DataBindingUtil
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import com.mrd.bitlib.model.AddressType
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.GetAmountActivity
import com.mycelium.wallet.activity.receive.ReceiveCoinsViewModel.Companion.GET_AMOUNT_RESULT_CODE
import com.mycelium.wallet.coinapult.CoinapultAccount
import com.mycelium.wallet.colu.ColuAccount
import com.mycelium.wallet.databinding.ReceiveCoinsActivityBinding
import com.mycelium.wallet.databinding.ReceiveCoinsActivityBtcBinding
import com.mycelium.wapi.wallet.AbstractAccount
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.bip44.Bip44BCHAccount
import com.mycelium.wapi.wallet.bip44.HDAccount
import com.mycelium.wapi.wallet.currency.CurrencyValue
import com.mycelium.wapi.wallet.single.SingleAddressAccount
import com.mycelium.wapi.wallet.single.SingleAddressBCHAccount
import kotlinx.android.synthetic.main.receive_coins_activity_qr.*
import java.util.*

class ReceiveCoinsActivity : AppCompatActivity() {
    private lateinit var viewModel: ReceiveCoinsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val mbwManager = MbwManager.getInstance(application)
        val isColdStorage = intent.getBooleanExtra(IS_COLD_STORAGE, false)
        val walletManager = mbwManager.getWalletManager(isColdStorage)
        val account = walletManager.getAccount(intent.getSerializableExtra(UUID) as UUID)
        val havePrivateKey = intent.getBooleanExtra(PRIVATE_KEY, false)
        val showIncomingUtxo = intent.getBooleanExtra(SHOW_UTXO, false)
        val viewModelProvider = ViewModelProviders.of(this)

        viewModel = when (account) {
            is SingleAddressBCHAccount, is Bip44BCHAccount -> viewModelProvider.get(ReceiveBchViewModel::class.java)
            is SingleAddressAccount, is HDAccount, is CoinapultAccount -> viewModelProvider.get(ReceiveBtcViewModel::class.java)
            is ColuAccount -> viewModelProvider.get(ReceiveCoCoViewModel::class.java)
            else -> throw NotImplementedError()
        }

        if (!viewModel.isInitialized()) {
            viewModel.init(account, havePrivateKey, showIncomingUtxo)
        }
        if (savedInstanceState != null) {
            viewModel.loadInstance(savedInstanceState)
        }
        activateNfc()

        initDatabinding(account)

        if (viewModel is ReceiveBtcViewModel)
            createAddressDropdown()
    }

    private fun createAddressDropdown() {
        val linearLayout: LinearLayout = findViewById(R.id.address_dropdown_layout)
        val tv: TextView = findViewById(R.id.tv_address_selected)
        val btcViewModel = (viewModel as ReceiveBtcViewModel)

        val p2pkh = resources.getString(R.string.receive_option_p2pkh)
        val p2sh = resources.getString(R.string.receive_option_p2sh)
        val bech = resources.getString(R.string.receive_option_bech)

        val addressTypesMenu = PopupMenu(this, linearLayout)
        addressTypesMenu.menu.add(p2pkh)
        addressTypesMenu.menu.add(p2sh)
        addressTypesMenu.menu.add(bech)

        linearLayout.setOnClickListener {
            addressTypesMenu.show()
        }

        // setting initial text based on current address type
        when {
            btcViewModel.getAccountDefaultAddressType() == AddressType.P2PKH -> tv.text = p2pkh
            btcViewModel.getAccountDefaultAddressType() == AddressType.P2SH_P2WPKH -> tv.text = p2sh
            btcViewModel.getAccountDefaultAddressType() == AddressType.P2WPKH -> tv.text = bech
        }

        addressTypesMenu.setOnMenuItemClickListener { item ->
            when {
                item.title == p2pkh -> btcViewModel.setAddressType(AddressType.P2PKH)
                item.title == p2sh -> btcViewModel.setAddressType(AddressType.P2SH_P2WPKH)
                item.title == bech -> btcViewModel.setAddressType(AddressType.P2WPKH)
            }

            tv.text = item.title
            false
        }

    }

    override fun onStart() {
        super.onStart()

        viewModel.getReceivingAddress().observe(this, Observer { ivQrCode.qrCode = viewModel.getPaymentUri() })
    }

    private fun initDatabinding(account: WalletAccount) {
        //Data binding, should be called after everything else
        val receiveCoinsActivityNBinding =
                when (account) {
                    is SingleAddressBCHAccount, is Bip44BCHAccount -> getDefaultBinding()
                    is AbstractAccount ->  {
                        // This is only actual if account contains multiple address types inside
                        if (account.availableAddressTypes.size > 1) {
                            val contentView = DataBindingUtil.setContentView<ReceiveCoinsActivityBtcBinding>(this, R.layout.receive_coins_activity_btc)
                            contentView.viewModel = viewModel as ReceiveBtcViewModel
                            contentView.activity = this
                            contentView
                        } else {
                            getDefaultBinding()
                        }
                    }
                    else -> getDefaultBinding()
                }
        receiveCoinsActivityNBinding.setLifecycleOwner(this)
    }

    private fun getDefaultBinding(): ReceiveCoinsActivityBinding {
        val contentView = DataBindingUtil.setContentView<ReceiveCoinsActivityBinding>(this, R.layout.receive_coins_activity)
        contentView.viewModel = viewModel
        contentView.activity = this
        return contentView
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

    override fun onSaveInstanceState(outState: Bundle) {
        viewModel.saveInstance(outState)
        super.onSaveInstanceState(outState)
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
        private const val UUID = "accountUuid"
        private const val PRIVATE_KEY = "havePrivateKey"
        private const val SHOW_UTXO = "showIncomingUtxo"
        private const val IS_COLD_STORAGE = "isColdStorage"

        @JvmStatic
        @JvmOverloads
        fun callMe(currentActivity: Activity, account: WalletAccount, havePrivateKey: Boolean,
                   showIncomingUtxo: Boolean = false, isColdStorage: Boolean = false) {
            currentActivity.startActivity(Intent(currentActivity, ReceiveCoinsActivity::class.java)
                    .putExtra(UUID, account.id)
                    .putExtra(PRIVATE_KEY, havePrivateKey)
                    .putExtra(SHOW_UTXO, showIncomingUtxo)
                    .putExtra(IS_COLD_STORAGE, isColdStorage))
        }
    }
}