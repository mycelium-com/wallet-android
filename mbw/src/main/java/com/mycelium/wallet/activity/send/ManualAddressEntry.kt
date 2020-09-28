package com.mycelium.wallet.activity.send

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.View
import android.view.Window
import android.widget.AdapterView
import android.widget.ArrayAdapter
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wapi.wallet.Address
import com.mycelium.wapi.wallet.fio.FioModule
import fiofoundation.io.fiosdk.isFioAddress
import kotlinx.android.synthetic.main.manual_entry.*

class ManualAddressEntry : Activity() {
    private var coinAddress: Address? = null
    private var fioAddress: String? = null
    private var entered: String? = null
    private lateinit var mbwManager: MbwManager

    override fun onCreate(savedInstanceState: Bundle?) {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.manual_entry)
        mbwManager = MbwManager.getInstance(this)
        etRecipient.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(arg0: CharSequence, arg1: Int, arg2: Int, arg3: Int) = Unit
            override fun beforeTextChanged(arg0: CharSequence, arg1: Int, arg2: Int, arg3: Int) = Unit
            override fun afterTextChanged(editable: Editable) {
                entered = editable.toString()

                val currencyType = mbwManager.selectedAccount.coinType
                coinAddress = currencyType.parseAddress(entered!!.trim { it <= ' ' })

                fioAddress = if (entered?.isFioAddress() == true) entered else null

                val recipientValid = coinAddress != null || fioAddress != null
                tvRecipientInvalid.visibility = if (!recipientValid) View.VISIBLE else View.GONE
                tvRecipientValid.visibility = if (recipientValid) View.VISIBLE else View.GONE
                btOk.isEnabled = recipientValid
                val fioModule = mbwManager.getWalletManager(false).getModuleById(FioModule.ID) as FioModule
                val fioNames = fioModule.getKnownNames().map { "${it.name}@${it.domain}" }.toTypedArray()
                lvKnownFioNames.adapter = ArrayAdapter<String>(this@ManualAddressEntry, R.layout.fio_address_item, fioNames)
                lvKnownFioNames.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
                    etRecipient.setText(parent.adapter.getItem(position) as String)
                }
            }
        })
        btOk.setOnClickListener { _ ->
            val result = Intent().apply {
                putExtra(ADDRESS_RESULT_NAME, coinAddress)
                putExtra(FIO_ADDRESS_RESULT_NAME, fioAddress)
            }
            this@ManualAddressEntry.setResult(RESULT_OK, result)
            finish()
        }
        etRecipient.inputType = InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE
        val account = mbwManager!!.selectedAccount
        tvTitle.text = getString(R.string.enter_address, account.coinType.name)
        val fioModule = mbwManager!!.getWalletManager(false).getModuleById(FioModule.ID) as FioModule
        lvKnownFioNames.adapter = ArrayAdapter<String>(this, R.layout.fio_address_item, fioModule.getAllFIONames())

        // Load saved state
        entered = savedInstanceState?.getString("entered") ?: ""
    }

    override fun onResume() {
        etRecipient.setText(entered)
        super.onResume()
    }

    public override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)
        savedInstanceState.putSerializable("entered", etRecipient.text.toString())
    }

    companion object {
        const val ADDRESS_RESULT_NAME = "address"
        const val FIO_ADDRESS_RESULT_NAME = "fioAddress"
    }
}
