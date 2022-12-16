package com.mycelium.wallet.activity.modern

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.fragment.app.Fragment
import com.mycelium.wallet.AddressBookManager
import com.mycelium.wallet.AddressBookManager.IconEntry
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import com.mycelium.wallet.activity.ScanActivity
import com.mycelium.wallet.activity.StringHandlerActivity
import com.mycelium.wallet.activity.modern.adapter.AddressBookAdapter
import com.mycelium.wallet.activity.modern.adapter.SelectAssetDialog.Companion.getInstance
import com.mycelium.wallet.activity.receive.ReceiveCoinsActivity.Companion.callMe
import com.mycelium.wallet.activity.send.SendCoinsActivity
import com.mycelium.wallet.activity.util.EnterAddressLabelUtil
import com.mycelium.wallet.activity.util.EnterAddressLabelUtil.AddressLabelChangedHandler
import com.mycelium.wallet.activity.util.getAddress
import com.mycelium.wallet.activity.util.getAssetUri
import com.mycelium.wallet.content.HandleConfigFactory.addressBookScanRequest
import com.mycelium.wallet.content.ResultType
import com.mycelium.wallet.databinding.AddressBookBinding
import com.mycelium.wallet.event.AddressBookChanged
import com.mycelium.wallet.event.AssetSelected
import com.mycelium.wapi.wallet.Address
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.squareup.otto.Subscribe
import java.util.*

class AddressBookFragment : Fragment() {
    private var mSelectedAddress: Address? = null
    private var mbwManager: MbwManager? = null
    private var addDialog: Dialog? = null
    private var currentActionMode: ActionMode? = null
    private var ownAddresses: Boolean? = null // set to null on purpose
    private var availableForSendingAddresses: Boolean? = null
    private var currency: CryptoCurrency? = null
    private var isSelectOnly = false
    private var binding: AddressBookBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mbwManager = MbwManager.getInstance(context)
        ownAddresses = requireArguments().getBoolean(OWN)
        availableForSendingAddresses = requireArguments().getBoolean(AVAILABLE_FOR_SENDING)
        currency = if (requireArguments().containsKey(SendCoinsActivity.ACCOUNT)) {
            mbwManager!!.getWalletManager(requireArguments().getBoolean(SendCoinsActivity.IS_COLD_STORAGE))
                    .getAccount((requireArguments().getSerializable(SendCoinsActivity.ACCOUNT) as UUID?)!!)!!.coinType
        } else {
            mbwManager!!.selectedAccount.coinType
        }
        isSelectOnly = requireArguments().getBoolean(SELECT_ONLY)
        setHasOptionsMenu(!isSelectOnly)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
            AddressBookBinding.inflate(inflater).apply {
                binding = this
            }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.lvForeignAddresses?.onItemClickListener = if (isSelectOnly) SelectItemListener() else itemListClickListener
    }

    override fun onResume() {
        MbwManager.getEventBus().register(this)
        updateUi()
        super.onResume()
    }

    override fun onPause() {
        MbwManager.getEventBus().unregister(this)
        super.onPause()
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    override fun onDestroy() {
        if (addDialog != null && addDialog!!.isShowing) {
            addDialog!!.dismiss()
        }
        super.onDestroy()
    }

    private fun updateUi() {
        if (!isAdded) {
            return
        }
        if (ownAddresses!!) {
            updateUiMine()
        } else {
            if (availableForSendingAddresses!!) {
                updateUiSending()
            } else {
                updateUiForeign()
            }
        }
    }

    private fun updateUiMine() {
        val activeAccounts = ArrayList(mbwManager!!.getWalletManager(false).getAllActiveAccounts())
        val entries = Utils.sortAccounts(activeAccounts, mbwManager!!.metadataStorage)
                .filter { it.receiveAddress != null && currency!!.id == it.coinType.id }
                .map { account ->
                    val name = mbwManager!!.metadataStorage.getLabelByAccount(account.id)
                    val drawableForAccount = Utils.getDrawableForAccount(account, true, resources)
                    IconEntry(account.receiveAddress, name, drawableForAccount, account.id)
                }
        if (entries.isEmpty()) {
            binding?.tvNoRecords?.visibility = View.VISIBLE
            binding?.lvForeignAddresses?.visibility = View.GONE
        } else {
            binding?.tvNoRecords?.visibility = View.GONE
            binding?.lvForeignAddresses?.visibility = View.VISIBLE
            binding?.lvForeignAddresses?.adapter = AddressBookAdapter(activity, R.layout.address_book_my_address_row, entries)
        }
    }

    private fun updateUiForeign() {
        val rawEntries = mbwManager!!.metadataStorage.allAddressLabels
        val entries = Utils.sortAddressbookEntries(rawEntries.map { (key, value) -> AddressBookManager.Entry(key, value) })
        if (entries.isEmpty()) {
            binding?.tvNoRecords?.visibility = View.VISIBLE
            binding?.lvForeignAddresses?.visibility = View.GONE
        } else {
            binding?.tvNoRecords?.visibility = View.GONE
            binding?.lvForeignAddresses?.visibility = View.VISIBLE
            binding?.lvForeignAddresses?.adapter = AddressBookAdapter(activity, R.layout.address_book_foreign_row, entries)
        }
    }

    private fun updateUiSending() {
        val rawEntries: Map<Address, String> = mbwManager!!.metadataStorage.allAddressLabels
        val account = mbwManager!!.selectedAccount
        val entries = Utils.sortAddressbookEntries(rawEntries.filter { (key, value) -> key.coinType == account.basedOnCoinType }
                .map { (key, value) -> AddressBookManager.Entry(key, value) })
        if (entries.isEmpty()) {
            binding?.tvNoRecords?.visibility = View.VISIBLE
            binding?.lvForeignAddresses?.visibility = View.GONE
        } else {
            binding?.tvNoRecords?.visibility = View.GONE
            binding?.lvForeignAddresses?.visibility = View.VISIBLE
            binding?.lvForeignAddresses?.adapter = AddressBookAdapter(activity, R.layout.address_book_sending_row, entries)
        }
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        if (!isVisibleToUser) {
            finishActionMode()
        }
    }

    private fun finishActionMode() {
        currentActionMode?.finish()
    }

    private val itemListClickListener = OnItemClickListener { listView, view, position, id ->
        mSelectedAddress = view.tag as Address
        val parent = requireActivity() as AppCompatActivity
        currentActionMode = parent.startSupportActionMode(object : ActionMode.Callback {
            override fun onCreateActionMode(actionMode: ActionMode, menu: Menu): Boolean {
                actionMode.menuInflater.inflate(R.menu.addressbook_context_menu, menu)
                return true
            }

            override fun onPrepareActionMode(actionMode: ActionMode, menu: Menu): Boolean {
                currentActionMode = actionMode
                view.background = resources.getDrawable(R.color.selectedrecord)
                return true
            }

            override fun onActionItemClicked(actionMode: ActionMode, menuItem: MenuItem): Boolean {
                val item = menuItem.itemId
                if (item == R.id.miDeleteAddress) {
                    mbwManager!!.runPinProtectedFunction(activity, pinProtectedDeleteEntry)
                    return true
                } else if (item == R.id.miEditAddress) {
                    mbwManager!!.runPinProtectedFunction(activity, pinProtectedEditEntry)
                    return true
                } else if (item == R.id.miShowQrCode) {
                    doShowQrCode()
                    return true
                }
                return false
            }

            override fun onDestroyActionMode(actionMode: ActionMode) {
                view.background = null
                currentActionMode = null
            }

            private fun doShowQrCode() {
                if (!isAdded) {
                    return
                }
                if (mSelectedAddress == null) {
                    return
                }
                val hasPrivateKey = mbwManager!!.getWalletManager(false).hasPrivateKey(mSelectedAddress!!)
                val tempAccount = mbwManager!!.createOnTheFlyAccount(mSelectedAddress)
                callMe(requireActivity(), mbwManager!!.getWalletManager(true).getAccount(tempAccount)!!,
                        hasPrivateKey, false, true)
                finishActionMode()
            }
        })
    }
    private val pinProtectedDeleteEntry = Runnable {
        AlertDialog.Builder(activity)
                .setMessage(R.string.delete_address_confirmation)
                .setCancelable(false)
                .setPositiveButton(R.string.yes) { dialog, id ->
                    dialog.cancel()
                    mbwManager!!.metadataStorage.deleteAddressMetadata(mSelectedAddress!!)
                    finishActionMode()
                    MbwManager.getEventBus().post(AddressBookChanged())
                }
                .setNegativeButton(R.string.no) { dialog, id -> finishActionMode() }
                .create()
                .show()
    }

    private inner class AddDialog(activity: Activity) : Dialog(activity) {
        init {
            setContentView(R.layout.add_to_address_book_dialog)
            setTitle(R.string.add_to_address_book_dialog_title)
            findViewById<View>(R.id.btScan).setOnClickListener { v: View? ->
                val request = addressBookScanRequest
                ScanActivity.callMe(this@AddressBookFragment, SCAN_RESULT_CODE.toInt(), request)
                dismiss()
            }
            val addresses = mbwManager!!.getWalletManager(false).parseAddress(Utils.getClipboardString(activity))
            findViewById<View>(R.id.btClipboard).isEnabled = addresses.size != 0
            findViewById<View>(R.id.btClipboard).setOnClickListener { v: View? ->
                if (addresses.isNotEmpty()) {
                    if (addresses.size == 1) {
                        addFromAddress(addresses[0])
                    } else {
                        getInstance(addresses).show(requireFragmentManager(), "dialog")
                    }
                } else {
                    Toaster(this@AddDialog.context).toast(R.string.unrecognized_format, true)
                }
                dismiss()
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
            if (item.itemId == R.id.miAddAddress) {
                addDialog = AddDialog(requireActivity())
                addDialog?.show()
                true
            } else {
                super.onOptionsItemSelected(item)
            }


    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        if (requestCode != SCAN_RESULT_CODE.toInt()) {
            super.onActivityResult(requestCode, resultCode, intent)
        }
        if (resultCode != Activity.RESULT_OK) {
            if (intent == null) {
                return  // user pressed back
            }
            val error = intent.getStringExtra(StringHandlerActivity.RESULT_ERROR)
            if (error != null) {
                Toaster(requireActivity()).toast(error, false)
            }
            return
        }
        val type = intent!!.getSerializableExtra(StringHandlerActivity.RESULT_TYPE_KEY) as ResultType?
        when (type) {
            ResultType.PRIVATE_KEY -> Utils.showSimpleMessageDialog(activity, R.string.addressbook_cannot_add_private_key)
            ResultType.ASSET_URI -> addFromAddress(intent.getAssetUri().address)
            ResultType.ADDRESS -> addFromAddress(intent.getAddress())
        }
    }

    private fun addFromAddress(address: Address?) {
        EnterAddressLabelUtil.enterAddressLabel(requireContext(), mbwManager!!.metadataStorage, address, "", addressLabelChanged)
    }

    private val addressLabelChanged = AddressLabelChangedHandler { address: String?, label: String? ->
        finishActionMode()
        MbwManager.getEventBus().post(AddressBookChanged())
    }

    private val pinProtectedEditEntry = Runnable {
        EnterAddressLabelUtil.enterAddressLabel(requireActivity(), mbwManager!!.metadataStorage,
                mSelectedAddress, "", addressLabelChanged)
    }

    @Subscribe
    fun onAddressBookChanged(event: AddressBookChanged?) {
        updateUi()
    }

    @Subscribe
    fun newAddressCreating(event: AssetSelected) {
        addFromAddress(event.address)
    }

    private inner class SelectItemListener : OnItemClickListener {
        override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
            val address = view.tag as Address
            val result = Intent()
            result.putExtra(ADDRESS_RESULT_NAME, address)
            if (parent.getItemAtPosition(position) is IconEntry) {
                val item = parent.getItemAtPosition(position) as IconEntry
                result.putExtra(ADDRESS_RESULT_ID, item.id)
                result.putExtra(ADDRESS_RESULT_LABEL, item.name)
            }
            requireActivity().setResult(Activity.RESULT_OK, result)
            requireActivity().finish()
        }
    }

    companion object {
        private const val SCAN_RESULT_CODE: Short = 1
        const val ADDRESS_RESULT_NAME = "address_result"
        const val ADDRESS_RESULT_ID = "address_result_id"
        const val OWN = "own"
        const val AVAILABLE_FOR_SENDING = "available_for_sending"
        const val SELECT_ONLY = "selectOnly"
        const val ADDRESS_RESULT_LABEL = "address_result_label"
    }
}