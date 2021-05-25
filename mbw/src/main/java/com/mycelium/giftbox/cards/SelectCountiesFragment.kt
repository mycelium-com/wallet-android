package com.mycelium.giftbox.cards

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DividerItemDecoration.VERTICAL
import com.mycelium.bequant.kyc.inputPhone.coutrySelector.CountriesSource
import com.mycelium.giftbox.cards.adapter.SelectCountiesAdapter
import com.mycelium.giftbox.cards.viewmodel.GiftBoxViewModel
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.view.DividerItemDecoration
import com.mycelium.wallet.databinding.FragmentSelectCountiesBinding


class SelectCountiesFragment : Fragment(R.layout.fragment_select_counties) {

    val activityViewModel: GiftBoxViewModel by activityViewModels()
    val adapter = SelectCountiesAdapter()
    var binding: FragmentSelectCountiesBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
            FragmentSelectCountiesBinding.inflate(inflater).apply {
                binding = this
            }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.list?.adapter = adapter
        binding?.list?.addItemDecoration(DividerItemDecoration(resources.getDrawable(R.drawable.divider_bequant), VERTICAL))
        adapter.selected.clear()
        adapter.selected.addAll(activityViewModel.counties.value ?: listOf())
        adapter.submitList(CountriesSource.countryModels)
        binding?.search?.doAfterTextChanged { search ->
            adapter.submitList(CountriesSource.countryModels
                    .filter { it.name.contains(search.toString(), true) || it.acronym.contains(search.toString(), true) })
        }
        binding?.clear?.setOnClickListener {
            binding?.search?.text = null
        }
    }

    override fun onPause() {
        activityViewModel.counties.postValue(adapter.selected)
        super.onPause()
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}