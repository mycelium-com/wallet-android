package com.mycelium.bequant.kyc.checkCode

import android.os.Bundle
import android.os.CountDownTimer
import android.view.*
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.mycelium.bequant.BequantPreference
import com.mycelium.bequant.common.ErrorHandler
import com.mycelium.bequant.common.loader
import com.mycelium.bequant.kyc.steps.Step1FragmentDirections
import com.mycelium.bequant.kyc.steps.Step3FragmentDirections
import com.mycelium.bequant.kyc.steps.adapter.ItemStep
import com.mycelium.bequant.kyc.steps.adapter.StepAdapter
import com.mycelium.bequant.kyc.steps.adapter.StepState
import com.mycelium.bequant.kyc.steps.viewmodel.HeaderViewModel
import com.mycelium.bequant.remote.model.KYCRequest
import com.mycelium.bequant.remote.repositories.Api
import com.mycelium.wallet.R
import com.mycelium.wallet.databinding.FragmentBequantKycVerifyPhoneBinding
import com.poovam.pinedittextfield.PinField
import java.util.concurrent.TimeUnit

class VerifyPhoneFragment : Fragment() {

    val headerViewModel: HeaderViewModel by viewModels()
    val viewModel: VerifyPhoneViewModel by viewModels()
    lateinit var kycRequest: KYCRequest

    val args: VerifyPhoneFragmentArgs by navArgs()
    var binding: FragmentBequantKycVerifyPhoneBinding? = null

    private var resendTimer = object : CountDownTimer(TimeUnit.MINUTES.toMillis(1), 1000) {
        override fun onTick(leftTime: Long) {
            binding?.resendTime?.text = "${TimeUnit.MILLISECONDS.toMinutes(leftTime)}:${TimeUnit.MILLISECONDS.toSeconds(leftTime % 60000)}"
        }

        override fun onFinish() {
            binding?.resendTimerLayout?.visibility = GONE
            binding?.tryAgainLayout?.visibility = VISIBLE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        kycRequest = args.kycRequest ?: KYCRequest()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            FragmentBequantKycVerifyPhoneBinding.inflate(inflater, container, false)
                    .apply {
                        binding = this
                        viewModel = this@VerifyPhoneFragment.viewModel
                        headerViewModel = this@VerifyPhoneFragment.headerViewModel
                        lifecycleOwner = this@VerifyPhoneFragment
                    }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as AppCompatActivity?)?.supportActionBar?.run {
            title = getString(R.string.identity_auth)
            setHomeAsUpIndicator(resources.getDrawable(R.drawable.ic_bequant_arrow_back))
        }
        binding?.stepHeader?.step?.text = getString(R.string.step_n, 3)
        binding?.stepHeader?.stepProgress?.progress = 3
        val stepAdapter = StepAdapter()
        binding?.body?.stepper?.adapter = stepAdapter
        stepAdapter.submitList(listOf(
                ItemStep(1, getString(R.string.personal_info), StepState.COMPLETE_EDITABLE)
                , ItemStep(2, getString(R.string.residential_address), StepState.COMPLETE_EDITABLE)
                , ItemStep(3, getString(R.string.phone_number), StepState.CURRENT)
                , ItemStep(4, getString(R.string.doc_selfie), StepState.FUTURE)))

        stepAdapter.clickListener = {
            when (it) {
                1 -> findNavController().navigate(Step3FragmentDirections.actionEditStep1(kycRequest))
                2 -> findNavController().navigate(Step3FragmentDirections.actionEditStep2(kycRequest))
            }
        }
        binding?.pinCode?.onTextCompleteListener = object : PinField.OnTextCompleteListener {
            override fun onTextComplete(enteredText: String): Boolean {
                checkCode(enteredText)
                return true
            }
        }
        binding?.tvTryAgain?.setOnClickListener {
            resendCode()
        }
        startTryAgainCountDown()
    }

    private fun startTryAgainCountDown() {
        binding?.resendTimerLayout?.visibility = VISIBLE
        binding?.tryAgainLayout?.visibility = GONE
        resendTimer.start()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_bequant_kyc_step, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
            when (item.itemId) {
                android.R.id.home -> {
                    activity?.onBackPressed()
                    true
                }
                R.id.stepper -> {
                    item.icon = resources.getDrawable(if (binding?.body?.stepperLayout?.visibility == VISIBLE) R.drawable.ic_chevron_down else R.drawable.ic_chevron_up)
                    binding?.body?.stepperLayout?.visibility = if (binding?.body?.stepperLayout?.visibility == VISIBLE) GONE else VISIBLE
                    true
                }
                else -> super.onOptionsItemSelected(item)
            }

    private fun resendCode() {
        loader(true)
        Api.kycRepository.mobileVerification(viewModel.viewModelScope, {
            startTryAgainCountDown()
            AlertDialog.Builder(requireContext())
                    .setMessage(it)
                    .setPositiveButton(R.string.button_ok) { _, _ ->
                    }.show()
        }, { code, error ->
            ErrorHandler(requireContext()).handle(error)
        }, {
            loader(false)
        })
    }

    private fun checkCode(code: String) {
        loader(true)
        Api.kycRepository.checkMobileVerification(viewModel.viewModelScope, code, {
            loader(false)
            nextPage()
        }, {
            loader(false)
            showError()
        })
    }

    private fun showError() {
        binding?.otpView?.error = "Error code"
    }

    private fun nextPage() {
        when {
            BequantPreference.getKYCSectionStatus("documents") -> {
                findNavController().navigate(Step1FragmentDirections.actionEditStep4(BequantPreference.getKYCRequest()))
            }
            else -> {
                findNavController().navigate(Step1FragmentDirections.actionPending())
            }
        }
    }
}