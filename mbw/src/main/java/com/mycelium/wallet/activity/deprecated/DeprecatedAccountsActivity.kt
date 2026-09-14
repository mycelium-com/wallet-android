package com.mycelium.wallet.activity.deprecated

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import com.mycelium.wallet.activity.modern.Toaster
import com.mycelium.wallet.activity.util.QrImageView
import com.mycelium.wallet.databinding.ActivityDeprecatedAccountsBinding

/**
 * One-time warning for users who had Bitcoin Vault or FIO accounts holding funds when support for
 * those coins was dropped. It lets them export the keys before the accounts are gone for good.
 */
class DeprecatedAccountsActivity : AppCompatActivity() {
    private lateinit var mbwManager: MbwManager
    private lateinit var binding: ActivityDeprecatedAccountsBinding
    private val adapter = DeprecatedAccountAdapter { showKeys(it) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeprecatedAccountsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        mbwManager = MbwManager.getInstance(this)
        supportActionBar?.run {
            setTitle(R.string.deprecated_accounts_title)
            setHomeAsUpIndicator(R.drawable.ic_back_arrow)
            setHomeButtonEnabled(true)
            setDisplayHomeAsUpEnabled(true)
        }
        binding.accounts.layoutManager = LinearLayoutManager(this)
        binding.accounts.adapter = adapter

        val accounts = DeprecatedAccounts.pending(mbwManager.walletDB)
        if (accounts.isEmpty()) {
            finish()
            return
        }
        adapter.submit(accounts)

        binding.btAcknowledge.setOnClickListener {
            DeprecatedAccounts.acknowledge(mbwManager.walletDB)
            finish()
        }
    }

    private fun showKeys(account: DeprecatedAccount) {
        mbwManager.runPinProtectedFunction(this) {
            val keys = try {
                DeprecatedAccounts.keysOf(account, mbwManager.masterSeedManager, mbwManager.network)
            } catch (e: Exception) {
                emptyList()
            }
            if (keys.isEmpty()) {
                Toaster(this).toast(R.string.deprecated_accounts_no_key, false)
                return@runPinProtectedFunction
            }
            showKeysDialog(account, keys)
        }
    }

    private fun showKeysDialog(account: DeprecatedAccount, keys: List<DeprecatedAccountKey>) {
        val container = LayoutInflater.from(this)
            .inflate(R.layout.dialog_deprecated_account_keys, null) as ViewGroup
        val keyList = container.findViewById<ViewGroup>(R.id.keyList)
        keys.forEach { key ->
            val view = LayoutInflater.from(this)
                .inflate(R.layout.item_deprecated_account_key, keyList, false)
            view.findViewById<TextView>(R.id.tvPath).text = key.derivationPath
            view.findViewById<TextView>(R.id.tvKey).text = key.key
            view.findViewById<QrImageView>(R.id.ivQr).setQrCode(key.key)
            view.findViewById<View>(R.id.btCopy).setOnClickListener {
                Utils.setClipboardString(key.key, this)
                Toaster(this).toast(R.string.copied_to_clipboard, true)
            }
            keyList.addView(view)
        }
        AlertDialog.Builder(this)
            .setTitle(account.label)
            .setView(container)
            .setPositiveButton(R.string.button_done, null)
            .show()
    }

    companion object {
        /** Shows the warning if there is anything to warn about. Returns true if it was shown. */
        @JvmStatic
        fun startIfNeeded(activity: Activity): Boolean {
            val walletDB = MbwManager.getInstance(activity).walletDB
            if (DeprecatedAccounts.pending(walletDB).isEmpty()) {
                return false
            }
            activity.startActivity(Intent(activity, DeprecatedAccountsActivity::class.java))
            return true
        }
    }
}

private class DeprecatedAccountAdapter(
    private val onExportClick: (DeprecatedAccount) -> Unit,
) : RecyclerView.Adapter<DeprecatedAccountAdapter.Holder>() {
    private var items: List<DeprecatedAccount> = emptyList()

    fun submit(accounts: List<DeprecatedAccount>) {
        items = accounts
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_deprecated_account, parent, false)
        )

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val account = items[position]
        val context: Context = holder.itemView.context
        holder.label.text = account.label
        holder.coin.text = account.coin.coinName
        holder.balance.text = account.formattedBalance()
        if (account.isFromMasterSeed) {
            holder.export.visibility = View.VISIBLE
            holder.note.visibility = View.GONE
            holder.export.setOnClickListener { onExportClick(account) }
        } else {
            holder.export.visibility = View.GONE
            holder.note.visibility = View.VISIBLE
            holder.note.text = context.getString(R.string.deprecated_accounts_no_key)
        }
    }

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val label: TextView = view.findViewById(R.id.tvLabel)
        val coin: TextView = view.findViewById(R.id.tvCoin)
        val balance: TextView = view.findViewById(R.id.tvBalance)
        val note: TextView = view.findViewById(R.id.tvNote)
        val export: View = view.findViewById(R.id.btExport)
    }
}
