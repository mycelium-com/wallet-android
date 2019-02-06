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
import com.mycelium.wallet.databinding.AddressFragmentBinding
import com.mycelium.wallet.databinding.AddressFragmentBtcBinding
import com.mycelium.wapi.wallet.bch.bip44.Bip44BCHAccount
import com.mycelium.wapi.wallet.bch.single.SingleAddressBCHAccount
import com.mycelium.wapi.wallet.btc.AbstractBtcAccount
import kotlinx.android.synthetic.main.address_fragment_label.*
import kotlinx.android.synthetic.main.address_fragment_qr.*

class AddressFragment : Fragment() {
    private val mbwManager = MbwManager.getInstance(activity)
    private lateinit var viewModel: AddressFragmentViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        setHasOptionsMenu(true)
        val viewModelProvider = ViewModelProviders.of(this)
        val account = mbwManager.selectedAccount
        this.viewModel = viewModelProvider.get(when (account) {
            is SingleAddressBCHAccount,
            is Bip44BCHAccount -> AddressFragmentCoinsModel::class.java
            is AbstractBtcAccount -> {
                if (account.availableAddressTypes.size > 1) {
                    AddressFragmentBtcModel::class.java
                } else {
                    AddressFragmentCoinsModel::class.java
                }
            }
            else -> AddressFragmentCoinsModel::class.java
        })
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding =
                when (mbwManager.selectedAccount) {
                    is Bip44BCHAccount, is SingleAddressBCHAccount -> {
                        createDefaultBinding(inflater, container)
                    }
                    is AbstractBtcAccount -> {
                        if ((mbwManager.selectedAccount as AbstractBtcAccount).availableAddressTypes.size > 1) {
                            val contentView = DataBindingUtil.inflate<AddressFragmentBtcBinding>(inflater, R.layout.address_fragment_btc,
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

    private fun createDefaultBinding(inflater: LayoutInflater, container: ViewGroup?): AddressFragmentBinding {
        val contentView = DataBindingUtil.inflate<AddressFragmentBinding>(inflater, R.layout.address_fragment,
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
