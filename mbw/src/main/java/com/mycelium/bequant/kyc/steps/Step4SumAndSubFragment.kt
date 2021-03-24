package com.mycelium.bequant.kyc.steps

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.mycelium.bequant.BequantPreference
import com.mycelium.bequant.common.ErrorHandler
import com.mycelium.bequant.common.loader
import com.mycelium.bequant.kyc.steps.adapter.ItemStep
import com.mycelium.bequant.kyc.steps.adapter.StepAdapter
import com.mycelium.bequant.kyc.steps.adapter.StepState
import com.mycelium.bequant.kyc.steps.viewmodel.DocumentViewModel
import com.mycelium.bequant.kyc.steps.viewmodel.HeaderViewModel
import com.mycelium.bequant.remote.model.KYCRequest
import com.mycelium.bequant.remote.repositories.Api
import com.mycelium.wallet.R
import com.mycelium.wallet.databinding.FragmentBequantSteps4SumSubBinding
import com.sumsub.sns.core.SNSMobileSDK
import com.sumsub.sns.core.data.listener.TokenExpirationHandler
import com.sumsub.sns.core.data.model.SNSCompletionResult
import com.sumsub.sns.core.data.model.SNSSDKState
import com.sumsub.sns.prooface.SNSProoface
import kotlinx.android.synthetic.main.part_bequant_step_header.*
import kotlinx.android.synthetic.main.part_bequant_stepper_body.*
import timber.log.Timber
import java.util.*

class Step4SumAndSubFragment : Fragment() {
    lateinit var headerViewModel: HeaderViewModel
    lateinit var viewModel: DocumentViewModel

    lateinit var kycRequest: KYCRequest
    val args: Step4SumAndSubFragmentArgs by navArgs()
    private var runnable: Runnable? = null // Runnable object to contain the navigation code

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        kycRequest = args.kycRequest
        viewModel = ViewModelProviders.of(this).get(DocumentViewModel::class.java)
        headerViewModel = ViewModelProviders.of(this).get(HeaderViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            DataBindingUtil.inflate<FragmentBequantSteps4SumSubBinding>(inflater, R.layout.fragment_bequant_steps_4_sum_sub, container, false)
                    .apply {
                        headerViewModel = this@Step4SumAndSubFragment.headerViewModel
                        lifecycleOwner = this@Step4SumAndSubFragment
                    }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as AppCompatActivity?)?.supportActionBar?.run {
            title = getString(R.string.identity_auth)
            setHomeAsUpIndicator(resources.getDrawable(R.drawable.ic_bequant_arrow_back))
        }
        step.text = getString(R.string.step_n, 3)
        stepProgress.progress = 3
        val stepAdapter = StepAdapter()
        stepper.adapter = stepAdapter
        stepAdapter.submitList(listOf(
                ItemStep(1, getString(R.string.personal_info), StepState.COMPLETE),
                ItemStep(2, getString(R.string.phone_number), StepState.COMPLETE),
                ItemStep(3, getString(R.string.doc_selfie), StepState.CURRENT)))

        requestSumSubToken()
    }

    override fun onResume() {
        super.onResume()
        runnable?.run()
    }

    private val tokenExpirationHandler = object : TokenExpirationHandler {
        override fun onTokenExpired(): String? {
            // Access token expired
            // get a new one and pass it to the callback to re-initiate the SDK
            val newToken = "..." // get a new token from your backend
            return newToken
        }
    }

    private val onSDKStateChangedHandler: (SNSSDKState, SNSSDKState) -> Unit = { newState, prevState ->
        Timber.d("onSDKStateChangedHandler: $prevState -> $newState")
        when (newState) {
            is SNSSDKState.Ready -> loader(false)
            is SNSSDKState.Failed -> {
                when (newState) {
                    is SNSSDKState.Failed.ApplicantNotFound -> Timber.e(newState.message)
                    is SNSSDKState.Failed.ApplicantMisconfigured -> Timber.e(newState.message)
                    is SNSSDKState.Failed.InitialLoadingFailed -> Timber.e(newState.exception, "Initial loading error")
                    is SNSSDKState.Failed.InvalidParameters -> Timber.e(newState.message)
                    is SNSSDKState.Failed.NetworkError -> Timber.e(newState.exception, newState.message)
                    is SNSSDKState.Failed.Unauthorized -> Timber.e(newState.exception, "Invalid token or a token can't be refreshed by the SDK. Please, check your token expiration handler")
                    is SNSSDKState.Failed.Unknown -> Timber.e(newState.exception, "Unknown error")
                }
            }
            is SNSSDKState.Initial -> BequantPreference.setSumSubLastState("initial")
            is SNSSDKState.Incomplete -> BequantPreference.setSumSubLastState("incomplete")
            is SNSSDKState.Pending -> BequantPreference.setSumSubLastState("pending")
            is SNSSDKState.FinallyRejected -> BequantPreference.setSumSubLastState("finally_rejected")
            is SNSSDKState.TemporarilyDeclined -> BequantPreference.setSumSubLastState("temporarily_declined")
            is SNSSDKState.Approved -> BequantPreference.setSumSubLastState("approved")
        }
    }

    private val onSDKCompletedHandler: (SNSCompletionResult, SNSSDKState) -> Unit = { result, state ->
        when (result) {
            is SNSCompletionResult.SuccessTermination -> {
                if (BequantPreference.getSumSubLastState() == "pending")
                    submitForm()
                else
                    goToNextStep()
            }

            is SNSCompletionResult.AbnormalTermination -> {
                if (BequantPreference.getSumSubLastState() == "pending")
                    submitForm()
                else
                    goToNextStep()
            }
        }
    }

    private fun goToNextStep() {
        if (!isResumed) {
            // add navigation to runnable as fragment is not resumed
            runnable = Runnable {
                BequantPreference.setKYCRequest(kycRequest)
                findNavController().navigate(Step4SumAndSubFragmentDirections.toStartFragment(), NavOptions.Builder().setPopUpTo(R.id.kycStep4SumSub, true).build())
                runnable = null

            }
        } else {
            runnable = null
            BequantPreference.setKYCRequest(kycRequest)
            findNavController().navigate(Step4SumAndSubFragmentDirections.toStartFragment(), NavOptions.Builder().setPopUpTo(R.id.kycStep4SumSub, true).build())
        }
    }

    private fun requestSumSubToken() {
        loader(true)
        Api.kycRepository.sumSubToken(viewModel.viewModelScope, success = { sumSubTokenResponse ->
            val modules = listOf(SNSProoface())

            activity?.let { activity ->
                val snsSdk = SNSMobileSDK.Builder(activity, sumSubTokenResponse.url, sumSubTokenResponse.flowName)
                        .withAccessToken(sumSubTokenResponse.token, onTokenExpiration = tokenExpirationHandler)
                        .withHandlers(onStateChanged = onSDKStateChangedHandler, onCompleted = onSDKCompletedHandler)
                        .withDebug(true)
                        .withModules(modules)
                        .withLocale(Locale.getDefault())
                        .build()

                snsSdk.launch()
            }
        })
    }

    private fun submitForm() {
        //loader(true)
        Api.kycRepository.submit(lifecycleScope, {
            if(it?.submitted == true){
                BequantPreference.setKYCSubmitDate(Date())
                BequantPreference.setKYCSubmitted(true)
                Toast.makeText(requireActivity(), "Submitted", Toast.LENGTH_LONG).show()
            }
            findNavController().navigate(Step4SumAndSubFragmentDirections.actionFinish())
        }, { code, msg ->
            ErrorHandler(requireContext()).handle(msg)
        }, {
            loader(false)
        }
        )
    }
}