package com.mycelium.wallet.activity.main.address

import android.app.Activity
import android.arch.lifecycle.ViewModelProviders
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v4.app.Fragment
import android.text.Html
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
import kotlinx.android.synthetic.main.address_fragment_path.*

class AddressFragment : Fragment() {
    private lateinit var mbwManager: MbwManager
    private lateinit var viewModel: AddressFragmentViewModel
    private var showBip44Path: Boolean = false

    private val eventBus: Bus
        get() = mbwManager!!.eventBus

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = DataBindingUtil.inflate<AddressFragmentBinding>(inflater, R.layout.address_fragment,
                container, false)
        binding.fragment = this
        binding.viewModel = viewModel
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setHasOptionsMenu(true)
        mbwManager = MbwManager.getInstance(activity)
        val viewModelProvider = ViewModelProviders.of(this)
        this.viewModel = when (mbwManager.selectedAccount) {
            is SingleAddressAccount, is HDAccount, is CoinapultAccount -> viewModelProvider.get(AddressFragmentBtcModel::class.java)
            else -> viewModelProvider.get(AddressFragmentCoinsModel::class.java)
        }
        if (!viewModel.isInitialized()) {
            viewModel.init(mbwManager.selectedAccount)
        }
        showBip44Path = mbwManager.getMetadataStorage().getShowBip44Path();
        super.onCreate(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ivQR.setOnClickListener {
            qrClick()
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
        if (!isAdded) {
            return
        }
        val account = mbwManager.selectedAccount
        if (account.isArchived) {
            return
        }

        // Update QR code
        val receivingAddress = Address.fromString(viewModel.getAccountAddress())

            // Set address
            ivQR.visibility = View.VISIBLE
            val address = viewModel.getAccountAddress()
            ivQR.qrCode = BitcoinUriWithAddress.fromAddress(receivingAddress).toString()
            tvAddress.setText(address)
            if (showBip44Path && receivingAddress.bip32Path != null) {
                val path = receivingAddress.bip32Path
                tvAddressPath.setText(path.toString())
            } else {
                tvAddressPath.setText("")
            }


        // Show name of bitcoin address according to address book
        val tvAddressTitle = tvAddressLabel
        val ivAccountType = ivAccountType

        var name = mbwManager!!.metadataStorage.getLabelByAccount(account.id)
        if (account is SingleAddressBCHAccount || account is Bip44BCHAccount) {
            name = getString(R.string.bitcoin_cash) + " - " + name
        }
        if (name.length == 0) {
            tvAddressTitle.visibility = View.GONE
            ivAccountType.visibility = View.GONE
        } else {
            tvAddressTitle.visibility = View.VISIBLE
            tvAddressTitle.setText(Html.fromHtml(name))

            // show account type icon next to the name
            val drawableForAccount = Utils.getDrawableForAccount(account, true, resources)
            if (drawableForAccount == null) {
                ivAccountType.visibility = View.GONE
            } else {
                ivAccountType.setImageDrawable(drawableForAccount)
                ivAccountType.visibility = View.VISIBLE
            }
        }
    }


    internal fun addressClick() {
        val address = viewModel.getAccountAddress()

        Utils.setClipboardString(address, activity)
        Toast.makeText(activity, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()

    }

    internal fun qrClick() {
        val account = mbwManager!!.selectedAccount
        viewModel.qrClickReaction()
        if(account is HDAccount || account is SingleAddressAccount || account is CoinapultAccount){
            updateUi()
        } else if (account.receivingAddress.isPresent) {
            ReceiveCoinsActivity.callMe(activity as Activity, account, account.canSpend())
        }
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
