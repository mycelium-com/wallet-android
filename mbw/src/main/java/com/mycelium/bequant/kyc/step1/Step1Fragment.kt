package com.mycelium.bequant.kyc.step1

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import com.mycelium.wallet.R
import com.mycelium.wallet.databinding.FragmentBequantSteps1Binding
import kotlinx.android.synthetic.main.activity_bequant_steps_2.*

class Step1Fragment : Fragment() {
    lateinit var viewModel: Step1ViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(Step1ViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            DataBindingUtil.inflate<FragmentBequantSteps1Binding>(inflater, R.layout.fragment_bequant_steps_1, container, false)
                    .apply {
                        viewModel = this@Step1Fragment.viewModel
                        lifecycleOwner = this@Step1Fragment
                    }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        btNext.setOnClickListener {
            findNavController().navigate(Step1FragmentDirections.actionNext())
        }
    }
}