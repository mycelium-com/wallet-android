package com.mycelium.bequant.kyc

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.mycelium.bequant.kyc.steps.Step3FragmentDirections
import com.mycelium.bequant.kyc.steps.adapter.ItemStep
import com.mycelium.bequant.kyc.steps.adapter.StepAdapter
import com.mycelium.bequant.kyc.steps.adapter.StepState
import com.mycelium.bequant.remote.model.KYCRequest
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
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as AppCompatActivity?)?.supportActionBar?.title = "Identity Authentication "
        (activity as AppCompatActivity?)?.supportActionBar?.setHomeAsUpIndicator(resources.getDrawable(R.drawable.ic_bequant_arrow_back))

        stepper.adapter = stepAdapter
        stepAdapter.submitList(listOf(ItemStep(0, "Phone Number", StepState.COMPLETE)
                , ItemStep(1, "Personal information", StepState.COMPLETE_EDITABLE)
                , ItemStep(2, "Residential Address", StepState.COMPLETE_EDITABLE)
                , ItemStep(3, "Documents & Selfie", StepState.COMPLETE_EDITABLE)))
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