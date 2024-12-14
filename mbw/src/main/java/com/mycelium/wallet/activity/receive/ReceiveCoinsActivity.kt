package com.mycelium.wallet.activity.receive

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.tech.Ndef
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.PopupMenu
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import androidx.databinding.ViewDataBinding
import asStringRes
import com.mrd.bitlib.model.AddressType
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.receive.viewmodel.ReceiveCoinsFactory
import com.mycelium.wallet.databinding.*
import com.mycelium.wapi.wallet.AddressContainer
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.bch.bip44.Bip44BCHAccount
import com.mycelium.wapi.wallet.bch.single.SingleAddressBCHAccount
import com.mycelium.wapi.wallet.btc.AbstractBtcAccount
import com.mycelium.wapi.wallet.btcvault.hd.BitcoinVaultHdAccount
import java.io.IOException
import java.util.*

class ReceiveCoinsActivity : AppCompatActivity() {
    private val viewModel: ReceiveCoinsViewModel by viewModels { ReceiveCoinsFactory(account) }
    private var bindingReceiveFioName: ReceiveCoinsActivityFioNameBinding? = null
    private var bindingReceiveBtcAddrType: ReceiveCoinsActivityBtcAddrTypeBinding? = null
    private var bindingReceiveQr: ReceiveCoinsActivityQrBinding? = null
    lateinit var account: WalletAccount<*>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mbwManager = MbwManager.getInstance(application)
        val isColdStorage = intent.getBooleanExtra(IS_COLD_STORAGE, false)
        val walletManager = mbwManager.getWalletManager(isColdStorage)
        account = walletManager.getAccount(intent.getSerializableExtra(UUID) as UUID)!!
        val havePrivateKey = intent.getBooleanExtra(PRIVATE_KEY, false)
        val showIncomingUtxo = intent.getBooleanExtra(SHOW_UTXO, false)

        if (!viewModel.isInitialized()) {
            viewModel.init(account, havePrivateKey, showIncomingUtxo)
        }
        if (savedInstanceState != null) {
            viewModel.loadInstance(savedInstanceState)
        }
        activateNfc()

        initDatabinding(account)

        // TODO remove after full taproot implementation
        var addressTypes = (account as? AddressContainer)?.availableAddressTypes
//        if (addressTypes != null) {
//            addressTypes = addressTypes - AddressType.P2TR
//        }
        if (viewModel is ReceiveBtcViewModel && (addressTypes?.size ?: 0) > 1) {
            createAddressDropdown(addressTypes!!)
        }
        bindingReceiveFioName?.fioNameSpinner?.adapter = ArrayAdapter<String>(this,
                R.layout.layout_receive_fio_names, R.id.text, viewModel.getFioNameList().value!!).apply {
            setDropDownViewResource(R.layout.layout_receive_fio_names_dropdown)
        }
        bindingReceiveFioName?.fioNameSpinner?.onItemSelectedListener = object:AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                viewModel.onFioNameSelected(bindingReceiveFioName?.fioNameSpinner?.getItemAtPosition(position))
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
        viewModel.getReceivingAddress().observe(this) {
            bindingReceiveQr?.ivQrCode?.qrCode = viewModel.getPaymentUri()
        }
        bindingReceiveFioName?.btCreateFioRequest?.setOnClickListener {
            viewModel.createFioRequest(this@ReceiveCoinsActivity)
        }
        addMenuProvider(MenuImpl())
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        viewModel.processReceivedResults(requestCode, resultCode, data, this)
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun createAddressDropdown(addressTypes: List<AddressType>) {
        val btcViewModel = viewModel as ReceiveBtcViewModel
        // setting initial text based on current address type
        bindingReceiveBtcAddrType?.selectedAddressText?.text = getString(btcViewModel.getAccountDefaultAddressType()?.asStringRes() ?: 0)

        bindingReceiveBtcAddrType?.addressDropdownImageView?.visibility = if (addressTypes.size > 1) {
            val addressTypesMenu = PopupMenu(this, bindingReceiveBtcAddrType?.addressDropdownImageView)
            addressTypes.forEach {
                addressTypesMenu.menu.add(Menu.NONE, it.ordinal, it.ordinal, it.asStringRes())
            }

            bindingReceiveBtcAddrType?.addressDropdownLayout?.referencedIds?.forEach {
                bindingReceiveBtcAddrType?.root?.findViewById<View>(it)?.setOnClickListener {
                    addressTypesMenu.show()
                }
            }

            addressTypesMenu.setOnMenuItemClickListener { item ->
                btcViewModel.setAddressType(AddressType.values()[item.itemId])
                bindingReceiveBtcAddrType?.selectedAddressText?.text = item.title
                false
            }
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    private fun initDatabinding(account: WalletAccount<*>): ViewDataBinding {
        //Data binding, should be called after everything else
        val receiveCoinsActivityNBinding =
                when (account) {
                    is SingleAddressBCHAccount, is Bip44BCHAccount -> getDefaultBinding()
                    is AbstractBtcAccount -> {
                        // This is only actual if account contains multiple address types inside
                        if (account.availableAddressTypes.size > 1) {
                            ReceiveCoinsActivityBtcBinding.inflate(layoutInflater).apply {
                                bindingReceiveFioName = this.layoutReceiveFioName
                                bindingReceiveBtcAddrType = this.layoutReceiveBtcAddrType
                                bindingReceiveQr = this.layoutReceiveQr
                                viewModel = this@ReceiveCoinsActivity.viewModel as ReceiveBtcViewModel
                                activity = this@ReceiveCoinsActivity
                            }
                        } else {
                            getDefaultBinding()
                        }
                    }
                    is BitcoinVaultHdAccount -> {
                        // This is only actual if account contains multiple address types inside
                        if (account.availableAddressTypes.size > 1) {
                            ReceiveCoinsActivityBtcBinding.inflate(layoutInflater).apply {
                                bindingReceiveFioName = this.layoutReceiveFioName
                                bindingReceiveBtcAddrType = this.layoutReceiveBtcAddrType
                                bindingReceiveQr = this.layoutReceiveQr
                                viewModel = this@ReceiveCoinsActivity.viewModel as ReceiveBtcViewModel
                                activity = this@ReceiveCoinsActivity
                            }
                        } else {
                            getDefaultBinding()
                        }
                    }
                    else -> getDefaultBinding()
                }
        receiveCoinsActivityNBinding.lifecycleOwner = this
        setContentView(receiveCoinsActivityNBinding.root)
        return receiveCoinsActivityNBinding
    }

    private fun getDefaultBinding(): ReceiveCoinsActivityBinding =
            ReceiveCoinsActivityBinding.inflate(layoutInflater)
                    .also {
                        bindingReceiveFioName = it.layoutReceiveFioName
                        bindingReceiveQr = it.layoutReceiveQr
                        it.viewModel = viewModel
                        it.activity = this
                    }

    private fun activateNfc() {
        viewModel.checkNfcAvailable()
        val nfc = viewModel.getNfc()
        if (nfc?.isEnabled == true) {
            nfc.enableReaderMode(this, {
                Ndef.get(it)?.let { mNdef ->
                    val uriRecord = NdefRecord.createUri(viewModel.getPaymentUri())
                    val msg = NdefMessage(uriRecord)
                    try {
                        mNdef.connect()
                        mNdef.writeNdefMessage(msg)
                    } catch (e: Exception) {
                        viewModel.toaster.toast("Nfc connection error", false)
                    } finally {
                        try {
                            mNdef.close();
                        } catch (e: IOException) {
                        }
                    }
                }
            }, NfcAdapter.FLAG_READER_NFC_A or
                    NfcAdapter.FLAG_READER_NFC_B or
                    NfcAdapter.FLAG_READER_NFC_F or
                    NfcAdapter.FLAG_READER_NFC_V, null)
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

    internal inner class MenuImpl : MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean =
            when (menuItem.itemId) {
                android.R.id.home -> {
                    onBackPressed()
                    true
                }

                else -> false
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
