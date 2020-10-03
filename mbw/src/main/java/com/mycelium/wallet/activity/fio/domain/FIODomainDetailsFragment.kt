package com.mycelium.wallet.activity.fio.domain

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.navArgs
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.fio.FakeService
import com.mycelium.wallet.activity.fio.domain.adapter.DomainDetailsAdapter
import com.mycelium.wallet.activity.fio.domain.adapter.FIONameItem
import com.mycelium.wallet.activity.fio.domain.viewmodel.FIODomainViewModel
import com.mycelium.wallet.activity.fio.mapaccount.AccountMappingActivity
import com.mycelium.wallet.activity.fio.registername.RegisterFioNameActivity
import com.mycelium.wallet.activity.view.VerticalSpaceItemDecoration
import com.mycelium.wallet.databinding.FragmentFioDomainDetailsBinding
import com.mycelium.wapi.wallet.fio.FIODomainService
import kotlinx.android.synthetic.main.fragment_fio_account_mapping.list
import kotlinx.android.synthetic.main.fragment_fio_domain_details.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class FIODomainDetailsFragment : Fragment() {

    private val viewModel: FIODomainViewModel by activityViewModels()

    val service: FIODomainService = FakeService
    val adapter = DomainDetailsAdapter()

    val args: FIODomainDetailsFragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            DataBindingUtil.inflate<FragmentFioDomainDetailsBinding>(inflater, R.layout.fragment_fio_domain_details, container, false)
                    .apply {
                        viewModel = this@FIODomainDetailsFragment.viewModel
                        lifecycleOwner = this@FIODomainDetailsFragment
                    }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity() as AppCompatActivity).supportActionBar?.run {
            title = getString(R.string.domain_details)
        }
        viewModel.fioDomain.value = args.domain.domain
        viewModel.fioDomainExpireDate.value = args.domain.expireDate
        list.addItemDecoration(VerticalSpaceItemDecoration(resources.getDimensionPixelOffset(R.dimen.fio_list_item_space)))
        list.adapter = adapter
        list.itemAnimator = null
        adapter.clickListener = {
            startActivity(Intent(context, AccountMappingActivity::class.java)
//                .putExtra("acc", fioModule.getFioAccountByFioName(names.first()))
//                .putExtra("fioName", names.first())
            )
        }
        updateList()
        createFIOName.setOnClickListener {
            startActivity(Intent(requireActivity(), RegisterFioNameActivity::class.java)
                    .putExtra("account", MbwManager.getInstance(requireContext()).selectedAccount.id))
        }
    }

    fun updateList() {
        CoroutineScope(Dispatchers.IO).launch {
            adapter.submitList(service.getFIONames(args.domain).map {
                FIONameItem(it.name, it.expireDate)
            })
        }
    }
}