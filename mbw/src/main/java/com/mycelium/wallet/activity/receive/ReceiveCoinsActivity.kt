package com.mycelium.wallet.activity.receive

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.PopupMenu
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import asStringRes
import com.mrd.bitlib.model.AddressType
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.databinding.ReceiveCoinsActivityBinding
import com.mycelium.wallet.databinding.ReceiveCoinsActivityBtcBinding
import com.mycelium.wapi.wallet.AddressContainer
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.bch.bip44.Bip44BCHAccount
import com.mycelium.wapi.wallet.bch.single.SingleAddressBCHAccount
import com.mycelium.wapi.wallet.btc.AbstractBtcAccount
import com.mycelium.wapi.wallet.btc.bip44.HDAccount
import com.mycelium.wapi.wallet.btc.single.SingleAddressAccount
import com.mycelium.wapi.wallet.btcvault.hd.BitcoinVaultHdAccount
import com.mycelium.wapi.wallet.erc20.ERC20Account
import com.mycelium.wapi.wallet.eth.EthAccount
import com.mycelium.wapi.wallet.fio.FioAccount
import kotlinx.android.synthetic.main.receive_coins_activity_btc_addr_type.*
import kotlinx.android.synthetic.main.receive_coins_activity_fio_name.*
import kotlinx.android.synthetic.main.receive_coins_activity_qr.*
import java.util.*

class ReceiveCoinsActivity : AppCompatActivity() {
    private lateinit var viewModel: ReceiveCoinsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mbwManager = MbwManager.getInstance(application)
        val isColdStorage = intent.getBooleanExtra(IS_COLD_STORAGE, false)
        val walletManager = mbwManager.getWalletManager(isColdStorage)
        val account = walletManager.getAccount(intent.getSerializableExtra(UUID) as UUID)!!
        val havePrivateKey = intent.getBooleanExtra(PRIVATE_KEY, false)
        val showIncomingUtxo = intent.getBooleanExtra(SHOW_UTXO, false)
        val viewModelProvider = ViewModelProviders.of(this)

        viewModel = when (account) {
            is SingleAddressBCHAccount, is Bip44BCHAccount -> viewModelProvider.get(ReceiveBchViewModel::class.java)
            is SingleAddressAccount, is HDAccount -> viewModelProvider.get(ReceiveBtcViewModel::class.java)
            is BitcoinVaultHdAccount -> viewModelProvider.get(ReceiveBtcViewModel::class.java)
            is EthAccount -> viewModelProvider.get(ReceiveEthViewModel::class.java)
            is ERC20Account -> viewModelProvider.get(ReceiveERC20ViewModel::class.java)
            is FioAccount -> viewModelProvider.get(ReceiveFIOViewModel::class.java)
            else -> viewModelProvider.get(ReceiveGenericCoinsViewModel::class.java)
        }

        if (!viewModel.isInitialized()) {
            viewModel.init(account, havePrivateKey, showIncomingUtxo)
        }
        if (savedInstanceState != null) {
            viewModel.loadInstance(savedInstanceState)
        }
        activateNfc()

        val binding = initDatabinding(account)
        initWithBindings(binding)

        // TODO remove after full taproot implementation
        var addressTypes = (account as? AddressContainer)?.availableAddressTypes
        if (addressTypes != null) {
            addressTypes = addressTypes - AddressType.P2TR
        }
        if (viewModel is ReceiveBtcViewModel && (addressTypes?.size ?: 0) > 1) {
            createAddressDropdown(addressTypes!!)
        }
        fioNameSpinner.adapter = ArrayAdapter<String>(this,
                R.layout.layout_receive_fio_names, R.id.text, viewModel.getFioNameList().value!!).apply {
            setDropDownViewResource(R.layout.layout_receive_fio_names_dropdown)
        }
        fioNameSpinner.onItemSelectedListener = object:AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                viewModel.onFioNameSelected(fioNameSpinner.getItemAtPosition(position))
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                viewModel.onFioNameSelected(null)
            }
        }
        supportActionBar?.run {
            title = getString(R.string.receive_cointype, viewModel.getCurrencySymbol())
            setHomeAsUpIndicator(R.drawable.ic_back_arrow)
            setHomeButtonEnabled(true)
            setDisplayHomeAsUpEnabled(true)
        }
        registerReceiver(nfcReceiver, IntentFilter(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED))
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        viewModel.processReceivedResults(requestCode, resultCode, data, this)
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
            when (item.itemId) {
                android.R.id.home -> {
                    onBackPressed()
                    true
                }
                else -> super.onOptionsItemSelected(item)
            }

    private fun initWithBindings(binding: ViewDataBinding?) {
        btCreateFioRequest.setOnClickListener {
            viewModel.createFioRequest(this@ReceiveCoinsActivity)
        }
    }

    private fun createAddressDropdown(addressTypes: List<AddressType>) {
        val btcViewModel = viewModel as ReceiveBtcViewModel
        // setting initial text based on current address type
        selectedAddressText.text = getString(btcViewModel.getAccountDefaultAddressType()?.asStringRes() ?: 0)

        address_dropdown_image_view.visibility = if (addressTypes.size > 1) {
            val addressTypesMenu = PopupMenu(this, address_dropdown_image_view)
            addressTypes.forEach {
                addressTypesMenu.menu.add(Menu.NONE, it.ordinal, it.ordinal, it.asStringRes())
            }

            addressDropdownLayout.referencedIds.forEach {
                findViewById<View>(it).setOnClickListener {
                    addressTypesMenu.show()
                }
            }

            addressTypesMenu.setOnMenuItemClickListener { item ->
                btcViewModel.setAddressType(AddressType.values()[item.itemId])
                selectedAddressText.text = item.title
                false
            }
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    override fun onStart() {
        super.onStart()

        viewModel.getReceivingAddress().observe(this, Observer {
            ivQrCode.qrCode = viewModel.getPaymentUri()
        })
    }

    private fun initDatabinding(account: WalletAccount<*>): ViewDataBinding? {
        //Data binding, should be called after everything else
        val receiveCoinsActivityNBinding =
                when (account) {
                    is SingleAddressBCHAccount, is Bip44BCHAccount -> getDefaultBinding()
                    is AbstractBtcAccount -> {
                        // This is only actual if account contains multiple address types inside
                        if (account.availableAddressTypes.size > 1) {
                            val contentView =
                                    DataBindingUtil.setContentView<ReceiveCoinsActivityBtcBinding>(
                                            this, R.layout.receive_coins_activity_btc)
                            contentView.viewModel = viewModel as ReceiveBtcViewModel
                            contentView.activity = this
                            contentView
                        } else {
                            getDefaultBinding()
                        }
                    }
                    is BitcoinVaultHdAccount -> {
                        // This is only actual if account contains multiple address types inside
                        if (account.availableAddressTypes.size > 1) {
                            val contentView =
                                    DataBindingUtil.setContentView<ReceiveCoinsActivityBtcBinding>(
                                            this, R.layout.receive_coins_activity_btc)
                            contentView.viewModel = viewModel as ReceiveBtcViewModel
                            contentView.activity = this
                            contentView
                        } else {
                            getDefaultBinding()
                        }
                    }
                    else -> getDefaultBinding()
                }
        receiveCoinsActivityNBinding.lifecycleOwner = this

        return receiveCoinsActivityNBinding
    }

    private fun getDefaultBinding(): ReceiveCoinsActivityBinding =
            DataBindingUtil
                    .setContentView<ReceiveCoinsActivityBinding>(this, R.layout.receive_coins_activity)
                    .also {
                        it.viewModel = viewModel
                        it.activity = this
                    }

    private fun activateNfc() {
        viewModel.checkNfcAvailable()
        val nfc = viewModel.getNfc()
        if (nfc?.isEnabled == true) {
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

    override fun onDestroy() {
        unregisterReceiver(nfcReceiver)
        super.onDestroy()
    }

    val nfcReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            activateNfc()
        }
    }

    companion object {
        private const val UUID = "accountUuid"
        private const val PRIVATE_KEY = "havePrivateKey"
        private const val SHOW_UTXO = "showIncomingUtxo"
        private const val IS_COLD_STORAGE = "isColdStorage"
        const val MANUAL_ENTRY_RESULT_CODE = 4
        const val REQUEST_CODE_FIO_NAME_MAPPING = 5

        @JvmStatic
        @JvmOverloads
        fun callMe(currentActivity: Activity, account: WalletAccount<*>, havePrivateKey: Boolean,
                   showIncomingUtxo: Boolean = false, isColdStorage: Boolean = false) {
            currentActivity.startActivity(Intent(currentActivity, ReceiveCoinsActivity::class.java)
                    .putExtra(UUID, account.id)
                    .putExtra(PRIVATE_KEY, havePrivateKey)
                    .putExtra(SHOW_UTXO, showIncomingUtxo)
                    .putExtra(IS_COLD_STORAGE, isColdStorage))
        }
    }
}
