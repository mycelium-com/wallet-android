package com.mycelium.bequant.kyc

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.mycelium.bequant.BequantPreference
import com.mycelium.bequant.common.ErrorHandler
import com.mycelium.bequant.common.loader
import com.mycelium.bequant.kyc.steps.adapter.ItemStep
import com.mycelium.bequant.kyc.steps.adapter.StepAdapter
import com.mycelium.bequant.kyc.steps.adapter.StepState
import com.mycelium.bequant.remote.model.KYCStatus
import com.mycelium.bequant.remote.repositories.Api
import com.mycelium.wallet.R
import kotlinx.android.synthetic.main.fragment_bequant_kyc_start.*


class StartFragment : Fragment(R.layout.fragment_bequant_kyc_start) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        if (BequantPreference.getKYCToken().isNotEmpty()) {
            loader(true)
            Api.kycRepository.status(lifecycleScope, { statusMsg ->
                when (statusMsg.global) {
                    KYCStatus.PENDING, KYCStatus.APPROVED, KYCStatus.SIGNED_OFF ->
                        findNavController().navigate(StartFragmentDirections.actionPending())
                    KYCStatus.INCOMPLETE ->
                        if (statusMsg.submitted) {
                            findNavController().navigate(StartFragmentDirections.actionPending())
                        } else {
                            findNavController().navigate(StartFragmentDirections.actionIncomplete())
                        }
                    KYCStatus.VERIFIED ->
                        findNavController().navigate(StartFragmentDirections.actionApproved())
                    KYCStatus.REJECTED ->
                        findNavController().navigate(StartFragmentDirections.actionRejected())
                }
            }, { code, msg ->
                ErrorHandler(requireContext()).handle(msg)
            }, {
                loader(false)
            })
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as AppCompatActivity?)?.supportActionBar?.run {
            title = getString(R.string.identity_auth)
            setHomeAsUpIndicator(resources.getDrawable(R.drawable.ic_bequant_arrow_back))
        }
        val stepAdapter = StepAdapter()
        stepper.adapter = stepAdapter
        stepAdapter.submitList(listOf(
                ItemStep(1, getString(R.string.personal_info), StepState.FUTURE)
                , ItemStep(2, getString(R.string.residential_address), StepState.FUTURE)
                , ItemStep(3, getString(R.string.phone_number), StepState.FUTURE)
                , ItemStep(4, getString(R.string.doc_selfie), StepState.FUTURE)))
        btnStart.setOnClickListener {
            findNavController().navigate(StartFragmentDirections.actionNext())
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