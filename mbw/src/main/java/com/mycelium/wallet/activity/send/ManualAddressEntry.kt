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
import android.widget.Toast
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import com.mycelium.wapi.wallet.Address
import com.mycelium.wapi.wallet.fio.FioModule
import com.mycelium.wapi.wallet.fio.FioName
import com.mycelium.wapi.wallet.fio.GetPubAddressResponse
import fiofoundation.io.fiosdk.isFioAddress
import kotlinx.android.synthetic.main.manual_entry.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import java.util.*

class ManualAddressEntry : Activity() {
    private var coinAddress: Address? = null
    private var fioAddress: String? = null
    private var entered: String? = null
    private lateinit var mbwManager: MbwManager
    private lateinit var fioModule: FioModule
    private lateinit var fioNames: Array<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.manual_entry)
        mbwManager = MbwManager.getInstance(this)
        fioModule = mbwManager.getWalletManager(false).getModuleById(FioModule.ID) as FioModule
        fioNames = fioModule.getKnownNames().map { "${it.name}@${it.domain}" }.toTypedArray()
        etRecipient.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(arg0: CharSequence, arg1: Int, arg2: Int, arg3: Int) = Unit
            override fun beforeTextChanged(arg0: CharSequence, arg1: Int, arg2: Int, arg3: Int) = Unit
            override fun afterTextChanged(editable: Editable) {
                updateUI()
            }
        })
        btOk.setOnClickListener { _ ->
            coinAddress?.run {
                finishOk(this)
            } ?: CoroutineScope(Dispatchers.Main).launch { tryFioFinish() }
        }
        etRecipient.inputType = InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE
        val account = mbwManager.selectedAccount
        tvTitle.text = getString(R.string.enter_address, account.coinType.name)
        lvKnownFioNames.adapter = ArrayAdapter<String>(this, R.layout.fio_address_item, fioNames)
        lvKnownFioNames.onItemClickListener = AdapterView.OnItemClickListener { parent, _, position, _ ->
            etRecipient.setText(parent.adapter.getItem(position) as String)
        }

        // Load saved state
        entered = savedInstanceState?.getString("entered") ?: ""
    }

    private fun updateUI() {
        entered = etRecipient.text.toString()

        val currencyType = mbwManager.selectedAccount.coinType
        coinAddress = currencyType.parseAddress(entered!!.trim { it <= ' ' })

        fioAddress = if (entered?.isFioAddress() == true) entered else null

        val recipientValid = coinAddress != null || fioAddress != null
        tvRecipientInvalid.visibility = if (!recipientValid) View.VISIBLE else View.GONE
        tvRecipientValid.visibility = if (recipientValid) View.VISIBLE else View.GONE
        btOk.isEnabled = recipientValid
        val filteredNames = fioNames.filter {
                    it.startsWith(entered.toString(), true)
                }.toTypedArray()
        if (filteredNames.isEmpty()) {
            llKnownFioNames.visibility = View.GONE
        } else {
            lvKnownFioNames.adapter = ArrayAdapter<String>(this@ManualAddressEntry,
                    R.layout.fio_address_item, filteredNames)
            llKnownFioNames.visibility = View.VISIBLE
        }
    }

    private fun finishOk(address: Address) {
        val result = Intent().apply {
            putExtra(ADDRESS_RESULT_NAME, address)
        }
        setResult(RESULT_OK, result)
        finish()
    }

    private suspend fun tryFioFinish() {
        withContext(Dispatchers.IO) {
            val coinType = mbwManager.selectedAccount.coinType
            val fioSymbol = coinType.symbol.toUpperCase(Locale.US)
            val mapper = ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            val client = OkHttpClient()
            try {
                val requestBody = """{"fio_address":"$fioAddress","chain_code":"$fioSymbol","token_code":"$fioSymbol"}"""
                val request = Request.Builder()
                        .url("${Utils.getFIOCoinType().url}chain/get_pub_address")
                        .post(RequestBody.create(MediaType.parse("application/json"), requestBody))
                        .build()
                val response = client.newCall(request).execute()
                val result = mapper.readValue(response.body()!!.string(), GetPubAddressResponse::class.java)
                finishOk(coinType.parseAddress(result.publicAddress ?: throw Exception("No public address found!"))!!)
                fioModule.addKnownName(FioName(fioAddress!!))
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    e.printStackTrace()
                    Toast.makeText(this@ManualAddressEntry, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
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
    }
}
