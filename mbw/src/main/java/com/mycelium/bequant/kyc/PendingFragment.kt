package com.mycelium.bequant.kyc

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.mycelium.bequant.BequantPreference
import com.mycelium.bequant.kyc.steps.adapter.ItemStep
import com.mycelium.bequant.kyc.steps.adapter.StepAdapter
import com.mycelium.bequant.kyc.steps.adapter.StepState
import com.mycelium.wallet.R
import kotlinx.android.synthetic.main.fragment_bequant_kyc_final_pending.*
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
        stepAdapter.submitList(listOf(
                ItemStep(1, getString(R.string.personal_info), StepState.COMPLETE)
                , ItemStep(2, getString(R.string.residential_address), StepState.COMPLETE)
                , ItemStep(3, getString(R.string.phone_number), StepState.COMPLETE)
                , ItemStep(4, getString(R.string.doc_selfie), StepState.COMPLETE_EDITABLE)))
        stepAdapter.clickListener = {
            when (it) {
                4 -> findNavController().navigate(PendingFragmentDirections.actionEditDocs(BequantPreference.getKYCRequest()))
            }
        }
        val submitDate = BequantPreference.getKYCSubmitDate()
        subtitle1.text = getString(R.string.bequant_application_submit_time,
                submitDateFormat.format(submitDate))
        subtitle2.text = getString(R.string.bequant_application_submit_time_left,
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