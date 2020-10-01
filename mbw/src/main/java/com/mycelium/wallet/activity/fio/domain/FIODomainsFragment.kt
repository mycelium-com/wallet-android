package com.mycelium.wallet.activity.fio.domain

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.fio.domain.adapter.DomainListAdapter
import com.mycelium.wallet.activity.fio.domain.adapter.FIODomainItem
import com.mycelium.wallet.activity.fio.domain.adapter.FIONameItem
import com.mycelium.wallet.activity.fio.mapaccount.AccountMappingActivity
import com.mycelium.wallet.activity.fio.mapaccount.adapter.Item
import com.mycelium.wallet.activity.fio.registerdomain.RegisterFIODomainActivity
import com.mycelium.wallet.activity.view.VerticalSpaceItemDecoration
import kotlinx.android.synthetic.main.fragment_fio_domains.*
import java.util.*


class FIODomainsFragment : Fragment(R.layout.fragment_fio_domains) {

    val adapter = DomainListAdapter()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity() as AppCompatActivity).supportActionBar?.run {
            title = getString(R.string.my_fio_domains)
        }
        list.adapter = adapter
        list.itemAnimator = null
        list.addItemDecoration(VerticalSpaceItemDecoration(resources.getDimensionPixelOffset(R.dimen.fio_list_item_space)))
        val preference = requireContext().getSharedPreferences("fio_domains_preference", Context.MODE_PRIVATE)
        adapter.fioNameClickListener = {
            startActivity(Intent(context, AccountMappingActivity::class.java)
//                .putExtra("fioAccount", fioModule.getFioAccountByFioName(names.first()))
//                .putExtra("fioName", names.first())
            )
        }
        adapter.fioDomainClickListener = {
            findNavController().navigate(R.id.actionNext)
        }
        adapter.switchGroupVisibilityListener = { fioDomain ->
            preference.edit().putBoolean("isClosed${fioDomain}", !preference.getBoolean("isClosed${fioDomain}", true)).apply()
            updateList(preference)
        }
        updateList(preference)
        addFIODomain.setOnClickListener {
            startActivity(Intent(requireActivity(), RegisterFIODomainActivity::class.java)
                    .putExtra("account", MbwManager.getInstance(requireContext()).selectedAccount.id))
        }
    }

    private fun updateList(preference: SharedPreferences) {
        adapter.submitList(mutableListOf<Item>().apply {
            val domain = "my-own-domain"
            val isClosed = preference.getBoolean("isClosed${domain}", true)
            add(FIODomainItem(domain, 2, isClosed))
            if (!isClosed) {
                add(FIONameItem("name1@my-own-domain", Date()))
                add(FIONameItem("name2@my-own-domain", Date()))
            }
        })
    }
}