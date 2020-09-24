package com.mycelium.wallet.activity.fio.mapaccount

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.fio.mapaccount.adapter.AccountNamesAdapter
import com.mycelium.wallet.activity.fio.mapaccount.adapter.FIONameItem
import com.mycelium.wapi.wallet.fio.FioModule
import kotlinx.android.synthetic.main.fragment_fio_name_details.*


class FioNameDetailsFragment : Fragment(R.layout.fragment_fio_name_details) {
    val adapter = AccountNamesAdapter()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity() as AppCompatActivity).supportActionBar?.run {
            title = "FIO names"
        }
        list.adapter = adapter
        val walletManager = MbwManager.getInstance(requireContext()).getWalletManager(false)
        val fioModule = walletManager.getModuleById(FioModule.ID) as FioModule
        val names = fioModule.getAllFIONames()
        adapter.submitList(names.map { FIONameItem(it) })
        addFIOName.setOnClickListener {
            findNavController().navigate(R.id.actionNext)
        }
    }
}