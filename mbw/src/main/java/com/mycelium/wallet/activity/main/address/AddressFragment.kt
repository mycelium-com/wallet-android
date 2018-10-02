package com.mycelium.wallet.activity.main.address


import android.arch.lifecycle.ViewModelProviders
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import com.mycelium.wallet.colu.ColuAccount
import com.mycelium.wallet.databinding.AddressFragmentBindingImpl
import com.mycelium.wallet.databinding.AddressFragmentBtcBindingImpl
import com.mycelium.wallet.event.AccountChanged
import com.mycelium.wallet.event.BalanceChanged
import com.mycelium.wallet.event.ReceivingAddressChanged
import com.mycelium.wapi.wallet.bip44.Bip44BCHAccount
import com.mycelium.wapi.wallet.bip44.HDAccount
import com.mycelium.wapi.wallet.single.SingleAddressAccount
import com.mycelium.wapi.wallet.single.SingleAddressBCHAccount
import com.squareup.otto.Bus
import com.squareup.otto.Subscribe
import kotlinx.android.synthetic.main.address_fragment_qr.*
import kotlinx.android.synthetic.main.address_fragment_addr.*
import kotlinx.android.synthetic.main.address_fragment_label.*

class AddressFragment : Fragment() {
    private var mbwManager = MbwManager.getInstance(activity)
    private lateinit var viewModel: AddressFragmentViewModel

    private val eventBus: Bus
        get() = mbwManager!!.eventBus

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding =
                when (mbwManager.selectedAccount) {
                    is Bip44BCHAccount, is SingleAddressBCHAccount -> {
                        val contentView = DataBindingUtil.inflate<AddressFragmentBindingImpl>(inflater, R.layout.address_fragment,
                                container, false)
                        contentView.fragment = this
                        contentView.viewModel = viewModel as AddressFragmentCoinsModel
                        contentView
                    }

                    is SingleAddressAccount, is HDAccount -> {
                        val contentView = DataBindingUtil.inflate<AddressFragmentBtcBindingImpl>(inflater, R.layout.address_fragment_btc,
                                container, false)
                        contentView.fragment = this
                        contentView.viewModel = viewModel as AddressFragmentBtcModel
                        contentView
                    }

                    else -> {
                        val contentView = DataBindingUtil.inflate<AddressFragmentBindingImpl>(inflater, R.layout.address_fragment,
                                container, false)
                        contentView.fragment = this
                        contentView.viewModel = viewModel as AddressFragmentCoinsModel
                        contentView
                    }
                }
        binding.setLifecycleOwner(this)
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setHasOptionsMenu(true)
        val viewModelProvider = ViewModelProviders.of(this)
        val account = mbwManager.selectedAccount
        this.viewModel = when (account) {
            is SingleAddressBCHAccount, is Bip44BCHAccount -> viewModelProvider.get(AddressFragmentCoinsModel::class.java)
            is SingleAddressAccount, is HDAccount -> viewModelProvider.get(AddressFragmentBtcModel::class.java)
            else -> viewModelProvider.get(AddressFragmentCoinsModel::class.java)
        }
        super.onCreate(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!viewModel.isInitialized()) {
            viewModel.init()
        }
        ivQR.visibility = View.VISIBLE
        ivQR.tapToCycleBrightness = false
        ivQR.setQrCode(viewModel.getAccountAddress().value)
        ivAccountType.setImageDrawable(viewModel.getDrawableForAccount(resources))
        copyTo.setOnClickListener {
            addressClick()
        }
        ivQR.setOnClickListener {
            viewModel.qrClickReaction(activity as AppCompatActivity)
            ivQR.setQrCode(viewModel.getAccountAddress().value)
        }

    }

    override fun onResume() {
        eventBus.register(this)
        super.onResume()
    }

    override fun onPause() {
        eventBus.unregister(this)
        super.onPause()
    }


    fun addressClick() {
        Utils.setClipboardString(viewModel.getAccountAddress().value, activity)
        Toast.makeText(activity, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
    }
}
