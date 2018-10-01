package com.mycelium.wallet.activity.main.address

import android.app.Activity
import android.arch.lifecycle.ViewModelProviders
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.mrd.bitlib.model.Address
import com.mycelium.wallet.BitcoinUriWithAddress
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import com.mycelium.wallet.activity.receive.ReceiveCoinsActivity
import com.mycelium.wallet.coinapult.CoinapultAccount
import com.mycelium.wallet.databinding.AddressFragmentBinding
import com.mycelium.wallet.databinding.AddressFragmentBindingImpl
import com.mycelium.wallet.databinding.AddressFragmentBtcBindingImpl
import com.mycelium.wallet.event.AccountChanged
import com.mycelium.wallet.event.BalanceChanged
import com.mycelium.wallet.event.ReceivingAddressChanged
import com.mycelium.wapi.wallet.bip44.Bip44BCHAccount
import com.mycelium.wapi.wallet.bip44.HDAccount
import com.mycelium.wapi.wallet.single.SingleAddressAccount
import com.squareup.otto.Bus
import com.squareup.otto.Subscribe
import kotlinx.android.synthetic.main.address_fragment_qr.*
import kotlinx.android.synthetic.main.address_fragment_addr.*
import kotlinx.android.synthetic.main.address_fragment_label.*
import kotlinx.android.synthetic.main.address_fragment_path.*

class AddressFragment : Fragment() {
    private var mbwManager = MbwManager.getInstance(activity)
    private lateinit var viewModel: AddressFragmentViewModel

    private val eventBus: Bus
        get() = mbwManager!!.eventBus

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding =
        when (mbwManager.selectedAccount) {
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

        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setHasOptionsMenu(true)
        val viewModelProvider = ViewModelProviders.of(this)
        this.viewModel = when (mbwManager.selectedAccount) {
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
        copyTo.setOnClickListener {
            addressClick()
        }

    }

    override fun onResume() {
        eventBus.register(this)
        updateUi()
        super.onResume()
    }

    override fun onPause() {
        eventBus.unregister(this)
        super.onPause()
    }

    private fun updateUi() {
        if (!isAdded || mbwManager.selectedAccount.isArchived) {
            return
        }

        // Update QR code
        ivQR.visibility = View.VISIBLE
        ivQR.qrCode = viewModel.getAddressUri()

        if (viewModel.getAccountLabel().equals("")) {
            tvAddressLabel.visibility = View.GONE
            ivAccountType.visibility = View.GONE
        } else {
            // show account type icon next to the name
            tvAddressLabel.visibility = View.VISIBLE
            ivAccountType.visibility = View.VISIBLE
            ivAccountType.setImageDrawable(viewModel.getDrawableForAccount(resources))
        }
    }


    internal fun addressClick() {
        Utils.setClipboardString(viewModel.getAccountAddress(), activity)
        Toast.makeText(activity, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()

    }

    /**
     * We got a new Receiving Address, either because the selected Account changed,
     * or because our HD Account received Coins and changed the Address
     */
    @Subscribe
    fun receivingAddressChanged(event: ReceivingAddressChanged) {
        updateUi()
    }

    @Subscribe
    fun accountChanged(event: AccountChanged) {
        updateUi()
    }

    @Subscribe
    fun balanceChanged(event: BalanceChanged) {
        updateUi()
    }
}
