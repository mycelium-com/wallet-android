package com.mycelium.bequant.kyc

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.mycelium.bequant.kyc.steps.adapter.ItemStep
import com.mycelium.bequant.kyc.steps.adapter.StepAdapter
import com.mycelium.bequant.kyc.steps.adapter.StepState
import com.mycelium.wallet.R
import kotlinx.android.synthetic.main.fragment_kyc_final_presubmit.*

class FinalPresubmitFragment : Fragment(R.layout.fragment_kyc_final_presubmit) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as AppCompatActivity?)?.supportActionBar?.title = "Identity Authentication "
        (activity as AppCompatActivity?)?.supportActionBar?.setHomeAsUpIndicator(resources.getDrawable(R.drawable.ic_bequant_arrow_back))

        //TODO demo
        val stepAdapter = StepAdapter()
        stepper.adapter = stepAdapter
        stepAdapter.submitList(listOf(ItemStep(0, "Phone Number", StepState.COMPLETE)
                , ItemStep(1, "Personal information", StepState.COMPLETE)
                , ItemStep(2, "Residential Address", StepState.COMPLETE)
                , ItemStep(3, "Documents & Selfie", StepState.COMPLETE)))
        //
        submitButton.setOnClickListener {
            Toast.makeText(requireActivity(),"Submitted",Toast.LENGTH_LONG).show()
//            findNavController().navigate(FinalPresubmitFragmentDirections.actionSubmit())
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