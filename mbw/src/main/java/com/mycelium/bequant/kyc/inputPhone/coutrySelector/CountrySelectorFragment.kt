package com.mycelium.bequant.kyc.inputPhone.coutrySelector;

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import com.mycelium.bequant.BequantConstants.ACTION_COUNTRY_SELECTED
import com.mycelium.bequant.BequantConstants.COUNTRY_MODEL_KEY
import com.mycelium.bequant.kyc.BequantKycViewModel
import com.mycelium.wallet.databinding.ActivityBequantKycCountryOfResidenceBinding

class CountrySelectorFragment : Fragment() {

    val viewModel: CountrySelectorViewModel by viewModels()
    val activityViewModel: BequantKycViewModel by activityViewModels()

    val args by navArgs<CountrySelectorFragmentArgs>()
    private var showPhoneCode = true
    private var binding: ActivityBequantKycCountryOfResidenceBinding? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        activityViewModel.updateActionBarTitle("Country of Residence")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            ActivityBequantKycCountryOfResidenceBinding.inflate(inflater, container, false)
                    .apply {
                        binding = this
                        viewModel = this@CountrySelectorFragment.viewModel
                        lifecycleOwner = this@CountrySelectorFragment
                    }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.rvCountries?.addItemDecoration(DividerItemDecoration(binding?.rvCountries?.context, DividerItemDecoration.VERTICAL))
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
        adapter.nationality = args.nationality
        adapter.showPhoneCode = args.showPhoneCode
        binding?.rvCountries?.adapter = adapter
        viewModel.search.observe(viewLifecycleOwner, Observer { text ->
            if (text.isNullOrEmpty()) {
                adapter.submitList(countryModels)
            } else {
                adapter.submitList(countryModels.filter {
                    if (args.nationality) {
                        it.nationality?.contains(text, true) == true || it.acronym3.contains(text, true)
                    } else {
                        it.name.contains(text, true) || it.acronym3.contains(text, true)
                    }
                })
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

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}
