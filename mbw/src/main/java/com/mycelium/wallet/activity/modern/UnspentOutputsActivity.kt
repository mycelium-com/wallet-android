package com.mycelium.wallet.activity.modern

import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.activity.modern.adapter.UnspentOutputAdapter
import com.mycelium.wallet.activity.util.setupFullscreen
import com.mycelium.wallet.databinding.UnspentOutputsActivityBinding
import com.mycelium.wapi.wallet.WalletAccount
import java.util.UUID

class UnspentOutputsActivity : AppCompatActivity() {

    private var _accountid: UUID? = null
    private val adapter = UnspentOutputAdapter()
    private lateinit var binding: UnspentOutputsActivityBinding

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(UnspentOutputsActivityBinding.inflate(layoutInflater).apply {
            binding = this
        }.getRoot())
        window.setupFullscreen()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        val mbwManager = MbwManager.getInstance(this.application)
        _accountid = intent.getSerializableExtra("account") as UUID?
        updateUi(mbwManager.getWalletManager(false).getAccount(_accountid!!))
    }

    private fun updateUi(account: WalletAccount<*>?) {
        val outputs = account?.getUnspentOutputViewModels()
        binding.tvNoOutputs.isVisible = outputs.orEmpty().isEmpty()
        adapter.submitList(outputs)
        binding.listUnspentOutputs.adapter = adapter
        if ((outputs?.size ?: 0) > 5) {
            binding.tvNoOutputs.append(" (" + outputs?.size.toString() + ")")
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
