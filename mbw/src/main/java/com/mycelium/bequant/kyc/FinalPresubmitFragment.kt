package com.mycelium.bequant.kyc

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.mycelium.bequant.BequantPreference
import com.mycelium.bequant.kyc.steps.adapter.ItemStep
import com.mycelium.bequant.kyc.steps.adapter.StepAdapter
import com.mycelium.bequant.kyc.steps.adapter.StepState
import com.mycelium.bequant.remote.model.KYCRequest
import com.mycelium.bequant.remote.model.KYCStatus
import com.mycelium.wallet.R
import kotlinx.android.synthetic.main.fragment_kyc_final_presubmit.*

class FinalPresubmitFragment : Fragment(R.layout.fragment_kyc_final_presubmit) {

    val args: FinalPresubmitFragmentArgs by navArgs()
    val stepAdapter = StepAdapter()
    lateinit var kycRequest: KYCRequest

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        kycRequest = args.kycRequest
        BequantPreference.setKYCRequest(kycRequest)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as AppCompatActivity?)?.supportActionBar?.title = "Identity Authentication "
        (activity as AppCompatActivity?)?.supportActionBar?.setHomeAsUpIndicator(resources.getDrawable(R.drawable.ic_bequant_arrow_back))

        stepper.adapter = stepAdapter
        stepAdapter.submitList(listOf(ItemStep(0, getString(R.string.phone_number), StepState.COMPLETE)
                , ItemStep(1, getString(R.string.personal_info), StepState.COMPLETE_EDITABLE)
                , ItemStep(2, getString(R.string.residential_address), StepState.COMPLETE_EDITABLE)
                , ItemStep(3, getString(R.string.doc_selfie), StepState.COMPLETE_EDITABLE)))
        stepAdapter.clickListener = {
            when (it) {
                1 -> findNavController().navigate(FinalPresubmitFragmentDirections.actionEditStep1(kycRequest))
                2 -> findNavController().navigate(FinalPresubmitFragmentDirections.actionEditStep2(kycRequest))
                3 -> findNavController().navigate(FinalPresubmitFragmentDirections.actionEditStep3(kycRequest))
            }
        }
        submitButton.setOnClickListener {
            Toast.makeText(requireActivity(), "Submitted", Toast.LENGTH_LONG).show()
            requireActivity().finish()
        }
        initByStatus()
    }

    private fun initByStatus() {
        when (BequantPreference.getKYCStatus()) {
            KYCStatus.PENDING -> {
                title.text = "Your application is being reviewed"
                subtitle1.text = "Application submitted: 23.11.2020 12:33\nEstimated review time: 23h left"
                subtitle2.text = "You still have the ability to change and resubmit your information."
            }
            else -> {
                title.text = getString(R.string.thank_you_for_completing_the_application_form)
                subtitle1.text = getString(R.string.if_you_are_happy_with_the_content_please_submit_your_application_by_pressing_the_submit_button)
                subtitle2.text = getString(R.string.we_will_notify_you_of_the_outcome_of_the_application_as_soon_as_we_review_your_information_thank_you_for_your_patience)
            }
        }
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