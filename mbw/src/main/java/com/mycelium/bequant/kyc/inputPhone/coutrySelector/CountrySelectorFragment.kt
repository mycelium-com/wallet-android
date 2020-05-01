package com.mycelium.bequant.kyc.inputPhone.coutrySelector;

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import com.mycelium.bequant.kyc.BequantKycViewModel
import com.mycelium.wallet.R
import com.mycelium.wallet.databinding.ActivityBequantKycCountryOfResidenceBinding
import kotlinx.android.synthetic.main.activity_bequant_kyc_country_of_residence.*

class CountrySelectorFragment : Fragment(R.layout.activity_bequant_kyc_country_of_residence) {

    lateinit var viewModel: CountrySelectorViewModel
    private lateinit var activityViewModel: BequantKycViewModel
    private var showPhoneCode = true

    companion object {
        val COUNTRY_MODEL_RESULT_CODE = 101
        val COUNTRY_MODEL_KEY = "phoneModel"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(CountrySelectorViewModel::class.java)
        showPhoneCode = arguments?.getBoolean("showPhoneCode") ?: true
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        activity?.run {
            activityViewModel = ViewModelProviders.of(this).get(BequantKycViewModel::class.java)
        } ?: throw Throwable("invalid activity")
        activityViewModel.updateActionBarTitle("Country of Residence")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            DataBindingUtil.inflate<ActivityBequantKycCountryOfResidenceBinding>(inflater, R.layout.activity_bequant_kyc_country_of_residence, container, false)
                    .apply {
                        viewModel = this@CountrySelectorFragment.viewModel
                        lifecycleOwner = this@CountrySelectorFragment
                    }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        rvCountries.addItemDecoration(DividerItemDecoration(rvCountries.context, DividerItemDecoration.VERTICAL))
        val adapter = CountriesAdapter(object : CountriesAdapter.ItemClickListener {
            override fun onItemClick(countryModel: CountryModel) {
                activityViewModel.country.value = countryModel
                findNavController().popBackStack()
//                targetFragment?.onActivityResult(
//                        targetRequestCode,
//                        COUNTRY_MODEL_RESULT_CODE,
//                        Intent().putExtra(COUNTRY_MODEL_KEY, countryModel))
            }
        }).apply {
            submitList(CountriesSource.countryModels)
        }
        adapter.showPhoneCode = showPhoneCode
        rvCountries.adapter = adapter

        edSearch.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {
                if (newText.isNullOrEmpty()) {
                    adapter.submitList(CountriesSource.countryModels)
                    return true
                }
                val filter = CountriesSource.countryModels.filter { it.name.toLowerCase().contains(newText.toLowerCase()) }
                adapter.submitList(filter)
                return true
            }
        })
    }
}
