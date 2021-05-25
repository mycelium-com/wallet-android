package com.mycelium.giftbox.cards

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.mycelium.bequant.kyc.inputPhone.coutrySelector.CountriesSource
import com.mycelium.giftbox.cards.adapter.SelectCountiesAdapter
import com.mycelium.giftbox.cards.viewmodel.GiftBoxViewModel
import com.mycelium.wallet.R
import kotlinx.android.synthetic.main.fragment_select_counties.*


class SelectCountiesFragment : Fragment(R.layout.fragment_select_counties) {

    val activityViewModel: GiftBoxViewModel by activityViewModels()
    val adapter = SelectCountiesAdapter()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        list.adapter = adapter
        adapter.selected.clear()
        adapter.selected.addAll(activityViewModel.counties.value ?: listOf())
        adapter.submitList(CountriesSource.countryModels)
    }

    override fun onPause() {
        activityViewModel.counties.postValue(adapter.selected)
        super.onPause()
    }
}