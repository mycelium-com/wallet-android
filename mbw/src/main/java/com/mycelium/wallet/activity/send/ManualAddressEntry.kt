package com.mycelium.wallet.activity.send

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.MenuItem
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import com.mycelium.wallet.activity.send.adapter.FIONameAdapter
import com.mycelium.wallet.databinding.ManualEntryBinding
import com.mycelium.wapi.wallet.Address
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.erc20.coins.ERC20Token
import com.mycelium.wapi.wallet.fio.FioEndpoints
import com.mycelium.wapi.wallet.fio.FioModule
import com.mycelium.wapi.wallet.fio.FioName
import com.mycelium.wapi.wallet.fio.GetPubAddressResponse
import fiofoundation.io.fiosdk.isFioAddress
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import java.io.IOException
import java.util.Locale

class ManualAddressEntry : AppCompatActivity() {
    private var coinAddress: Address? = null
    private var fioAddress: String? = null
    private var entered: String? = null
    private lateinit var mbwManager: MbwManager
    private lateinit var fioModule: FioModule
    private lateinit var fioEndpoints: FioEndpoints
    private lateinit var fioNames: List<String>
    private lateinit var coinType: CryptoCurrency
    private val fioNameToNbpaMap = mutableMapOf<String, String>()
    private var fioQueryCounter = 0
    private val checkedFioNames = mutableSetOf<String>()
    private val fioNameRegistered = mutableMapOf<String, Boolean>()
    private var noConnection = false
    private lateinit var statusViews: List<View>
    private val mapper = ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    private val client = OkHttpClient()
    private val adapter = FIONameAdapter()
    private lateinit var binding: ManualEntryBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(ManualEntryBinding.inflate(layoutInflater).apply { binding = this }.root)
        val isForFio = intent.getBooleanExtra(FOR_FIO_REQUEST, false)
        supportActionBar?.run {
            title = getString(if (!isForFio) R.string.enter_recipient_title else R.string.fio_enter_fio_name_title)
            setHomeAsUpIndicator(R.drawable.ic_back_arrow)
            setHomeButtonEnabled(true)
            setDisplayHomeAsUpEnabled(true)
        }
        statusViews = listOf(binding.tvCheckingFioAddress, binding.tvRecipientInvalid, binding.tvRecipientValid,
            binding.tvNoConnection, binding.tvEnterRecipientDescription, binding.tvRecipientHasNoSuchAddress)
        mbwManager = MbwManager.getInstance(this)
        fioEndpoints = mbwManager.fioEndpoints
        coinType = if (!isForFio) mbwManager.selectedAccount.coinType else Utils.getFIOCoinType()
        fioModule = mbwManager.getWalletManager(false).getModuleById(FioModule.ID) as FioModule
        fioNames = fioModule.getKnownNames().map { "${it.name}@${it.domain}" }
        binding.etRecipient.doOnTextChanged { _, _, _, _ -> updateUI()}
        binding.btOk.setOnClickListener { finishOk(coinAddress!!) }
        binding.etRecipient.inputType = InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE
        binding.etRecipient.hint = if (!isForFio) getString(R.string.enter_recipient_hint, coinType.name) else getString(R.string.fio_name)
        binding.tvEnterRecipientDescription.text = if(!isForFio) getString(R.string.enter_recipient_description, coinType.name) else getString(R.string.enter_fio_name)
        binding.tvRecipientInvalid.text = if(!isForFio) getString(R.string.recipient_invalid) else getString(R.string.fio_recipient_invalid)
        binding.lvKnownFioNames.adapter = adapter
        binding.lvKnownFioNames.addItemDecoration(DividerItemDecoration(this, LinearLayoutManager.VERTICAL))
        adapter.clickListener = { fioName ->
            binding.etRecipient.setText(fioName)
        }
        adapter.submitList(listOf(FIONameAdapter.HEADER_ITEM) + fioNames)

        // Load saved state
        entered = savedInstanceState?.getString("entered") ?: ""
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
            when (item.itemId) {
                android.R.id.home -> {
                    onBackPressed()
                    true
                }
                else -> super.onOptionsItemSelected(item)
            }

    private fun updateUI() {
        binding.tvRecipientHasNoSuchAddress.text = getString(R.string.recipient_has_no_such_address, binding.etRecipient.text, coinType.symbol)
        val isFio: Boolean = binding.etRecipient.text.toString().isFioAddress()
        coinAddress = coinType.parseAddress(fioNameToNbpaMap[binding.etRecipient.text.toString()])
        if (entered != binding.etRecipient.text.toString()) {
            entered = binding.etRecipient.text.toString()
            fioAddress = if (isFio) {
                tryFio(entered!!)
                entered
            } else {
                coinAddress = coinType.parseAddress(entered!!.trim { it <= ' ' })
                null
            }
        }
        val recipientValid = coinAddress != null
        for (tv in statusViews) {
            tv.visibility = GONE
        }
        when {
            entered?.isEmpty() ?: true -> binding.tvEnterRecipientDescription
            recipientValid -> binding.tvRecipientValid
            fioQueryCounter > 0 -> binding.tvCheckingFioAddress
            noConnection && isFio -> binding.tvNoConnection
            fioNameRegistered[entered!!] == true -> binding.tvRecipientHasNoSuchAddress
            else -> binding.tvRecipientInvalid
        }.visibility = VISIBLE
        binding.btOk.isEnabled = recipientValid
        val filteredNames = fioNames.filter {
            it.startsWith(entered.toString(), true)
        }
        if (filteredNames.isEmpty()) {
            binding.lvKnownFioNames.visibility = GONE
        } else {
            adapter.submitList(listOf(FIONameAdapter.HEADER_ITEM) + filteredNames)
            binding.lvKnownFioNames.visibility = VISIBLE
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
    private fun tryFio(address: String) {
        if (fioNameToNbpaMap.contains(address) || checkedFioNames.contains(address)) {
            // we have the result cached already
            return
        }
        fioQueryCounter += 2 // we query the server twice
        checkedFioNames.add(address)
        updateUI()
        // TODO: 10/6/20 Use: val result = FioTransactionHistoryService.getPubkeyByFioAddress()
        //       It probably needs changes to preserve the error handling.
        val tokenCode = coinType.symbol.toUpperCase(Locale.US)
        val chainCode = if (coinType is ERC20Token) "ETH" else tokenCode
        queryAddress(address, chainCode, tokenCode)
        queryAddressAvailability(address)
    }

    private fun queryAddress(address: String, chainCode: String, tokenCode: String) {
        val requestBody = """{"fio_address":"$address","chain_code":"$chainCode","token_code":"$tokenCode"}"""

        val request = Request.Builder()
                .url("${fioEndpoints.getCurrentApiEndpoint().baseUrl}chain/get_pub_address")
                .post(RequestBody.create("application/json".toMediaType(), requestBody))
                .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                noConnection = false
                val reply = response.body!!.string()
                val result = mapper.readValue(reply, GetPubAddressResponse::class.java)
                result.publicAddress?.let { npbaString ->
                    fioModule.addKnownName(FioName(address))
                    fioNameToNbpaMap[address] = npbaString
                }
                fioQueryCounter--
                runOnUiThread { updateUI() }
            }

            override fun onFailure(call: Call, e: IOException) {
                checkedFioNames.remove(address)
                noConnection = true
                fioQueryCounter--
                runOnUiThread { updateUI() }
            }
        })
    }

    private fun queryAddressAvailability(address: String) {
        val requestBody = """{"fio_name":"$address"}"""
        val request = Request.Builder()
                .url("${fioEndpoints.getCurrentApiEndpoint().baseUrl}chain/avail_check")
                .post(RequestBody.create("application/json".toMediaType(), requestBody))
                .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                fioQueryCounter--
                noConnection = false
                val reply = response.body!!.string()
                fioNameRegistered[address] = reply.contains("1") //.contains(""""is_registered":1""")
                runOnUiThread { updateUI() }
            }

            override fun onFailure(call: Call, e: IOException) {
                fioQueryCounter--
                noConnection = true
                runOnUiThread { updateUI() }
            }
        })
    }

    override fun onResume() {
        binding.etRecipient.setText(entered)
        super.onResume()
    }

    public override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)
        savedInstanceState.putSerializable("entered", binding.etRecipient.text.toString())
    }

    companion object {
        const val ADDRESS_RESULT_NAME = "address"
        const val ADDRESS_RESULT_FIO = "fioName"
        const val FOR_FIO_REQUEST = "IS_FOR_FIO_REQUEST"
    }
}
