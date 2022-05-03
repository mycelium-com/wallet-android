package com.mycelium.wallet.external.changelly2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.mycelium.giftbox.purchase.adapter.AccountAdapter
import com.mycelium.wallet.activity.modern.model.accounts.AccountsListModel
import com.mycelium.wallet.databinding.FragmentChangelly2SelectAccountBinding


class SelectAccountFragment : Fragment() {
    val adapter = AccountAdapter()

    var binding: FragmentChangelly2SelectAccountBinding? = null
    private val listModel: AccountsListModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            FragmentChangelly2SelectAccountBinding.inflate(inflater).apply {
                binding = this
            }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.list?.adapter = adapter
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}