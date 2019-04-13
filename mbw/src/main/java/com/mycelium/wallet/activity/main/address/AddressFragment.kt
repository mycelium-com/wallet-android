package com.mycelium.wallet.activity.main.address


import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.databinding.AddressFragmentBindingImpl
import com.mycelium.wallet.databinding.AddressFragmentBtcBindingImpl
import com.mycelium.wapi.wallet.AbstractAccount
import com.mycelium.wapi.wallet.bip44.Bip44BCHAccount
import com.mycelium.wapi.wallet.single.SingleAddressBCHAccount
import kotlinx.android.synthetic.main.address_fragment_label.*
import kotlinx.android.synthetic.main.address_fragment_qr.*

class AddressFragment : Fragment() {
    private val mbwManager by lazy {  MbwManager.getInstance(context) }
    private lateinit var viewModel: AddressFragmentViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        setHasOptionsMenu(true)
        val viewModelProvider = ViewModelProviders.of(this)
        val account = mbwManager.selectedAccount
        this.viewModel = when (account) {
            is SingleAddressBCHAccount, is Bip44BCHAccount -> viewModelProvider.get(AddressFragmentCoinsModel::class.java)
            is AbstractAccount -> {
                // HACK:
                if (account.availableAddressTypes.size > 1) {
                    viewModelProvider.get(AddressFragmentBtcModel::class.java)
                } else {
                    viewModelProvider.get(AddressFragmentCoinsModel::class.java)
                }
            }
            else -> viewModelProvider.get(AddressFragmentCoinsModel::class.java)
        }
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding =
                when (mbwManager.selectedAccount) {
                    is Bip44BCHAccount, is SingleAddressBCHAccount -> {
                        createDefaultBinding(inflater, container)
                    }
                    is AbstractAccount -> {
                        if ((mbwManager.selectedAccount as AbstractAccount).availableAddressTypes.size > 1) {
                            val contentView = DataBindingUtil.inflate<AddressFragmentBtcBindingImpl>(inflater, R.layout.address_fragment_btc,
                                    container, false)
                            contentView.activity = activity
                            contentView.viewModel = viewModel as AddressFragmentBtcModel
                            contentView
                        } else {
                            createDefaultBinding(inflater, container)
                        }
                    }
                    else -> {
                        createDefaultBinding(inflater, container)
                    }
                }
        binding.setLifecycleOwner(this)
        return binding.root
    }

    private fun createDefaultBinding(inflater: LayoutInflater, container: ViewGroup?): AddressFragmentBindingImpl {
        val contentView = DataBindingUtil.inflate<AddressFragmentBindingImpl>(inflater, R.layout.address_fragment,
                container, false)
        contentView.activity = activity
        contentView.viewModel = viewModel as AddressFragmentCoinsModel
        return contentView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!viewModel.isInitialized()) {
            viewModel.init()
        }

        ivQR.tapToCycleBrightness = false
        ivQR.qrCode = viewModel.getAccountAddress().value.toString()

        val drawableForAccount = viewModel.getDrawableForAccount(resources)
        if (drawableForAccount != null) {
            ivAccountType.setImageDrawable(drawableForAccount)
        }
        viewModel.getAccountAddress().observe(this, Observer { newAddress ->
            if (newAddress != null) {
                ivQR.qrCode = newAddress.toString()
            }
        })
    }
}
