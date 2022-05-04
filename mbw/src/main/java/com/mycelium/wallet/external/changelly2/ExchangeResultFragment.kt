package com.mycelium.wallet.external.changelly2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.mycelium.wallet.R
import com.mycelium.wallet.databinding.FragmentChangelly2ExchangeResultBinding
import com.mycelium.wallet.external.changelly2.remote.Changelly2Repository
import com.mycelium.wallet.external.changelly2.viewmodel.ExchangeResultViewModel


class ExchangeResultFragment : DialogFragment() {

    var binding: FragmentChangelly2ExchangeResultBinding? = null
    val viewModel: ExchangeResultViewModel by viewModels()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.CustomDialog);
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
            FragmentChangelly2ExchangeResultBinding.inflate(inflater).apply {
                binding = this
                viewModel
            }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val txId = arguments?.getString(KEY_TX_ID)
        Changelly2Repository.getTransaction(lifecycleScope, txId!!,
                {
                    if(it?.result != null) {
                        viewModel.setTransaction(it.result!!)
                    }
                },
                { _, _ ->

                },
                {

                })
    }

    companion object {
        const val KEY_TX_ID = "tx_id"
    }
}