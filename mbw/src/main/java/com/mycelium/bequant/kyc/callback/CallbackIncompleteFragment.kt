package com.mycelium.bequant.kyc.callback

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.mycelium.bequant.BequantPreference
import com.mycelium.bequant.kyc.steps.adapter.ItemStep
import com.mycelium.bequant.kyc.steps.adapter.StepAdapter
import com.mycelium.bequant.kyc.steps.adapter.StepState
import com.mycelium.bequant.remote.repositories.Api
import com.mycelium.wallet.R
import kotlinx.android.synthetic.main.fragment_bequant_kyc_approved_callback.stepper
import kotlinx.android.synthetic.main.fragment_bequant_kyc_incomplete_callback.*


class CallbackIncompleteFragment : Fragment(R.layout.fragment_bequant_kyc_incomplete_callback) {

    private val stepAdapter = StepAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as AppCompatActivity?)?.supportActionBar?.run {
            title = getString(R.string.identity_auth)
            setHomeAsUpIndicator(resources.getDrawable(R.drawable.ic_bequant_clear))
        }

        stepper.adapter = stepAdapter
        Api.kycRepository.status(lifecycleScope, { success ->
            stepAdapter.submitList(listOf(
                    ItemStep(1, getString(R.string.personal_info),
                            if (success.sections.map { it.entries.first() }.firstOrNull { it.key == "personal_information" }?.value == false) StepState.ERROR else StepState.COMPLETE),
                    ItemStep(2, getString(R.string.residential_address),
                            if (success.sections.map { it.entries.first() }.firstOrNull { it.key == "residential_address" }?.value == false) StepState.ERROR else StepState.COMPLETE),
                    ItemStep(3, getString(R.string.phone_number),
                            if (success.sections.map { it.entries.first() }.firstOrNull { it.key == "phone" }?.value == false) StepState.ERROR else StepState.COMPLETE),
                    ItemStep(4, getString(R.string.doc_selfie),
                            if (success.sections.map { it.entries.first() }.firstOrNull { it.key == "documents" }?.value == false) StepState.ERROR else StepState.COMPLETE)))
        })
        stepAdapter.clickListener = {
            when (it) {
                1 -> findNavController().navigate(CallbackIncompleteFragmentDirections.actionEditStep1(BequantPreference.getKYCRequest()))
                2 -> findNavController().navigate(CallbackIncompleteFragmentDirections.actionEditStep2(BequantPreference.getKYCRequest()))
                3 -> findNavController().navigate(CallbackIncompleteFragmentDirections.actionEditStep3(BequantPreference.getKYCRequest()))
                4 -> findNavController().navigate(CallbackIncompleteFragmentDirections.actionEditDocs(BequantPreference.getKYCRequest()))
            }
        }
        val kycStatusMsg = BequantPreference.getKYCStatusMessage()
        message.text = kycStatusMsg
        seeMore.visibility = if (kycStatusMsg?.isNotEmpty() == true) View.VISIBLE else View.GONE
        seeMore.setOnClickListener {
            seeMore.visibility = View.GONE
            message.visibility = View.VISIBLE
        }
        updateInfo.setOnClickListener {
            when {
                !BequantPreference.getKYCSectionStatus("personal_information") -> {
                    findNavController().navigate(CallbackIncompleteFragmentDirections.actionEditStep1(BequantPreference.getKYCRequest()))
                }
                !BequantPreference.getKYCSectionStatus("residential_address") -> {
                    findNavController().navigate(CallbackIncompleteFragmentDirections.actionEditStep2(BequantPreference.getKYCRequest()))
                }
                !BequantPreference.getKYCSectionStatus("phone") -> {
                    findNavController().navigate(CallbackIncompleteFragmentDirections.actionEditStep3(BequantPreference.getKYCRequest()))
                }
                !BequantPreference.getKYCSectionStatus("documents") -> {
                    findNavController().navigate(CallbackIncompleteFragmentDirections.actionEditDocs(BequantPreference.getKYCRequest()))
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
            when (item.itemId) {
                android.R.id.home -> {
                    activity?.finish()
                    true
                }
                else -> super.onOptionsItemSelected(item)
            }
}