package com.mycelium.bequant.kyc.steps

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.viewModelScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.mycelium.bequant.BequantPreference
import com.mycelium.bequant.Constants
import com.mycelium.bequant.common.ErrorHandler
import com.mycelium.bequant.common.loader
import com.mycelium.bequant.kyc.inputPhone.coutrySelector.CountryModel
import com.mycelium.bequant.kyc.steps.adapter.ItemStep
import com.mycelium.bequant.kyc.steps.adapter.StepAdapter
import com.mycelium.bequant.kyc.steps.adapter.StepState
import com.mycelium.bequant.kyc.steps.viewmodel.HeaderViewModel
import com.mycelium.bequant.kyc.steps.viewmodel.Step2ViewModel
import com.mycelium.bequant.remote.model.KYCApplicant
import com.mycelium.bequant.remote.model.KYCRequest
import com.mycelium.bequant.remote.model.toModel
import com.mycelium.bequant.remote.repositories.Api
import com.mycelium.wallet.R
import com.mycelium.wallet.databinding.FragmentBequantSteps2Binding
import kotlinx.android.synthetic.main.fragment_bequant_steps_2.*
import kotlinx.android.synthetic.main.part_bequant_step_header.*
import kotlinx.android.synthetic.main.part_bequant_stepper_body.*

class Step2Fragment : Fragment() {
    lateinit var viewModel: Step2ViewModel
    lateinit var headerViewModel: HeaderViewModel
    lateinit var kycRequest: KYCRequest

    val args: Step2FragmentArgs by navArgs()

    val receiver = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, intent: Intent?) {
            intent?.getParcelableExtra<CountryModel>(Constants.COUNTRY_MODEL_KEY)?.let {
                viewModel.country.value = it.name
                viewModel.countryAcronym.value = it.acronym
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        kycRequest = args.kycRequest
        viewModel = ViewModelProviders.of(this).get(Step2ViewModel::class.java)
        viewModel.fromModel(kycRequest)
        headerViewModel = ViewModelProviders.of(this).get(HeaderViewModel::class.java)
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(receiver, IntentFilter(Constants.ACTION_COUNTRY_SELECTED))
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            DataBindingUtil.inflate<FragmentBequantSteps2Binding>(inflater, R.layout.fragment_bequant_steps_2, container, false)
                    .apply {
                        viewModel = this@Step2Fragment.viewModel
                        headerViewModel = this@Step2Fragment.headerViewModel
                        lifecycleOwner = this@Step2Fragment
                    }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as AppCompatActivity?)?.supportActionBar?.run {
            title = getString(R.string.identity_auth)
            setHomeAsUpIndicator(resources.getDrawable(R.drawable.ic_bequant_arrow_back))
        }
        step.text = getString(R.string.step_n, 2)
        stepProgress.progress = 2
        val stepAdapter = StepAdapter()
        stepper.adapter = stepAdapter
        stepAdapter.submitList(listOf(
                ItemStep(1, getString(R.string.personal_info), StepState.COMPLETE_EDITABLE)
                , ItemStep(2, getString(R.string.residential_address), StepState.CURRENT)
                , ItemStep(3, getString(R.string.phone_number), StepState.FUTURE)
                , ItemStep(4, getString(R.string.doc_selfie), StepState.FUTURE)))

        stepAdapter.clickListener = {
            when (it) {
                1 -> findNavController().navigate(Step2FragmentDirections.actionEditStep1().setKycRequest(kycRequest))
            }
        }

        tvCountry.setOnClickListener {
            findNavController().navigate(Step2FragmentDirections.actionSelectCountry())
        }
        btNext.setOnClickListener {
            loader(true)
            Api.signRepository.accountOnceToken(viewModel.viewModelScope, {
                it?.token?.let { onceToken ->
                    viewModel.fillModel(kycRequest)
                    val applicant = KYCApplicant(BequantPreference.getPhone(), BequantPreference.getEmail())
                    applicant.userId = onceToken
                    Api.kycRepository.create(viewModel.viewModelScope, kycRequest.toModel(applicant), {
                        findNavController().navigate(Step2FragmentDirections.actionNext(kycRequest))
                    }, { code, msg ->
                        ErrorHandler(requireContext()).handle(msg)
                    }, {
                        loader(false)
                    })
                }
            }, { code, msg ->
                loader(false)
                ErrorHandler(requireContext()).handle(msg)
            })
        }

        viewModel.run { 
            listOf(addressLine1, addressLine2, city, postcode, country).forEach { 
                it.observe(viewLifecycleOwner, Observer {
                    viewModel.nextButton.value = viewModel.isValid()
                })
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_bequant_kyc_step, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
            when (item.itemId) {
                android.R.id.home -> {
                    activity?.onBackPressed()
                    true
                }
                R.id.stepper -> {
                    item.icon = resources.getDrawable(if (stepperLayout.visibility == View.VISIBLE) R.drawable.ic_chevron_down else R.drawable.ic_chevron_up)
                    stepperLayout.visibility = if (stepperLayout.visibility == View.VISIBLE) View.GONE else View.VISIBLE
                    true
                }
                else -> super.onOptionsItemSelected(item)
            }

    override fun onDestroy() {
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(receiver)
        super.onDestroy()
    }
}
