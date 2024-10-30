package com.mycelium.giftbox.cards.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import com.mycelium.bequant.kyc.inputPhone.coutrySelector.CountryModel
import com.mycelium.giftbox.GiftboxPreference
import com.mycelium.wallet.R


class GiftBoxViewModel(application: Application) : AndroidViewModel(application) {
    val selectedCountries = MutableLiveData<List<CountryModel>>(GiftboxPreference.selectedCountries())
    val countries = MutableLiveData<List<CountryModel>>(emptyList())
    val categories = MutableLiveData<List<String>>(emptyList())
    val currentTab = MutableLiveData<String>()

    val orderLoading = MutableLiveData<Boolean>()
    var reloadStore = false

    fun currentCountries(): LiveData<String> =
            selectedCountries.switchMap {
                GiftboxPreference.setSelectedCountries(it)
                return@switchMap MutableLiveData<String>(when (it.size) {
                    0 -> getApplication<Application>().getString(R.string.all_countries)
                    1 -> it[0].name
                    else -> getApplication<Application>().resources.getQuantityString(R.plurals.d_countries, it.size, it.size)
                })
            }
}