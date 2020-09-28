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
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wapi.wallet.Address
import com.mycelium.wapi.wallet.fio.FioModule
import com.mycelium.wapi.wallet.fio.GetPubAddressResponse
import com.mycelium.wapi.wallet.fio.coins.FIOTest
import fiofoundation.io.fiosdk.isFioAddress
import kotlinx.android.synthetic.main.manual_entry.*
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import kotlin.concurrent.thread

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
        val fioModule = mbwManager.getWalletManager(false).getModuleById(FioModule.ID) as FioModule
        val fioNames = fioModule.getKnownNames().map { "${it.name}@${it.domain}" }.toTypedArray()
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
                lvKnownFioNames.adapter = ArrayAdapter<String>(
                        this@ManualAddressEntry,
                        R.layout.fio_address_item,
                        fioNames.filter {
                            it.startsWith(entered.toString(), true)
                        }.toTypedArray())
            }
        })
        btOk.setOnClickListener { _ ->
            coinAddress?.run {
                finishOk(this)
            } ?: tryFioFinish()
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

    private fun finishOk(address: Address) {
        val result = Intent().apply {
            putExtra(ADDRESS_RESULT_NAME, address)
        }
        setResult(RESULT_OK, result)
        finish()
    }

    private fun tryFioFinish() {
        thread {
            val coinType = mbwManager.selectedAccount.coinType
            val mapper = ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            val client = OkHttpClient()
            val requestBody = """{"fio_address":"$fioAddress","chain_code":"BTC","token_code":"BTC"}"""
            val request = Request.Builder()
                    .url("${FIOTest.url}chain/get_pub_address")
                    .post(RequestBody.create(MediaType.parse("application/json"), requestBody))
                    .build()
            try {
                val response = client.newCall(request).execute()
                val result = mapper.readValue(response.body()!!.string(), GetPubAddressResponse::class.java)
                finishOk(coinType.parseAddress(result.publicAddress!!)!!)
            } catch (e: Exception) {
                e.printStackTrace()
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
