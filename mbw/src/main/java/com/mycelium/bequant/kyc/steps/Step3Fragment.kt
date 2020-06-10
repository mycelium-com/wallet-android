package com.mycelium.bequant.kyc.steps

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.*
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.mycelium.bequant.kyc.steps.adapter.*
import com.mycelium.bequant.kyc.steps.viewmodel.HeaderViewModel
import com.mycelium.bequant.kyc.steps.viewmodel.Step3ViewModel
import com.mycelium.bequant.remote.KYCRepository
import com.mycelium.bequant.remote.model.KYCDocument
import com.mycelium.bequant.remote.model.KYCRequest
import com.mycelium.wallet.R
import com.mycelium.wallet.databinding.FragmentBequantSteps3Binding
import kotlinx.android.synthetic.main.fragment_bequant_steps_3.*
import kotlinx.android.synthetic.main.part_bequant_step_header.*
import kotlinx.android.synthetic.main.part_bequant_stepper_body.*


class Step3Fragment : Fragment() {
    lateinit var viewModel: Step3ViewModel
    lateinit var headerViewModel: HeaderViewModel
    lateinit var kycRequest: KYCRequest

    val args: Step3FragmentArgs by navArgs()

    val identityAdapter = DocumentAdapter()
    val proofAddressAdapter = DocumentAdapter()
    val selfieAdapter = DocumentAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        kycRequest = args.kycRequest
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
                1 -> findNavController().navigate(Step3FragmentDirections.actionEditStep1().setKycRequest(kycRequest))
                2 -> findNavController().navigate(Step3FragmentDirections.actionEditStep2(kycRequest))
            }
        }
        identityList.adapter = identityAdapter
        proofAddressList.adapter = proofAddressAdapter
        selfieList.adapter = selfieAdapter
        addIndentity.setOnClickListener {
            DocumentAttachDialog().apply {
                setTargetFragment(this@Step3Fragment, REQUEST_CODE_INDENTITY)
            }.show(parentFragmentManager, "upload_document")
        }
        addProofAddress.setOnClickListener {
            DocumentAttachDialog().apply {
                setTargetFragment(this@Step3Fragment, REQUEST_CODE_PROOF_ADDRESS)
            }.show(parentFragmentManager, "upload_document")
        }
        addSelfie.setOnClickListener {
            DocumentAttachDialog().apply {
                setTargetFragment(this@Step3Fragment, REQUEST_CODE_SELFIE)
            }.show(parentFragmentManager, "upload_document")
        }

        btFinish.setOnClickListener {
            findNavController().navigate(Step3FragmentDirections.actionNext(kycRequest))
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

    var counter: Int = 0
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
//                && requestCode / 1000 == DocumentAttachDialog.REQURST_CODE_CAMERA / 1000) {
            if (requestCode == REQUEST_CODE_INDENTITY || requestCode % 10 == REQUEST_CODE_INDENTITY % 10) {
                if (data != null && data.extras != null) {
                    val imageBitmap = getBitmap(data)
                    identityAdapter.submitList(
                            identityAdapter.currentList + Document(imageBitmap, "Doc" + (++counter).toString())
                    )
                    KYCRepository.repository.uploadDocument(viewModel.viewModelScope, KYCDocument.PASSPORT, data.data?.path!!) {

                    }
                }
            }

            if (requestCode == REQUEST_CODE_PROOF_ADDRESS || requestCode % 10 == REQUEST_CODE_PROOF_ADDRESS % 10) {
                if (data != null && data.extras != null) {
                    val imageBitmap = getBitmap(data)
                    proofAddressAdapter.submitList(
                            proofAddressAdapter.currentList + Document(imageBitmap, "Doc" + (++counter).toString())
                    )
                    KYCRepository.repository.uploadDocument(viewModel.viewModelScope, KYCDocument.POA, data.data?.path!!) {

                    }
                }
            }

            if (requestCode == REQUEST_CODE_SELFIE || requestCode % 10 == REQUEST_CODE_SELFIE % 10) {
                if (data != null && data.extras != null) {
                    val imageBitmap = getBitmap(data)
                    selfieAdapter.submitList(
                            selfieAdapter.currentList + Document(imageBitmap, "Photo" + (++counter).toString())
                    )
                    KYCRepository.repository.uploadDocument(viewModel.viewModelScope, KYCDocument.SELFIE, data.data?.path!!) {

                    }
                }
            }
        }
    }

    private fun getBitmap(data: Intent) =
            if (data?.data is Uri) MediaStore.Images.Media.getBitmap(requireActivity().contentResolver, data?.data) else data.extras["data"] as Bitmap

    companion object {
        const val REQUEST_CODE_INDENTITY = 1001
        const val REQUEST_CODE_PROOF_ADDRESS = 1002
        const val REQUEST_CODE_SELFIE = 1003
    }
}