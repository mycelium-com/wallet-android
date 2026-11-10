package com.mycelium.wallet.activity.addaccount

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.databinding.DialogAddErc20TokenBinding
import com.mycelium.wapi.wallet.trc20.coins.TRC20Token
import com.mycelium.wapi.wallet.tron.TronAccount
import java.util.UUID

class AddTRC20TokenDialog : DialogFragment() {

    var binding: DialogAddErc20TokenBinding? = null

    var listener: ((List<TRC20Token>) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.MyceliumModern_Dialog_BlueButtons_InFragment)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        DialogAddErc20TokenBinding.inflate(inflater, container, false).apply {
            binding = this
        }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val tronAccountId = arguments?.getSerializable("account_id") as UUID?
        val mbwManager = MbwManager.getInstance(context)
        val tronAccount = if (tronAccountId != null) mbwManager.getWalletManager(false).getAccount(tronAccountId) else null

        val supportedTokens = mbwManager.supportedTRC20Tokens.values.toList().sortedBy { it.name }
        val addedTokens = getAddedTokens(mbwManager, tronAccountId)
        val adapter = TRC20TokenAdapter(addedTokens)
        adapter.selectListener = {
            binding?.buttonOk?.isEnabled = adapter.getSelectedList().isNotEmpty()
        }
        adapter.submit(supportedTokens)
        binding?.list?.adapter = adapter
        binding?.buttonOk?.setOnClickListener {
            dismiss()
            listener?.invoke(adapter.getSelectedList())
        }
        binding?.buttonCancel?.setOnClickListener {
            dismiss()
        }

        if (addedTokens.size < supportedTokens.size) {
            binding?.title?.titleText?.text = if (tronAccount != null) getString(R.string.select_trc20_token, tronAccount.label) else getString(R.string.select_trc20_token_new_account)
        } else {
            binding?.title?.titleText?.text = if (tronAccount != null) getString(R.string.list_added_trc20_tokens, tronAccount.label) else getString(R.string.list_added_trc20_tokens_new_account)
            binding?.buttonOk?.isVisible = false
        }
    }

    override fun onResume() {
        super.onResume()
        val width = (resources.displayMetrics.widthPixels * 0.90).toInt()
        val height = (resources.displayMetrics.heightPixels * 0.80).toInt()
        dialog?.window?.setLayout(width, height)
    }

    /** All supported tokens already enabled for the account with [tronAccountId] (none for a new account). */
    private fun getAddedTokens(mbwManager: MbwManager, tronAccountId: UUID?): List<TRC20Token> =
        mutableListOf<TRC20Token>().apply {
            if (tronAccountId != null) {
                val tronAccount = mbwManager.getWalletManager(false).getAccount(tronAccountId)
                val enabledTokens = (tronAccount as TronAccount).enabledTokens
                addAll(mbwManager.supportedTRC20Tokens.filter { enabledTokens.contains(it.value.contractAddress) }.values)
            }
        }
}
