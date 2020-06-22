package com.mycelium.bequant.kyc.steps

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import com.mycelium.wallet.activity.news.NewsImageActivity
import com.mycelium.wallet.databinding.FragmentBequantSteps3Binding
import kotlinx.android.synthetic.main.fragment_bequant_steps_3.*
import kotlinx.android.synthetic.main.part_bequant_step_header.*
import kotlinx.android.synthetic.main.part_bequant_stepper_body.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


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
        step.text = getString(R.string.step_n, 3)
        stepProgress.progress = 3
        val stepAdapter = StepAdapter()
        stepper.adapter = stepAdapter
        stepAdapter.submitList(listOf(ItemStep(0, getString(R.string.phone_number), StepState.COMPLETE)
                , ItemStep(1, getString(R.string.personal_info), StepState.COMPLETE_EDITABLE)
                , ItemStep(2, getString(R.string.residential_address), StepState.COMPLETE_EDITABLE)
                , ItemStep(3, getString(R.string.doc_selfie), StepState.CURRENT)))

        stepAdapter.clickListener = {
            when (it) {
                1 -> findNavController().navigate(Step3FragmentDirections.actionEditStep1().setKycRequest(kycRequest))
                2 -> findNavController().navigate(Step3FragmentDirections.actionEditStep2(kycRequest))
            }
        }
        identityList.adapter = identityAdapter
        identityAdapter.submitList(kycRequest.identityList.map {
            Document(BitmapFactory.decodeFile(it), "Doc" + (++counter).toString(), it)
        })
        identityAdapter.removeListner = {
            kycRequest.identityList.remove(it.name)
        }
        identityAdapter.viewListener = {
            startActivity(Intent(requireContext(), NewsImageActivity::class.java)
                    .putExtra("url", it.url))
        }
        proofAddressList.adapter = proofAddressAdapter
        proofAddressAdapter.submitList(kycRequest.poaList.map {
            Document(BitmapFactory.decodeFile(it), "Doc" + (++counter).toString(), it)
        })
        proofAddressAdapter.removeListner = {
            kycRequest.poaList.remove(it.name)
        }
        proofAddressAdapter.viewListener = {
            startActivity(Intent(requireContext(), NewsImageActivity::class.java)
                    .putExtra("url", it.url))
        }
        selfieList.adapter = selfieAdapter
        selfieAdapter.submitList(kycRequest.selfieList.map {
            Document(BitmapFactory.decodeFile(it), "Doc" + (++counter).toString(), it)
        })
        selfieAdapter.removeListner = {
            kycRequest.selfieList.remove(it.name)
        }
        selfieAdapter.viewListener = {
            startActivity(Intent(requireContext(), NewsImageActivity::class.java)
                    .putExtra("url", it.url))
        }
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
            if (requestCode == REQUEST_CODE_INDENTITY || requestCode % 10 == REQUEST_CODE_INDENTITY % 10) {
                uploadImage(data, identityAdapter, KYCDocument.PASSPORT, kycRequest.identityList)
            }

            if (requestCode == REQUEST_CODE_PROOF_ADDRESS || requestCode % 10 == REQUEST_CODE_PROOF_ADDRESS % 10) {
                uploadImage(data, proofAddressAdapter, KYCDocument.POA, kycRequest.poaList)
            }

            if (requestCode == REQUEST_CODE_SELFIE || requestCode % 10 == REQUEST_CODE_SELFIE % 10) {
                uploadImage(data, selfieAdapter, KYCDocument.SELFIE, kycRequest.selfieList)
            }
        }
    }

    private fun uploadImage(data: Intent?, adapter: DocumentAdapter, docType: KYCDocument, requestList: MutableList<String>) {
        val outputFile = if (data?.data != null) getFileFromGallery(data) else DocumentAttachDialog.currentPhotoFile

        val item = Document(BitmapFactory.decodeFile(outputFile?.absolutePath), "Doc" + (++counter).toString(), outputFile?.absolutePath)
        adapter.submitList(adapter.currentList + item)
        KYCRepository.repository.uploadDocument(viewModel.viewModelScope, docType,
                outputFile!!, { uploaded, total ->
            item.size = total
            item.progress = (uploaded * 100 / total).toInt()
            adapter.notifyItemChanged(adapter.currentList.indexOf(item))
        }, {
            requestList.add(outputFile.absolutePath)
        })
    }

    private fun getFileFromGallery(data: Intent): File =
            File.createTempFile(SimpleDateFormat("yyyyMMdd_HHmmss").format(Date()),
                    ".jpg", requireContext().cacheDir).apply {
                MediaStore.Images.Media.getBitmap(requireActivity().contentResolver, data.data)
                        .compress(Bitmap.CompressFormat.JPEG, 100, this.outputStream())
            }

    companion object {
        const val REQUEST_CODE_INDENTITY = 1001
        const val REQUEST_CODE_PROOF_ADDRESS = 1002
        const val REQUEST_CODE_SELFIE = 1003
    }
}