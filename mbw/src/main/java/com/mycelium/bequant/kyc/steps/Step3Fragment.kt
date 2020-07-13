package com.mycelium.bequant.kyc.steps

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.mycelium.bequant.BequantPreference
import com.mycelium.bequant.Constants.COUNTRY_MODEL_KEY
import com.mycelium.bequant.common.ErrorHandler
import com.mycelium.bequant.common.loader
import com.mycelium.bequant.kyc.steps.adapter.ItemStep
import com.mycelium.bequant.kyc.steps.adapter.StepAdapter
import com.mycelium.bequant.kyc.steps.adapter.StepState
import com.mycelium.bequant.kyc.steps.viewmodel.HeaderViewModel
import com.mycelium.bequant.kyc.steps.viewmodel.InputPhoneViewModel
import com.mycelium.bequant.remote.model.KYCRequest
import com.mycelium.bequant.remote.repositories.Api
import com.mycelium.wallet.R
import com.mycelium.wallet.databinding.FragmentBequantKycStep3Binding
import kotlinx.android.synthetic.main.fragment_bequant_kyc_step_3.*
import kotlinx.android.synthetic.main.part_bequant_step_header.*
import kotlinx.android.synthetic.main.part_bequant_stepper_body.*

class Step3Fragment : Fragment() {

    lateinit var headerViewModel: HeaderViewModel
    lateinit var viewModel: InputPhoneViewModel
    lateinit var kycRequest: KYCRequest

    val args: Step3FragmentArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        kycRequest = args.kycRequest ?: KYCRequest()
        viewModel = ViewModelProviders.of(this).get(InputPhoneViewModel::class.java)
        headerViewModel = ViewModelProviders.of(this).get(HeaderViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            DataBindingUtil.inflate<FragmentBequantKycStep3Binding>(inflater, R.layout.fragment_bequant_kyc_step_3, container, false)
                    .apply {
                        viewModel = this@Step3Fragment.viewModel
                        headerViewModel = this@Step3Fragment.headerViewModel
                        lifecycleOwner = this@Step3Fragment
                    }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as AppCompatActivity?)?.supportActionBar?.title = getString(R.string.identity_auth)
        step.text = getString(R.string.step_n, 3)
        stepProgress.progress = 3
        val stepAdapter = StepAdapter()
        stepper.adapter = stepAdapter
        stepAdapter.submitList(listOf(
                ItemStep(1, getString(R.string.personal_info), StepState.COMPLETE_EDITABLE)
                , ItemStep(2, getString(R.string.residential_address), StepState.COMPLETE_EDITABLE)
                , ItemStep(3, getString(R.string.phone_number), StepState.CURRENT)
                , ItemStep(4, getString(R.string.doc_selfie), StepState.FUTURE)))

        stepAdapter.clickListener = {
            when (it) {
                1 -> findNavController().navigate(Step3FragmentDirections.actionEditStep1(kycRequest))
                2 -> findNavController().navigate(Step3FragmentDirections.actionEditStep2(kycRequest))
            }
        }
        btGetCode.setOnClickListener {
            sendCode()
        }
        tvCountry.setOnClickListener {
            findNavController().navigate(Step3FragmentDirections.actionChooseCountry())
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            viewModel.countryModel.value = data?.getParcelableExtra(COUNTRY_MODEL_KEY)
        }
    }

    private fun sendCode() {
        tvErrorCode.visibility = View.GONE
        loader(true)
        viewModel.getRequest()?.let { request ->
            val phone = "+${request.mobilePhoneCountryCode}${request.mobilePhone}"
            BequantPreference.setPhone(phone)
            Api.kycRepository.mobileVerification(viewModel.viewModelScope, {
                AlertDialog.Builder(requireContext())
                        .setMessage(it)
                        .setPositiveButton(R.string.ok) { _, _ ->
                            findNavController().navigate(Step3FragmentDirections.actionNext(kycRequest))
                        }
                        .show()
            }, { code, error ->
                ErrorHandler(requireContext()).handle(error)
            }, { loader(false) })
        } ?: run {
            tvErrorCode.visibility = View.VISIBLE
        }
    }
}