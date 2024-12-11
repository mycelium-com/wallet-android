package com.mycelium.wallet.activity.main.address

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.WalletApplication
import com.mycelium.wallet.activity.modern.Toaster
import com.mycelium.wallet.activity.util.QrImageView
import com.mycelium.wallet.databinding.AddressFragmentBinding
import com.mycelium.wallet.databinding.AddressFragmentBtcBinding
import com.mycelium.wallet.databinding.AddressFragmentFionameBinding
import com.mycelium.wallet.databinding.AddressFragmentLabelBinding

class AddressFragment : Fragment() {
    private val mbwManager = MbwManager.getInstance(WalletApplication.getInstance())
    val viewModel: AddressFragmentViewModel by viewModels { AddressFragmentViewModel.Factory }
    private var bindingLabel: AddressFragmentLabelBinding? = null
    private var bindingFioName: AddressFragmentFionameBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        setHasOptionsMenu(true)
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding =
                if (AddressFragmentViewModel.accountSupportsMultipleBtcReceiveAddresses(mbwManager.selectedAccount)) {
                    AddressFragmentBtcBinding.inflate(inflater, container, false).also {
                        bindingLabel = it.layoutLabel
                        it.activity = activity
                        it.viewModel = viewModel as AddressFragmentBtcModel
                    }
                } else {
                    AddressFragmentBinding.inflate(inflater, container, false).also {
                        bindingLabel = it.layoutLabel
                        bindingFioName = it.layoutFioName
                        it.activity = activity
                        it.viewModel = viewModel as AddressFragmentCoinsModel
                    }
                }
        binding.lifecycleOwner = this
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!viewModel.isInitialized()) {
            viewModel.init()
        }
        val ivQR = view.findViewById<QrImageView>(R.id.ivQR)
        ivQR.tapToCycleBrightness = false
        ivQR.qrCode = viewModel.getAddressString()

        val drawableForAccount = viewModel.getDrawableForAccount(resources)
        if (drawableForAccount != null) {
            bindingLabel?.ivAccountType?.setImageDrawable(drawableForAccount)
        }
        viewModel.getAccountAddress().observe(viewLifecycleOwner, Observer { newAddress ->
            if (newAddress != null) {
                ivQR.qrCode = viewModel.getAddressString()
            }
        })

        val preference = requireContext().getSharedPreferences("fio_balance_screen_preference", Context.MODE_PRIVATE)
        viewModel.getRegisteredFIONames().observe(viewLifecycleOwner, Observer { names ->
            val spinnerAdapter: ArrayAdapter<String> = ArrayAdapter<String>(requireContext(),
                    R.layout.layout_address_fragment_fio_names, R.id.text, names.map { it.name }).apply {
                setDropDownViewResource(R.layout.layout_address_fragment_fio_names_dropdown)
            }
            bindingFioName?.fioNamesSpinner?.adapter = spinnerAdapter
            if (names.isNotEmpty()) {
                val selectedName = preference.getString("selectedFioNameFor${mbwManager.selectedAccount.label}", names.first().name)
                bindingFioName?.fioNamesSpinner?.setSelection(spinnerAdapter.getPosition(selectedName))
            }
            bindingFioName?.fioNamesSpinner?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(p0: AdapterView<*>?) {}
                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                    bindingFioName?.tvBundledTxsNum?.text = "Bundled transactions: ${names[p2].bundledTxsNum}"
                    preference.edit().putString("selectedFioNameFor${mbwManager.selectedAccount.label}", names[p2].name).apply()
                }
            }
        })
        bindingLabel?.syncStatus?.setOnClickListener {
            Toaster(requireContext()).toastSyncFailed(viewModel.getAccount().lastSyncStatus())
        }
    }
}
