package com.mycelium.bequant.kyc.callback

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.mycelium.bequant.kyc.steps.adapter.ItemStep
import com.mycelium.bequant.kyc.steps.adapter.StepAdapter
import com.mycelium.bequant.kyc.steps.adapter.StepState
import com.mycelium.wallet.R
import com.mycelium.wallet.databinding.FragmentBequantKycApprovedCallbackBinding


class CallbackApprovedFragment : Fragment() {

    private val stepAdapter = StepAdapter()
    var binding: FragmentBequantKycApprovedCallbackBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentBequantKycApprovedCallbackBinding.inflate(inflater, container, false)
        .apply {
            binding = this
        }
        .root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as AppCompatActivity?)?.supportActionBar?.run {
            title = getString(R.string.identity_auth)
            setHomeAsUpIndicator(resources.getDrawable(R.drawable.ic_bequant_clear))
        }

        binding?.stepper?.adapter = stepAdapter
        stepAdapter.submitList(listOf(
                ItemStep(1, getString(R.string.personal_info), StepState.COMPLETE)
                , ItemStep(2, getString(R.string.residential_address), StepState.COMPLETE)
                , ItemStep(3, getString(R.string.phone_number), StepState.COMPLETE)
                , ItemStep(4, getString(R.string.doc_selfie), StepState.COMPLETE)))

        binding?.confirmButton?.setOnClickListener {
            requireActivity().finish()
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