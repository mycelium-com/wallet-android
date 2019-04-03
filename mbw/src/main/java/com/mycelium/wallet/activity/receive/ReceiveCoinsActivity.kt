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
import android.view.Menu
import android.widget.PopupMenu
import com.mrd.bitlib.model.AddressType
import android.view.Window
import android.view.WindowManager
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.GetAmountActivity
import com.mycelium.wallet.activity.receive.ReceiveCoinsViewModel.Companion.GET_AMOUNT_RESULT_CODE
import com.mycelium.wallet.databinding.ReceiveCoinsActivityBinding
import com.mycelium.wallet.databinding.ReceiveCoinsActivityBtcBinding
import com.mycelium.wapi.wallet.btc.AbstractBtcAccount
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.bch.bip44.Bip44BCHAccount
import com.mycelium.wapi.wallet.btc.bip44.HDAccount
import com.mycelium.wapi.wallet.btc.single.SingleAddressAccount
import com.mycelium.wapi.wallet.bch.single.SingleAddressBCHAccount
import com.mycelium.wapi.wallet.coins.Value

import kotlinx.android.synthetic.main.receive_coins_activity_btc_addr_type.*
import kotlinx.android.synthetic.main.receive_coins_activity_qr.*

class ReceiveCoinsActivity : AppCompatActivity() {
    private lateinit var viewModel: ReceiveCoinsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        val mbwManager = MbwManager.getInstance(application)
        val account = mbwManager.selectedAccount
        val havePrivateKey = intent.getBooleanExtra(PRIVATE_KEY, false)
        val showIncomingUtxo = intent.getBooleanExtra(SHOW_UTXO, false)
        val viewModelProvider = ViewModelProviders.of(this)

        viewModel = when (account) {
            is SingleAddressBCHAccount, is Bip44BCHAccount -> viewModelProvider.get(ReceiveBchViewModel::class.java)
            is SingleAddressAccount, is HDAccount -> viewModelProvider.get(ReceiveBtcViewModel::class.java)
            else -> viewModelProvider.get(ReceiveGenericCoinsViewModel::class.java)
        }

        if (!viewModel.isInitialized()) {
            viewModel.init(account, havePrivateKey, showIncomingUtxo)
        }
        if (savedInstanceState != null) {
            viewModel.loadInstance(savedInstanceState)
        }
        activateNfc()

        initDatabinding(account)

        if (viewModel is ReceiveBtcViewModel &&
               (account as? AbstractBtcAccount)?.availableAddressTypes?.size ?: 0 > 1) {
            createAddressDropdown((account as? AbstractBtcAccount)?.availableAddressTypes!!)
        }
    }

    private fun createAddressDropdown(addressTypes: MutableList<AddressType>) {
        val btcViewModel = (viewModel as ReceiveBtcViewModel)

        val descriptionMap: Map<AddressType, Int> = addressTypes.map {
            it to when(it) {
                AddressType.P2PKH -> R.string.p2pkh
                AddressType.P2WPKH -> R.string.bech
                AddressType.P2SH_P2WPKH -> R.string.p2sh
            }
        }.toMap()

        val addressTypesMenu = PopupMenu(this, addressDropdownLayout)
        addressTypes.forEach {
            addressTypesMenu.menu.add(Menu.NONE, it.ordinal, it.ordinal, descriptionMap[it]!!)
        }

        addressDropdownLayout.setOnClickListener {
            addressTypesMenu.show()
        }

        // setting initial text based on current address type
        selectedAddressText.text = getString(descriptionMap[btcViewModel.getAccountDefaultAddressType()]!!)

        addressTypesMenu.setOnMenuItemClickListener { item ->
            btcViewModel.setAddressType(AddressType.values()[item.itemId])
            selectedAddressText.text = item.title
            false
        }
    }

    override fun onStart() {
        super.onStart()

        viewModel.getReceivingAddress().observe(this, Observer { ivQrCode.qrCode = viewModel.getPaymentUri() })
    }

    private fun initDatabinding(account: WalletAccount<*,*>) {
        //Data binding, should be called after everything else
        val receiveCoinsActivityNBinding =
                when (account) {
                    is SingleAddressBCHAccount, is Bip44BCHAccount -> getDefaultBinding()
                    is AbstractBtcAccount ->  {
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
            val amount = data?.getSerializableExtra(GetAmountActivity.AMOUNT) as Value
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
        fun callMe(currentActivity: Activity, account: WalletAccount<*,*>, havePrivateKey: Boolean,
                   showIncomingUtxo: Boolean = false, isColdStorage: Boolean = false) {
            currentActivity.startActivity(Intent(currentActivity, ReceiveCoinsActivity::class.java)
                    .putExtra(UUID, account.id)
                    .putExtra(PRIVATE_KEY, havePrivateKey)
                    .putExtra(SHOW_UTXO, showIncomingUtxo)
                    .putExtra(IS_COLD_STORAGE, isColdStorage))
        }
    }
}
