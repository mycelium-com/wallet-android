package com.mycelium.wallet.activity.send

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.MenuItem
import android.view.View
import android.view.View.*
import android.view.Window
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import com.mycelium.wapi.wallet.Address
import com.mycelium.wapi.wallet.coins.CryptoCurrency
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
import java.io.IOException
import java.util.*

class ManualAddressEntry : AppCompatActivity() {
    private var coinAddress: Address? = null
    private var fioAddress: String? = null
    private var entered: String? = null
    private lateinit var mbwManager: MbwManager
    private lateinit var fioModule: FioModule
    private lateinit var fioNames: Array<String>
    private lateinit var coinType: CryptoCurrency
    private val fioNameToNbpaMap = mutableMapOf<String, String>()
    private var fioQueryCounter = 0
    private val checkedFioNames = mutableSetOf<String>()
    private var noConnection = false
    private lateinit var statusViews: List<View>

    override fun onCreate(savedInstanceState: Bundle?) {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        val isForFio = intent.getBooleanExtra(FOR_FIO_REQUEST, false)
        supportActionBar?.run {
            title = getString(if(!isForFio)R.string.enter_recipient_title else R.string.fio_enter_fio_name_title)
            setHomeAsUpIndicator(R.drawable.ic_back_arrow)
            setHomeButtonEnabled(true)
            setDisplayHomeAsUpEnabled(true)
        }
        super.onCreate(savedInstanceState)
        setContentView(R.layout.manual_entry)
        statusViews = listOf(tvCheckingFioAddress, tvRecipientInvalid, tvRecipientValid,
                tvNoConnection, tvEnterRecipientDescription)
        mbwManager = MbwManager.getInstance(this)
        coinType = mbwManager.selectedAccount.coinType
        fioModule = mbwManager.getWalletManager(false).getModuleById(FioModule.ID) as FioModule
        fioNames = fioModule.getKnownNames().map { "${it.name}@${it.domain}" }.toTypedArray()
        etRecipient.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(arg0: CharSequence, arg1: Int, arg2: Int, arg3: Int) = Unit
            override fun beforeTextChanged(arg0: CharSequence, arg1: Int, arg2: Int, arg3: Int) = Unit
            override fun afterTextChanged(editable: Editable) {
                updateUI()
            }
        })
        btOk.setOnClickListener { finishOk(coinAddress!!) }
        etRecipient.inputType = InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE
        etRecipient.hint = if (!isForFio) getString(R.string.enter_recipient_hint, coinType.name) else getString(R.string.fio_name)
        tvEnterRecipientDescription.text = getString(R.string.enter_recipient_description, coinType.name)
        lvKnownFioNames.adapter = ArrayAdapter<String>(this, R.layout.fio_address_item, fioNames)
        lvKnownFioNames.onItemClickListener = AdapterView.OnItemClickListener { parent, _, position, _ ->
            etRecipient.setText(parent.adapter.getItem(position) as String)
        }

        // Load saved state
        entered = savedInstanceState?.getString("entered") ?: ""
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean =
            when (item?.itemId) {
                android.R.id.home -> {
                    onBackPressed()
                    true
                }
                else -> super.onOptionsItemSelected(item)
            }

    private fun updateUI() {
        val isFio: Boolean = etRecipient.text.toString().isFioAddress()
        coinAddress = coinType.parseAddress(fioNameToNbpaMap[etRecipient.text.toString()])
        if (entered != etRecipient.text.toString()) {
            entered = etRecipient.text.toString()
            fioAddress = if (isFio) {
                if (coinAddress == null && !checkedFioNames.contains(entered!!)) {
                    // query fio for a native blockchain public address
                    CoroutineScope(Dispatchers.Main).launch { tryFio(entered!!) }
                }
                entered
            } else {
                coinAddress = coinType.parseAddress(entered!!.trim { it <= ' ' })
                null
            }
        }
        val recipientValid = coinAddress != null
        for (tv in statusViews) { tv.visibility = GONE }
        when {
            entered?.isEmpty() ?: true -> tvEnterRecipientDescription
            recipientValid -> tvRecipientValid
            fioQueryCounter > 0 -> tvCheckingFioAddress
            noConnection && isFio -> tvNoConnection
            else -> tvRecipientInvalid
        }.visibility = VISIBLE
        btOk.isEnabled = recipientValid
        val filteredNames = fioNames.filter {
                    it.startsWith(entered.toString(), true)
                }.toTypedArray()
        if (filteredNames.isEmpty()) {
            llKnownFioNames.visibility = GONE
        } else {
            lvKnownFioNames.adapter = ArrayAdapter<String>(this@ManualAddressEntry,
                    R.layout.fio_address_item, filteredNames)
            llKnownFioNames.visibility = VISIBLE
        }
    }

    private fun finishOk(address: Address) {
        val result = Intent().apply {
            putExtra(ADDRESS_RESULT_NAME, address)
            putExtra(ADDRESS_RESULT_FIO, fioAddress)
        }
        setResult(RESULT_OK, result)
        finish()
    }

    /**
     * Query FIO for the given fio address
     */
    private suspend fun tryFio(address: String) {
        checkedFioNames.add(address)
        fioQueryCounter++
        updateUI()
        withContext(Dispatchers.IO) {
            val fioSymbol = coinType.symbol.toUpperCase(Locale.US)
            val mapper = ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            val client = OkHttpClient()
            try {
                val requestBody = """{"fio_address":"$address","chain_code":"$fioSymbol","token_code":"$fioSymbol"}"""
                val request = Request.Builder()
                        .url("${Utils.getFIOCoinType().url}chain/get_pub_address")
                        .post(RequestBody.create(MediaType.parse("application/json"), requestBody))
                        .build()
                val response = client.newCall(request).execute()
                noConnection = false
                // TODO: 10/5/20 At least when using a debugger, replies can end up out of order
                //       which might result in hard to debug bugs.
                if (response.isSuccessful) {
                    val reply = response.body()!!.string()
                    val result = mapper.readValue(reply, GetPubAddressResponse::class.java)
                    result.publicAddress?.let { npbaString ->
                        fioModule.addKnownName(FioName(address))
                        fioNameToNbpaMap[address] = npbaString
                    }
                }
            } catch (e: IOException) {
                // We have to check that name again once we have a connection
                checkedFioNames.remove(address)
                noConnection = true
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    e.printStackTrace()
                    Toast.makeText(this@ManualAddressEntry, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                fioQueryCounter--
                withContext(Dispatchers.Main) {
                    updateUI()
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
        const val ADDRESS_RESULT_FIO = "fioName"
        const val FOR_FIO_REQUEST = "IS_FOR_FIO_REQUEST"
    }
}
