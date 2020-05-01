package com.mycelium.bequant.kyc.inputPhone.coutrySelector;

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import com.mycelium.bequant.Constants.ACTION_COUNTRY_SELECTED
import com.mycelium.bequant.Constants.COUNTRY_MODEL_KEY
import com.mycelium.bequant.kyc.BequantKycViewModel
import com.mycelium.wallet.R
import com.mycelium.wallet.databinding.ActivityBequantKycCountryOfResidenceBinding
import kotlinx.android.synthetic.main.activity_bequant_kyc_country_of_residence.*

class CountrySelectorFragment : Fragment() {

    lateinit var viewModel: CountrySelectorViewModel
    private lateinit var activityViewModel: BequantKycViewModel
    private var showPhoneCode = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
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
        val countryModels = CountriesSource.countryModels
        val adapter = CountriesAdapter(object : CountriesAdapter.ItemClickListener {
            override fun onItemClick(countryModel: CountryModel) {
                if (countryModel.acronym == "US" && !showPhoneCode) {
                    findNavController().navigate(CountrySelectorFragmentDirections.actionFail())
                } else {
                    LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(Intent(ACTION_COUNTRY_SELECTED)
                            .putExtra(COUNTRY_MODEL_KEY, countryModel))
                    if (targetFragment == null) {
                        activityViewModel.country.value = countryModel
                        findNavController().popBackStack()
                    } else {
                        targetFragment?.onActivityResult(targetRequestCode, RESULT_OK,
                                Intent().putExtra(COUNTRY_MODEL_KEY, countryModel))
                        activity?.onBackPressed()
                    }
                }
            }
        }).apply {
            submitList(countryModels)
        }
        adapter.showPhoneCode = showPhoneCode
        rvCountries.adapter = adapter
        viewModel.search.observe(viewLifecycleOwner, Observer { text ->
            if (text.isNullOrEmpty()) {
                adapter.submitList(countryModels)
            } else {
                val filter = countryModels.filter {
                    it.name.toLowerCase().contains(text.toLowerCase()) || it.acronym.toLowerCase().contains(text.toLowerCase())
                }
                adapter.submitList(filter)
            }
        })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
            when (item.itemId) {
                android.R.id.home -> {
                    activity?.onBackPressed()
                    true
                }
                else -> super.onOptionsItemSelected(item)
            }
}
