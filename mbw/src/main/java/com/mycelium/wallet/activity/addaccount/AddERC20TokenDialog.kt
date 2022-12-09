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
import com.mycelium.wapi.wallet.erc20.coins.ERC20Token
import com.mycelium.wapi.wallet.eth.EthAccount
import java.util.*


class AddERC20TokenDialog : DialogFragment() {

    var binding: DialogAddErc20TokenBinding? = null

    var listener: ((List<ERC20Token>) -> Unit)? = null

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
        val ethAccountId = arguments?.getSerializable("account_id") as UUID?
        val mbwManager = MbwManager.getInstance(context)
        val ethAccount = if (ethAccountId != null) mbwManager.getWalletManager(false).getAccount(ethAccountId) else null

        val supportedTokens = mbwManager.supportedERC20Tokens.values.toList().sortedBy { it.name }
        val addedTokens = getAddedTokens(mbwManager, ethAccountId)
        val adapter = ERC20TokenAdapter(addedTokens)
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
            binding?.title?.titleText?.text = if (ethAccount != null) getString(R.string.select_token, ethAccount.label) else getString(R.string.select_token_new_account)
        } else {
            binding?.title?.titleText?.text = if (ethAccount != null) getString(R.string.list_added_tokens, ethAccount.label) else getString(R.string.list_added_tokens_new_account)
            binding?.buttonOk?.isVisible = false
        }
    }

    override fun onResume() {
        super.onResume()
        val width = (resources.displayMetrics.widthPixels * 0.90).toInt()
        val height = (resources.displayMetrics.heightPixels * 0.80).toInt()
        dialog?.window?.setLayout(width, height)
    }

    /**
     * @return all supported tokens or all supported tokens except these that are already enabled for
     * account with ethAccountId.
     */
    private fun getAddedTokens(mbwManager: MbwManager, ethAccountId: UUID?): List<ERC20Token> =
            mutableListOf<ERC20Token>().apply {
                if (ethAccountId != null) {
                    val ethAccount = mbwManager.getWalletManager(false).getAccount(ethAccountId)
                    val enabledTokens = (ethAccount as EthAccount).enabledTokens
                    addAll(mbwManager.supportedERC20Tokens.filter { enabledTokens.contains(it.value.contractAddress) }.values)
                }
            }
}