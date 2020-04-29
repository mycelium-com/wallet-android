package com.mycelium.bequant.kyc.steps

import android.os.Bundle
import android.view.*
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import com.mycelium.bequant.kyc.steps.adapter.ItemStep
import com.mycelium.bequant.kyc.steps.adapter.StepAdapter
import com.mycelium.bequant.kyc.steps.adapter.StepState
import com.mycelium.bequant.kyc.steps.viewmodel.HeaderViewModel
import com.mycelium.bequant.kyc.steps.viewmodel.Step3ViewModel
import com.mycelium.bequant.remote.model.KYCRequest
import com.mycelium.wallet.R
import com.mycelium.wallet.databinding.FragmentBequantSteps3Binding
import kotlinx.android.synthetic.main.part_bequant_step_header.*
import kotlinx.android.synthetic.main.part_bequant_stepper_body.*

class Step3Fragment : Fragment() {
    lateinit var viewModel: Step3ViewModel
    lateinit var headerViewModel: HeaderViewModel
    lateinit var kycRequest: KYCRequest

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        kycRequest = arguments?.getSerializable("kycRequest") as KYCRequest
        viewModel = ViewModelProviders.of(this).get(Step3ViewModel::class.java)
        headerViewModel = ViewModelProviders.of(this).get(HeaderViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            DataBindingUtil.inflate<FragmentBequantSteps3Binding>(inflater, R.layout.fragment_bequant_steps_3, container, false)
                    .apply {
                        viewModel = this@Step3Fragment.viewModel
                        headerViewModel = this@Step3Fragment.headerViewModel
                        lifecycleOwner = this@Step3Fragment
                    }.root


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        step.text = "Step 3"
        stepProgress.progress = 3
        val stepAdapter = StepAdapter()
        stepper.adapter = stepAdapter
        stepAdapter.submitList(listOf(ItemStep(0, "Phone Number", StepState.COMPLETE)
                , ItemStep(1, "Personal information", StepState.COMPLETE_EDITABLE)
                , ItemStep(2, "Residential Address", StepState.COMPLETE_EDITABLE)
                , ItemStep(3, "Documents & Selfie", StepState.CURRENT)))

        stepAdapter.clickListener = {
            when (it) {
                1 -> findNavController().navigate(Step3FragmentDirections.actionEditStep1(kycRequest))
                2 -> findNavController().navigate(Step3FragmentDirections.actionEditStep2(kycRequest))
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_bequant_kyc_step, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
            when (item.itemId) {
                R.id.stepper -> {
                    stepperLayout.visibility = if (stepperLayout.visibility == View.VISIBLE) View.GONE else View.VISIBLE
                    true
                }
                else -> super.onOptionsItemSelected(item)
            }
}