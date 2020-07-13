package com.mycelium.bequant.kyc

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.mycelium.bequant.BequantPreference
import com.mycelium.bequant.common.ErrorHandler
import com.mycelium.bequant.common.loader
import com.mycelium.bequant.kyc.steps.adapter.ItemStep
import com.mycelium.bequant.kyc.steps.adapter.StepAdapter
import com.mycelium.bequant.kyc.steps.adapter.StepState
import com.mycelium.bequant.remote.model.KYCDocument
import com.mycelium.bequant.remote.model.KYCRequest
import com.mycelium.bequant.remote.repositories.Api
import com.mycelium.wallet.R
import kotlinx.android.synthetic.main.fragment_bequant_kyc_final_presubmit.*
import java.io.File
import java.util.*

class FinalPresubmitFragment : Fragment(R.layout.fragment_bequant_kyc_final_presubmit) {

    val args: FinalPresubmitFragmentArgs by navArgs()
    private val stepAdapter = StepAdapter()
    lateinit var kycRequest: KYCRequest

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        kycRequest = args.kycRequest
        BequantPreference.setKYCRequest(kycRequest)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as AppCompatActivity?)?.supportActionBar?.run {
            title = getString(R.string.identity_auth)
            setHomeAsUpIndicator(resources.getDrawable(R.drawable.ic_bequant_arrow_back))
        }
        stepper.adapter = stepAdapter
        stepAdapter.submitList(listOf(
                ItemStep(1, getString(R.string.personal_info), StepState.COMPLETE)
                , ItemStep(2, getString(R.string.residential_address), StepState.COMPLETE)
                , ItemStep(3, getString(R.string.phone_number), StepState.COMPLETE)
                , ItemStep(4, getString(R.string.doc_selfie), StepState.COMPLETE_EDITABLE)))
        stepAdapter.clickListener = {
            when (it) {
                4 -> findNavController().navigate(FinalPresubmitFragmentDirections.actionEditDocs(kycRequest))
            }
        }
        discardButton.setOnClickListener {
            findNavController().navigate(FinalPresubmitFragmentDirections.actionSubmit())
        }
        submitButton.setOnClickListener {
            loader(true)
            Api.kycRepository.uploadDocuments(lifecycleScope, mutableMapOf<File, KYCDocument>().apply {
                kycRequest.identityList.forEach {
                    put(File(it), KYCDocument.PASSPORT)
                }
                kycRequest.poaList.forEach {
                    put(File(it), KYCDocument.POA)
                }
                kycRequest.selfieList.forEach {
                    put(File(it), KYCDocument.SELFIE)
                }
            }, {
                BequantPreference.setKYCSubmitDate(Date())
                Toast.makeText(requireActivity(), "Submitted", Toast.LENGTH_LONG).show()
                findNavController().navigate(FinalPresubmitFragmentDirections.actionSubmit())
            }, {
                ErrorHandler(requireContext()).handle(it)
            }, {
                loader(false)
            })
        }
        if (!kycRequest.isResubmit) {
            title.text = getString(R.string.thank_you_for_completing_the_application_form)
            subtitle1.text = getString(R.string.if_you_are_happy_with_the_content_please_submit_your_application_by_pressing_the_submit_button)
            subtitle2.text = getString(R.string.we_will_notify_you_of_the_outcome_of_the_application_as_soon_as_we_review_your_information_thank_you_for_your_patience)
        } else {
            title.text = getString(R.string.do_you_want_resubmit)
            subtitle1.visibility = GONE
            subtitle2.visibility = GONE
            discardButton.visibility = VISIBLE
            submitButton.text = getString(R.string.resubmit)
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