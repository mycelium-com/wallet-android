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
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import butterknife.BindView
import butterknife.OnClick
import com.google.common.base.Optional
import com.mrd.bitlib.model.Address
import com.mycelium.wallet.BitcoinUriWithAddress
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import com.mycelium.wallet.activity.receive.ReceiveCoinsActivity
import com.mycelium.wallet.activity.util.QrImageView
import com.mycelium.wallet.coinapult.CoinapultAccount
import com.mycelium.wallet.databinding.AddressFragmentBinding
import com.mycelium.wallet.event.AccountChanged
import com.mycelium.wallet.event.BalanceChanged
import com.mycelium.wallet.event.ReceivingAddressChanged
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.bip44.HDAccount
import com.mycelium.wapi.wallet.single.SingleAddressAccount
import com.squareup.otto.Bus
import com.squareup.otto.Subscribe

class AddressFragment : Fragment() {

    private var _root: View? = null
    private lateinit var _mbwManager: MbwManager
    private lateinit var viewModel: AddressFragmentViewModel
    private var _showBip44Path: Boolean = false
    @BindView(R.id.ivQR)
    public var qrButton: QrImageView? = null

    private val eventBus: Bus
        get() = _mbwManager!!.eventBus

    val address: Optional<Address>
        get() = _mbwManager!!.selectedAccount.receivingAddress

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = DataBindingUtil.inflate<AddressFragmentBinding>(inflater, R.layout.address_fragment, container, false)
        binding.setLifecycleOwner(this)
        return this.view
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        _mbwManager = MbwManager.getInstance(activity)
        val viewModelProvider = ViewModelProviders.of(this)
        this.viewModel = when (_mbwManager.selectedAccount) {
            is SingleAddressAccount, is HDAccount, is CoinapultAccount -> viewModelProvider.get(AddressFragmentBtcModel::class.java)
            else -> viewModelProvider.get(AddressFragmentCoinsModel::class.java)
        }
        DataBindingUtil.setContentView<AddressFragmentBinding>(this.requireActivity(), R.layout.address_fragment)
        if (!viewModel.isInitialized()) {
            viewModel.init(_mbwManager.selectedAccount)
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
        val account = _mbwManager.selectedAccount
        if (account.isArchived) {
            return
        }

        // Update QR code
        val receivingAddress = address

        // Update address
        if (receivingAddress.isPresent) {
            // Set address
            qrButton!!.visibility = View.VISIBLE
            val address = receivingAddress.get().toString()
            qrButton!!.qrCode = BitcoinUriWithAddress.fromAddress(receivingAddress.get()).toString()
            (_root!!.findViewById(R.id.tvAddress) as TextView).text = address
            if (_showBip44Path && receivingAddress.get().bip32Path != null) {
                val path = receivingAddress.get().bip32Path
                (_root!!.findViewById(R.id.tvAddressPath) as TextView).text = path.toString()
            } else {
                (_root!!.findViewById(R.id.tvAddressPath) as TextView).text = ""
            }
        } else {
            // No address available
            qrButton!!.visibility = View.INVISIBLE
            (_root!!.findViewById(R.id.tvAddress) as TextView).text = ""
            (_root!!.findViewById(R.id.tvAddressPath) as TextView).text = ""
        }

        // Show name of bitcoin address according to address book
        val tvAddressTitle = _root!!.findViewById(R.id.tvAddressLabel) as TextView
        val ivAccountType = _root!!.findViewById(R.id.ivAccountType) as ImageView

        var name = _mbwManager!!.metadataStorage.getLabelByAccount(account.id)
        if (account.type == WalletAccount.Type.BCHSINGLEADDRESS || account.type == WalletAccount.Type.BCHBIP44) {
            name = getString(R.string.bitcoin_cash) + " - " + name
        }
        if (name.length == 0) {
            tvAddressTitle.visibility = View.GONE
            ivAccountType.visibility = View.GONE
        } else {
            tvAddressTitle.visibility = View.VISIBLE
            tvAddressTitle.text = Html.fromHtml(name)

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

    @OnClick(R.id.address_layout)
    internal fun addressClick() {
        val address = address
        if (address.isPresent) {
            Utils.setClipboardString(address.get().toString(), activity)
            Toast.makeText(activity, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
        }
    }

    @OnClick(R.id.ivQR)
    internal fun qrClick() {
        val account = _mbwManager!!.selectedAccount
        if (account.receivingAddress.isPresent) {
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
