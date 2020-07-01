package com.mycelium.bequant.kyc.checkCode

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
import com.mycelium.bequant.common.loader
import com.mycelium.bequant.kyc.steps.Step3FragmentDirections
import com.mycelium.bequant.kyc.steps.adapter.ItemStep
import com.mycelium.bequant.kyc.steps.adapter.StepAdapter
import com.mycelium.bequant.kyc.steps.adapter.StepState
import com.mycelium.bequant.kyc.steps.viewmodel.HeaderViewModel
import com.mycelium.bequant.remote.KYCRepository
import com.mycelium.bequant.remote.model.KYCRequest
import com.mycelium.wallet.R
import com.mycelium.wallet.databinding.FragmentBequantKycVerifyPhoneBinding
import com.poovam.pinedittextfield.PinField
import kotlinx.android.synthetic.main.fragment_bequant_kyc_verify_phone.*
import kotlinx.android.synthetic.main.part_bequant_step_header.*
import kotlinx.android.synthetic.main.part_bequant_stepper_body.*

class VerifyPhoneFragment : Fragment(R.layout.fragment_bequant_kyc_verify_phone) {

    lateinit var headerViewModel: HeaderViewModel
    lateinit var viewModel: VerifyPhoneViewModel
    lateinit var kycRequest: KYCRequest

    val args: VerifyPhoneFragmentArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        kycRequest = args.kycRequest ?: KYCRequest()
        viewModel = ViewModelProviders.of(this).get(VerifyPhoneViewModel::class.java)
        headerViewModel = ViewModelProviders.of(this).get(HeaderViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            DataBindingUtil.inflate<FragmentBequantKycVerifyPhoneBinding>(inflater, R.layout.fragment_bequant_kyc_verify_phone, container, false)
                    .apply {
                        viewModel = this@VerifyPhoneFragment.viewModel
                        headerViewModel = this@VerifyPhoneFragment.headerViewModel
                        lifecycleOwner = this@VerifyPhoneFragment
                    }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as AppCompatActivity?)?.supportActionBar?.title = getString(R.string.identity_auth)
        step.text = getString(R.string.step_n, 3)
        stepProgress.progress = 3
//        tvSubtitle.text = getString(R.string.we_will_call_to_give_you_a_confirmation_code, kycRequest.phone)
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
        pinCode.onTextCompleteListener = object : PinField.OnTextCompleteListener {
            override fun onTextComplete(enteredText: String): Boolean {
                checkCode(enteredText)
                return true
            }
        }
        tvTryAgain.setOnClickListener {
            resendCode()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_bequant_kyc_step, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
            when (item.itemId) {
                R.id.stepper -> {
                    item.icon = resources.getDrawable(if (stepperLayout.visibility == View.VISIBLE) R.drawable.ic_chevron_down else R.drawable.ic_chevron_up)
                    stepperLayout.visibility = if (stepperLayout.visibility == View.VISIBLE) View.GONE else View.VISIBLE
                    true
                }
                else -> super.onOptionsItemSelected(item)
            }

    private fun resendCode() {
        loader(true)
        KYCRepository.repository.mobileVerification(viewModel.viewModelScope) {
            loader(false)
            AlertDialog.Builder(requireContext())
                    .setMessage(it)
                    .setPositiveButton(R.string.button_ok) { _, _ ->
                    }.show()
        }
    }

    private fun checkCode(code: String) {
        loader(true)
        KYCRepository.repository.checkMobileVerification(viewModel.viewModelScope, code, {
            loader(false)
            findNavController().navigate(VerifyPhoneFragmentDirections.actionNext(kycRequest))
        }, {
            loader(false)
            showError()
        })
    }

    private fun showError() {
        otp_view.error = "Error code"
    }
}