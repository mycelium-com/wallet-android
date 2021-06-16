package com.mycelium.giftbox.cards

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DividerItemDecoration.VERTICAL
import com.mycelium.bequant.kyc.inputPhone.coutrySelector.CountryModel
import com.mycelium.giftbox.cards.adapter.SelectCountiesAdapter
import com.mycelium.giftbox.cards.viewmodel.GiftBoxViewModel
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.view.DividerItemDecoration
import com.mycelium.wallet.databinding.FragmentSelectCountiesBinding


class SelectCountiesFragment : Fragment() {

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
        adapter.selected.addAll(activityViewModel.selectedCountries.value ?: listOf())
        adapter.submitList(listOf(CountryModel("All Countries", "", "", 0)) +
                activityViewModel.countries.value
                        ?.sortedWith(compareBy({ activityViewModel.selectedCountries.value?.contains(it) != true }, { it.name }))!!)
        binding?.search?.doAfterTextChanged { search ->
            adapter.submitList(activityViewModel.countries.value
                    ?.filter { it.name.contains(search.toString(), true) || it.acronym3.contains(search.toString(), true) })
        }
        binding?.clear?.setOnClickListener {
            binding?.search?.text = null
        }
    }

    override fun onPause() {
        activityViewModel.selectedCountries.value = adapter.selected.filter { it.code != 0 }
        super.onPause()
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}