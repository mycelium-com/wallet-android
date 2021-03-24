package com.mycelium.bequant.kyc

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.mycelium.bequant.BequantPreference
import com.mycelium.bequant.common.ErrorHandler
import com.mycelium.bequant.common.loader
import com.mycelium.bequant.kyc.steps.Step4SumAndSubFragmentDirections
import com.mycelium.bequant.kyc.steps.adapter.ItemStep
import com.mycelium.bequant.kyc.steps.adapter.StepAdapter
import com.mycelium.bequant.kyc.steps.adapter.StepState
import com.mycelium.bequant.remote.repositories.Api
import com.mycelium.wallet.R
import kotlinx.android.synthetic.main.fragment_bequant_kyc_final_pending.*
import kotlinx.android.synthetic.main.fragment_bequant_kyc_final_pending.stepper
import kotlinx.android.synthetic.main.fragment_bequant_kyc_final_pending.subtitle1
import kotlinx.android.synthetic.main.fragment_bequant_kyc_final_pending.subtitle2
import kotlinx.android.synthetic.main.fragment_bequant_kyc_final_pending.title
import kotlinx.android.synthetic.main.fragment_bequant_kyc_final_presubmit.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit


class PendingFragment : Fragment(R.layout.fragment_bequant_kyc_final_pending) {

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
        setupUI()
    }

    private fun isStep1Completed()  = BequantPreference.getKYCSectionStatus("personal_information")
    private fun isStep2Completed() = BequantPreference.getKYCSectionStatus("phone") && isStep1Completed()
    private fun isStep3Completed() = isStep2Completed() && (BequantPreference.getSumSubLastState() != "initial" && BequantPreference.getSumSubLastState() != "incomplete")

    private fun setupUI(){
        val step1State = if (isStep1Completed())
            StepState.COMPLETE
        else
            StepState.COMPLETE_EDITABLE

        val step2State = if (isStep2Completed())
            StepState.COMPLETE
        else
            StepState.COMPLETE_EDITABLE

        val step3State = if (!isStep2Completed())
            StepState.FUTURE
        else if (isStep3Completed()){
            StepState.COMPLETE
        } else {
            StepState.COMPLETE_EDITABLE
        }

        stepAdapter.submitList(listOf(
                ItemStep(1, getString(R.string.personal_info), step1State),
                ItemStep(2, getString(R.string.phone_number), step2State),
                ItemStep(3, getString(R.string.doc_selfie), step3State)))
        stepAdapter.clickListener = {
            when (it) {
                1 -> findNavController().navigate(PendingFragmentDirections.actionEmailVerification(BequantPreference.getKYCRequest()))
                2 -> findNavController().navigate(PendingFragmentDirections.actionPhoneVerification(BequantPreference.getKYCRequest()))
                3 -> findNavController().navigate(PendingFragmentDirections.actionEditDocs(BequantPreference.getKYCRequest()))
            }
        }

        if(BequantPreference.getKYCSubmitted()){
            val submitDate = BequantPreference.getKYCSubmitDate()
            subtitle1.text = getString(R.string.bequant_application_submit_time,
                    submitDateFormat.format(submitDate))
            subtitle3.text = getString(R.string.you_cant_change_info)
            subtitle2.text = getString(R.string.bequant_application_submit_time_left,
                    REVIEW_TIME - (TimeUnit.MILLISECONDS.toHours(Date().time - submitDate.time)))
            updateInfo.visibility = View.GONE
            subtitle1.visibility = View.VISIBLE
            subtitle2.visibility = View.VISIBLE
            subtitle3.visibility = View.VISIBLE
        } else {
            if(isStep1Completed() && isStep2Completed() && isStep3Completed()){
                title.text = getString(R.string.thank_you_for_completing_the_application_form)
                subtitle1.text = getString(R.string.if_you_are_happy_with_the_content_please_submit_your_application_by_pressing_the_submit_button)
                subtitle2.text = getString(R.string.we_will_notify_you_of_the_outcome_of_the_application_as_soon_as_we_review_your_information_thank_you_for_your_patience)
                subtitle3.visibility = View.GONE
                updateInfo.text = getString(R.string.submit)
                updateInfo.setOnClickListener {
                    submitForm()
                }
            } else {
                subtitle1.visibility = View.GONE
                subtitle2.visibility = View.GONE
                subtitle3.visibility = View.GONE
                updateInfo.visibility = View.VISIBLE
                updateInfo.text = getString(R.string.update_info)
                updateInfo.setOnClickListener {
                    when {
                        step1State == StepState.COMPLETE_EDITABLE -> findNavController().navigate(PendingFragmentDirections.actionEmailVerification(BequantPreference.getKYCRequest()))
                        step2State == StepState.COMPLETE_EDITABLE -> findNavController().navigate(PendingFragmentDirections.actionPhoneVerification(BequantPreference.getKYCRequest()))
                        step3State == StepState.COMPLETE_EDITABLE -> findNavController().navigate(PendingFragmentDirections.actionEditDocs(BequantPreference.getKYCRequest()))
                    }
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

    private fun submitForm() {
        loader(true)
        Api.kycRepository.submit(lifecycleScope, {
            if(it?.submitted == true){
                BequantPreference.setKYCSubmitDate(Date())
                BequantPreference.setKYCSubmitted(true)
                setupUI()
            }
        }, { code, msg ->
            ErrorHandler(requireContext()).handle(msg)
        }, {
            loader(false)
        }
        )
    }

    companion object {
        val submitDateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm")
        const val REVIEW_TIME = 48
    }
}