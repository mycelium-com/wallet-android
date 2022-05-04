package com.mycelium.wallet.external.changelly2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.mycelium.giftbox.purchase.adapter.AccountAdapter
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.modern.model.accounts.AccountsGroupModel
import com.mycelium.wallet.activity.modern.model.accounts.AccountsListModel
import com.mycelium.wallet.databinding.FragmentChangelly2SelectAccountBinding


class SelectAccountFragment : DialogFragment() {
    val adapter = AccountAdapter()

    var binding: FragmentChangelly2SelectAccountBinding? = null
    private val listModel: AccountsListModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.CustomDialog);
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
            FragmentChangelly2SelectAccountBinding.inflate(inflater).apply {
                binding = this
            }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.list?.adapter = adapter
        listModel.accountsData.observe(viewLifecycleOwner) {
            generateAccountList(it)
        }
        val accountsGroupsList = listModel.accountsData.value!!
        generateAccountList(accountsGroupsList)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    private fun generateAccountList(accountView: List<AccountsGroupModel>) {

    }
}