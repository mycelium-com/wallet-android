package com.mycelium.bequant.kyc.callback

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
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
import com.mycelium.wallet.databinding.FragmentBequantKycIncompleteCallbackBinding


class CallbackIncompleteFragment : Fragment() {

    private val stepAdapter = StepAdapter()
    var binding: FragmentBequantKycIncompleteCallbackBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentBequantKycIncompleteCallbackBinding.inflate(inflater, container, false)
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
        Api.kycRepository.status(lifecycleScope, { success ->
            stepAdapter.submitList(listOf(
                    ItemStep(1, getString(R.string.personal_info),
                            if (BequantPreference.getKYCSectionStatus("personal_information")) StepState.ERROR else StepState.COMPLETE),
                    ItemStep(2, getString(R.string.residential_address),
                            if (BequantPreference.getKYCSectionStatus("residential_address")) StepState.ERROR else StepState.COMPLETE),
                    ItemStep(3, getString(R.string.phone_number),
                            if (BequantPreference.getKYCSectionStatus("phone")) StepState.ERROR else StepState.COMPLETE),
                    ItemStep(4, getString(R.string.doc_selfie),
                            if (BequantPreference.getKYCSectionStatus("documents")) StepState.ERROR else StepState.COMPLETE)))
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
        binding?.message?.text = kycStatusMsg
        binding?.seeMore?.visibility = if (kycStatusMsg?.isNotEmpty() == true) View.VISIBLE else View.GONE
        binding?.seeMore?.setOnClickListener {
            binding?.seeMore?.visibility = View.GONE
            binding?.message?.visibility = View.VISIBLE
        }
        binding?.updateInfo?.setOnClickListener {
            when {
                BequantPreference.getKYCSectionStatus("personal_information") -> {
                    findNavController().navigate(CallbackIncompleteFragmentDirections.actionEditStep1(BequantPreference.getKYCRequest()))
                }
                BequantPreference.getKYCSectionStatus("residential_address") -> {
                    findNavController().navigate(CallbackIncompleteFragmentDirections.actionEditStep2(BequantPreference.getKYCRequest()))
                }
                BequantPreference.getKYCSectionStatus("phone") -> {
                    findNavController().navigate(CallbackIncompleteFragmentDirections.actionEditStep3(BequantPreference.getKYCRequest()))
                }
                BequantPreference.getKYCSectionStatus("documents") -> {
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

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}