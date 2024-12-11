package com.mycelium.bequant.kyc

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.mycelium.bequant.BequantPreference
import com.mycelium.bequant.kyc.steps.adapter.ItemStep
import com.mycelium.bequant.kyc.steps.adapter.StepAdapter
import com.mycelium.bequant.kyc.steps.adapter.StepState
import com.mycelium.wallet.R
import com.mycelium.wallet.databinding.FragmentBequantKycFinalPendingBinding
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit


class PendingFragment : Fragment(R.layout.fragment_bequant_kyc_final_pending) {

    private val stepAdapter = StepAdapter()
    private var binding: FragmentBequantKycFinalPendingBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentBequantKycFinalPendingBinding.inflate(inflater, container, false)
        .apply {
            binding = this
        }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as AppCompatActivity?)?.supportActionBar?.run {
            title = getString(R.string.identity_auth)
            setHomeAsUpIndicator(resources.getDrawable(R.drawable.ic_bequant_clear))
        }
        binding?.stepper?.adapter = stepAdapter
        stepAdapter.submitList(listOf(
                ItemStep(1, getString(R.string.personal_info), StepState.COMPLETE),
                ItemStep(2, getString(R.string.residential_address), StepState.COMPLETE),
                ItemStep(3, getString(R.string.phone_number), StepState.COMPLETE),
                ItemStep(4, getString(R.string.doc_selfie), StepState.COMPLETE)))
        binding?.subtitle3?.text = getString(R.string.you_cant_change_info)
        stepAdapter.clickListener = {
            when (it) {
                4 -> findNavController().navigate(PendingFragmentDirections.actionEditDocs(BequantPreference.getKYCRequest().apply {
                    isResubmit = true
                }))
            }
        }
        val submitDate = BequantPreference.getKYCSubmitDate()
        binding?.subtitle1?.text = getString(R.string.bequant_application_submit_time,
                submitDateFormat.format(submitDate))
        binding?.subtitle2?.text = getString(R.string.bequant_application_submit_time_left,
                REVIEW_TIME - (TimeUnit.MILLISECONDS.toHours(Date().time - submitDate.time)))
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
            when (item.itemId) {
                android.R.id.home -> {
                    activity?.finish()
                    true
                }
                else -> super.onOptionsItemSelected(item)
            }

    companion object {
        val submitDateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm")
        const val REVIEW_TIME = 48
    }
}