package com.mycelium.wallet.activity.main.address

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.WalletApplication
import com.mycelium.wallet.activity.modern.Toaster
import com.mycelium.wallet.databinding.AddressFragmentBinding
import com.mycelium.wallet.databinding.AddressFragmentBtcBinding
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.btc.AbstractBtcAccount
import com.mycelium.wapi.wallet.btcvault.AbstractBtcvAccount
import kotlinx.android.synthetic.main.address_fragment_fioname.*
import kotlinx.android.synthetic.main.address_fragment_label.*
import kotlinx.android.synthetic.main.address_fragment_qr.*

class AddressFragment : Fragment() {
    private val mbwManager = MbwManager.getInstance(WalletApplication.getInstance())
    private lateinit var viewModel: AddressFragmentViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        setHasOptionsMenu(true)
        val viewModelProvider = ViewModelProviders.of(this)
        this.viewModel = viewModelProvider.get(
                if (accountSupportsMultipleBtcReceiveAddresses(mbwManager.selectedAccount)) {
                    AddressFragmentBtcModel::class.java
                } else {
                    AddressFragmentCoinsModel::class.java
                })
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding =
                if (accountSupportsMultipleBtcReceiveAddresses(mbwManager.selectedAccount)) {
                    DataBindingUtil.inflate<AddressFragmentBtcBinding>(inflater, R.layout.address_fragment_btc,
                            container, false).also {
                        it.activity = activity
                        it.viewModel = viewModel as AddressFragmentBtcModel
                    }
                } else {
                    DataBindingUtil.inflate<AddressFragmentBinding>(inflater, R.layout.address_fragment,
                            container, false).also {
                        it.activity = activity
                        it.viewModel = viewModel as AddressFragmentCoinsModel
                    }
                }
        binding.lifecycleOwner = this
        return binding.root
    }

    private fun accountSupportsMultipleBtcReceiveAddresses(account: WalletAccount<*>): Boolean =
            (account is AbstractBtcAccount && account.availableAddressTypes.size > 1)
                    || (account is AbstractBtcvAccount && account.availableAddressTypes.size > 1)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!viewModel.isInitialized()) {
            viewModel.init()
        }

        ivQR.tapToCycleBrightness = false
        ivQR.qrCode = viewModel.getAddressString()

        val drawableForAccount = viewModel.getDrawableForAccount(resources)
        if (drawableForAccount != null) {
            ivAccountType.setImageDrawable(drawableForAccount)
        }
        viewModel.getAccountAddress().observe(viewLifecycleOwner, Observer { newAddress ->
            if (newAddress != null) {
                ivQR.qrCode = viewModel.getAddressString()
            }
        })

        val preference = requireContext().getSharedPreferences("fio_balance_screen_preference", Context.MODE_PRIVATE)
        viewModel.getRegisteredFIONames().observe(this, Observer { names ->
            val spinnerAdapter: ArrayAdapter<String> = ArrayAdapter<String>(requireContext(),
                    R.layout.layout_address_fragment_fio_names, R.id.text, names.map { it.name }).apply {
                setDropDownViewResource(R.layout.layout_address_fragment_fio_names_dropdown)
            }
            fioNamesSpinner?.adapter = spinnerAdapter
            if (names.isNotEmpty()) {
                val selectedName = preference.getString("selectedFioNameFor${mbwManager.selectedAccount.label}", names.first().name)
                fioNamesSpinner?.setSelection(spinnerAdapter.getPosition(selectedName))
            }
            fioNamesSpinner?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(p0: AdapterView<*>?) {}
                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                    tvBundledTxsNum.text = "Bundled transactions: ${names[p2].bundledTxsNum}"
                    preference.edit().putString("selectedFioNameFor${mbwManager.selectedAccount.label}", names[p2].name).apply()
                }
            }
        })
        syncStatus.setOnClickListener {
            Toaster(requireContext()).toastSyncFailed(viewModel.getAccount().lastSyncStatus())
        }
    }
}
