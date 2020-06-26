package com.mycelium.bequant.kyc.callback

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.mycelium.bequant.BequantPreference
import com.mycelium.bequant.kyc.steps.adapter.ItemStep
import com.mycelium.bequant.kyc.steps.adapter.StepAdapter
import com.mycelium.bequant.kyc.steps.adapter.StepState
import com.mycelium.wallet.R
import kotlinx.android.synthetic.main.fragment_kyc_approved_callback.stepper
import kotlinx.android.synthetic.main.fragment_kyc_reject_callback.*


class CallbackRejectFragment : Fragment(R.layout.fragment_kyc_reject_callback) {
    private val stepAdapter = StepAdapter()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as AppCompatActivity?)?.supportActionBar?.run {
            title = getString(R.string.identity_auth)
            setHomeAsUpIndicator(resources.getDrawable(R.drawable.ic_bequant_arrow_back))
        }

        stepper.adapter = stepAdapter
        stepAdapter.submitList(listOf(ItemStep(0, getString(R.string.phone_number), StepState.COMPLETE)
                , ItemStep(1, getString(R.string.personal_info), StepState.COMPLETE_EDITABLE)
                , ItemStep(2, getString(R.string.residential_address), StepState.COMPLETE_EDITABLE)
                , ItemStep(3, getString(R.string.doc_selfie), StepState.ERROR)))
        stepAdapter.clickListener = {
            when (it) {
                3 -> findNavController().navigate(CallbackRejectFragmentDirections.actionEditStep3(BequantPreference.getKYCRequest()))
            }
        }
        message.text = ""
        seeMore.setOnClickListener {
            seeMore.visibility = View.GONE
            message.visibility = View.VISIBLE
        }
        updateInfo.setOnClickListener {
            findNavController().navigate(CallbackRejectFragmentDirections.actionEditStep3(BequantPreference.getKYCRequest()))
        }
    }
}