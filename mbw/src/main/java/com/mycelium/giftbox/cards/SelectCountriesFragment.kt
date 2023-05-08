package com.mycelium.giftbox.cards

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration.VERTICAL
import com.mycelium.bequant.kyc.inputPhone.coutrySelector.CountryModel
import com.mycelium.giftbox.cards.adapter.ALL_COUNTRIES
import com.mycelium.giftbox.cards.adapter.RUSSIA
import com.mycelium.giftbox.cards.adapter.SelectCountriesAdapter
import com.mycelium.giftbox.cards.viewmodel.GiftBoxViewModel
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.view.DividerItemDecoration
import com.mycelium.wallet.databinding.FragmentSelectCountriesBinding


class SelectCountriesFragment : Fragment() {

    val activityViewModel: GiftBoxViewModel by activityViewModels()
    val adapter = SelectCountriesAdapter()
    var binding: FragmentSelectCountriesBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
            FragmentSelectCountriesBinding.inflate(inflater).apply {
                binding = this
            }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.list?.adapter = adapter
        binding?.list?.addItemDecoration(DividerItemDecoration(resources.getDrawable(R.drawable.divider_bequant), VERTICAL))
        adapter.toggleChecked(if (activityViewModel.selectedCountries.value?.isNotEmpty() == true) {
            activityViewModel.selectedCountries.value!!.first()
        } else {
            ALL_COUNTRIES
        })
        adapter.selectedChangeListener = {
            activityViewModel.selectedCountries.value = if (it.code != 0) listOf(it) else listOf()
            activityViewModel.reloadStore = true
            binding?.list?.postDelayed({ findNavController().popBackStack() }, 150) //for more smotch ui
        }
        val countryList = listOf(ALL_COUNTRIES) +
                (listOf(RUSSIA) + (activityViewModel.countries.value ?: emptyList()))
                        ?.sortedWith(compareBy({ activityViewModel.selectedCountries.value?.contains(it) != true }, { it.name }))!!
        adapter.submitList(countryList)
        binding?.search?.doAfterTextChanged { search ->
            adapter.submitList(countryList
                    .filter { it.name.contains(search.toString(), true) || it.acronym3.contains(search.toString(), true) })
        }
        binding?.clear?.setOnClickListener {
            binding?.search?.text = null
        }
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}