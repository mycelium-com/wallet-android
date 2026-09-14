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
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.databinding.ManualEntryBinding
import com.mycelium.wapi.wallet.Address
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.erc20.coins.ERC20Token
import com.mycelium.wapi.wallet.trc20.coins.TRC20Token

class ManualAddressEntry : AppCompatActivity() {
    private var coinAddress: Address? = null
    private var entered: String? = null
    private lateinit var mbwManager: MbwManager
    private lateinit var coinType: CryptoCurrency
    private lateinit var statusViews: List<View>
    private lateinit var binding: ManualEntryBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(ManualEntryBinding.inflate(layoutInflater).apply { binding = this }.root)
        supportActionBar?.run {
            title = getString(R.string.enter_recipient_title)
            setHomeAsUpIndicator(R.drawable.ic_back_arrow)
            setHomeButtonEnabled(true)
            setDisplayHomeAsUpEnabled(true)
        }
        statusViews = listOf(binding.tvRecipientInvalid, binding.tvRecipientValid,
            binding.tvEnterRecipientDescription)
        mbwManager = MbwManager.getInstance(this)
        coinType = mbwManager.selectedAccount.coinType
        binding.etRecipient.doOnTextChanged { _, _, _, _ -> updateUI() }
        binding.btOk.setOnClickListener { finishOk(coinAddress!!) }
        binding.etRecipient.inputType = InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE
        binding.etRecipient.hint = getString(R.string.enter_recipient_hint, coinType.name)
        binding.tvEnterRecipientDescription.text = getString(R.string.enter_recipient_description, coinType.name)
        binding.tvRecipientInvalid.text = getString(R.string.recipient_invalid)

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
        if (entered != binding.etRecipient.text.toString()) {
            entered = binding.etRecipient.text.toString()
            coinAddress = coinType.parseAddress(entered!!.trim { it <= ' ' })
        }
        val recipientValid = coinAddress != null
        for (tv in statusViews) {
            tv.visibility = GONE
        }
        when {
            entered?.isEmpty() ?: true -> binding.tvEnterRecipientDescription
            recipientValid -> binding.tvRecipientValid
            else -> binding.tvRecipientInvalid
        }.visibility = VISIBLE
        binding.btOk.isEnabled = recipientValid
    }

    private fun finishOk(address: Address) {
        val result = Intent().apply {
            putExtra(ADDRESS_RESULT_NAME, address)
        }
        setResult(RESULT_OK, result)
        finish()
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
    }
}
