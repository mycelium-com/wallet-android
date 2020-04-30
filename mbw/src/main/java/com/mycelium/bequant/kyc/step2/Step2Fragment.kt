package com.mycelium.bequant.kyc.step2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import com.mycelium.bequant.remote.model.KYCRequest
import com.mycelium.wallet.R
import com.mycelium.wallet.databinding.FragmentBequantSteps2Binding
import kotlinx.android.synthetic.main.fragment_bequant_steps_2.*

class Step2Fragment : Fragment() {
    lateinit var viewModel: Step2ViewModel
    lateinit var kycRequest: KYCRequest

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        kycRequest = arguments?.getSerializable("kycRequest") as KYCRequest
        viewModel = ViewModelProviders.of(this).get(Step2ViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            DataBindingUtil.inflate<FragmentBequantSteps2Binding>(inflater, R.layout.fragment_bequant_steps_2, container, false)
                    .apply {
                        viewModel = this@Step2Fragment.viewModel
                        lifecycleOwner = this@Step2Fragment
                    }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btNext.setOnClickListener {
            viewModel.fillModel(kycRequest)
            findNavController().navigate(Step2FragmentDirections.actionNext(kycRequest))
        }
    }
}