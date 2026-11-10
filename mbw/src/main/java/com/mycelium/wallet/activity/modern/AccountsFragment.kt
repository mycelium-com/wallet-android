package com.mycelium.wallet.activity.modern

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.text.Html
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.CompoundButton
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.common.base.Preconditions
import com.mrd.bitlib.model.AddressType
import com.mycelium.bequant.intro.BequantIntroActivity
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import com.mycelium.wallet.activity.AddAccountActivity
import com.mycelium.wallet.activity.AddAdvancedAccountActivity
import com.mycelium.wallet.activity.MessageSigningActivity
import com.mycelium.wallet.activity.export.VerifyBackupActivity
import com.mycelium.wallet.activity.modern.adapter.AccountListAdapter
import com.mycelium.wallet.activity.modern.helper.AccountsActionModeCallback
import com.mycelium.wallet.activity.settings.boostGapLimitDialog
import com.mycelium.wallet.activity.util.EnterAddressLabelUtil
import com.mycelium.wallet.activity.util.toStringFriendlyWithUnit
import com.mycelium.wallet.activity.util.toStringWithUnit
import com.mycelium.wallet.activity.view.DividerItemDecoration
import com.mycelium.wallet.databinding.FragmentAccountsBinding
import com.mycelium.wallet.event.AccountChanged
import com.mycelium.wallet.event.AccountListChanged
import com.mycelium.wallet.event.BalanceChanged
import com.mycelium.wallet.event.ExchangeRatesRefreshed
import com.mycelium.wallet.event.ExchangeSourceChanged
import com.mycelium.wallet.event.ExtraAccountsChanged
import com.mycelium.wallet.event.ReceivingAddressChanged
import com.mycelium.wallet.event.SelectedCurrencyChanged
import com.mycelium.wallet.event.SyncFailed
import com.mycelium.wallet.event.SyncProgressUpdated
import com.mycelium.wallet.event.SyncStarted
import com.mycelium.wallet.event.SyncStopped
import com.mycelium.wallet.lt.LocalTraderEventSubscriber
import com.mycelium.wallet.lt.LocalTraderManager
import com.mycelium.wallet.lt.api.CreateTrader
import com.mycelium.wallet.lt.api.DeleteTrader
import com.mycelium.wallet.persistence.MetadataStorage
import com.mycelium.wapi.wallet.Address
import com.mycelium.wapi.wallet.AddressUtils
import com.mycelium.wapi.wallet.AesKeyCipher
import com.mycelium.wapi.wallet.ExportableAccount
import com.mycelium.wapi.wallet.KeyCipher.InvalidKeyCipher
import com.mycelium.wapi.wallet.SyncMode
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.WalletManager
import com.mycelium.wapi.wallet.bch.bip44.Bip44BCHAccount
import com.mycelium.wapi.wallet.bch.single.SingleAddressBCHAccount
import com.mycelium.wapi.wallet.btc.bip44.BitcoinHDModule
import com.mycelium.wapi.wallet.btc.bip44.HDAccount
import com.mycelium.wapi.wallet.btc.bip44.HDPubOnlyAccount
import com.mycelium.wapi.wallet.btc.bip44.getActiveMasterseedAccounts
import com.mycelium.wapi.wallet.btc.bip44.getActiveMasterseedHDAccounts
import com.mycelium.wapi.wallet.btc.single.SingleAddressAccount
import com.mycelium.wapi.wallet.coins.AssetInfo
import com.mycelium.wapi.wallet.coins.Balance
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.colu.AddressColuConfig
import com.mycelium.wapi.wallet.colu.ColuAccount
import com.mycelium.wapi.wallet.colu.coins.ColuMain
import com.mycelium.wapi.wallet.colu.getColuAccounts
import com.mycelium.wapi.wallet.erc20.ERC20Account
import com.mycelium.wapi.wallet.erc20.getActiveERC20Accounts
import com.mycelium.wapi.wallet.erc20.getERC20Accounts
import com.mycelium.wapi.wallet.eth.AbstractEthERC20Account
import com.mycelium.wapi.wallet.eth.EthAccount
import com.mycelium.wapi.wallet.eth.getEthAccounts
import com.mycelium.wapi.wallet.trc20.TRC20Account
import com.mycelium.wapi.wallet.trc20.getActiveTRC20Accounts
import com.mycelium.wapi.wallet.trc20.getTRC20Accounts
import com.mycelium.wapi.wallet.tron.AbstractTronAccount
import com.mycelium.wapi.wallet.tron.TronAccount
import com.mycelium.wapi.wallet.manager.Config
import com.squareup.otto.Bus
import com.squareup.otto.Subscribe
import java.util.UUID

class AccountsFragment : Fragment() {
    private var walletManager: WalletManager? = null
    private var _storage: MetadataStorage? = null
    private var _mbwManager: MbwManager? = null
    private var localTraderManager: LocalTraderManager? = null
    private var toaster: Toaster? = null
    private var accountListAdapter: AccountListAdapter? = null
    private var eventBus: Bus? = null
    private var binding: FragmentAccountsBinding? = null
    private val menuProvider = MenuImpl()

    /**
     * Called when the activity is first created.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentAccountsBinding.inflate(inflater, container, false).apply {
        binding = this
    }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.rvRecords?.let { rvRecords ->
            accountListAdapter = AccountListAdapter(this, _mbwManager!!)
            rvRecords.setAdapter(accountListAdapter)
            rvRecords.addItemDecoration(
                DividerItemDecoration(
                    resources.getDrawable(R.drawable.divider_account_list),
                    LinearLayoutManager.VERTICAL
                )
            )
            rvRecords.setHasFixedSize(true)
            rvRecords.itemAnimator?.changeDuration = 0 //avoid item blinking
        }
        accountListAdapter?.setItemClickListener(recordAddressClickListener)
        accountListAdapter?.investmentAccountClickListener =
            object : AccountListAdapter.ItemClickListener {
                override fun onItemClick(account: WalletAccount<out Address>) {
                    startActivity(Intent(requireContext(), BequantIntroActivity::class.java))
                }
            }
    }

    override fun onAttach(context: Context) {
        _mbwManager = MbwManager.getInstance(context)
        walletManager = _mbwManager!!.getWalletManager(false)
        localTraderManager = _mbwManager!!.localTraderManager
        localTraderManager!!.subscribe(ltSubscriber)
        _storage = _mbwManager!!.metadataStorage
        eventBus = MbwManager.getEventBus()
        toaster = Toaster(this)
        super.onAttach(context)
    }

    override fun setMenuVisibility(menuVisible: Boolean) {
        super.setMenuVisibility(menuVisible)
        if(binding == null) {
            return
        }
        if (menuVisible) {
            requireActivity().addMenuProvider(menuProvider, viewLifecycleOwner)
        } else {
            requireActivity().removeMenuProvider(menuProvider)
            finishCurrentActionMode()
        }
    }

    override fun onResume() {
        eventBus!!.register(this)
        binding?.btUnlock?.setOnClickListener(unlockClickedListener)
        update()
        super.onResume()
    }

    override fun onPause() {
        eventBus!!.unregister(this)
        finishCurrentActionMode()
        super.onPause()
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    override fun onDetach() {
        localTraderManager!!.unsubscribe(ltSubscriber)
        super.onDetach()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        requireActivity().invalidateOptionsMenu()
        if (requestCode == ADD_RECORD_RESULT_CODE && resultCode == Activity.RESULT_OK) {
            val accountid = data?.getSerializableExtra(AddAccountActivity.RESULT_KEY) as UUID?
            if (accountid != null) {
                //check whether the account is active - we might have scanned the priv key for an archived watchonly
                val account = walletManager!!.getAccount(accountid)
                if (account!!.isActive) {
                    _mbwManager!!.setSelectedAccount(accountid)
                }
                accountListAdapter!!.setFocusedAccountId(account.id)
                updateIncludingMenus()
                if (account !is ColuAccount && account !is ERC20Account && account !is TRC20Account &&
                    !data.getBooleanExtra(AddAccountActivity.IS_UPGRADE, false)
                ) {
                    setLabelOnAccount(account, account.label, false)
                }
                eventBus!!.post(ExtraAccountsChanged())
                eventBus!!.post(AccountChanged(accountid))
            }
        } else if (requestCode == ADD_RECORD_RESULT_CODE && resultCode == AddAdvancedAccountActivity.RESULT_MSG) {
            AlertDialog.Builder(requireActivity())
                .setMessage(data?.getStringExtra(AddAccountActivity.RESULT_MSG))
                .setPositiveButton(R.string.button_ok, null)
                .create()
                .show()
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun interruptSync(accountsToInterrupt: Collection<WalletAccount<*>>) {
        accountsToInterrupt.forEach { wa ->
            wa.interruptSync()
        }
    }

    private fun getPotentialBalance(account: WalletAccount<*>): Value? {
        if (account.isArchived) {
            return null
        } else {
            return account.accountBalance.spendable
        }
    }

    private fun deleteAccount(accountToDelete: WalletAccount<*>) {
        Preconditions.checkNotNull<WalletAccount<*>?>(accountToDelete)
        val linkedAccounts = getLinkedAccounts(accountToDelete)

        val accountsToInterrupt: MutableCollection<WalletAccount<*>> = HashSet<WalletAccount<*>>()
        accountsToInterrupt.add(accountToDelete)
        accountsToInterrupt.addAll(linkedAccounts)
        interruptSync(accountsToInterrupt)

        val checkBoxView = View.inflate(activity, R.layout.delkey_checkbox, null)
        val keepAddrCheckbox = checkBoxView.findViewById<CheckBox>(R.id.checkbox)
        keepAddrCheckbox.text = getString(R.string.i_know_what_iam_doing)
        keepAddrCheckbox.isChecked = false

        val deleteDialog = AlertDialog.Builder(requireActivity())
        deleteDialog.setTitle(R.string.delete_account)
        deleteDialog.setMessage(Html.fromHtml(createDeleteDialogText(accountToDelete, linkedAccounts)))

        //        // add checkbox only for SingleAddressAccounts and only if a private key is present
        val hasPrivateData = (accountToDelete is ExportableAccount
                && (accountToDelete as ExportableAccount).getExportData(AesKeyCipher.defaultKeyCipher()).privateData.isPresent)
        val deleteCheckbox = accountToDelete is SingleAddressAccount && hasPrivateData

        if (deleteCheckbox) {
            deleteDialog.setView(checkBoxView)
        }

        //        if (accountToDelete instanceof ColuAccount && accountToDelete.canSpend()) {
//            Log.d(TAG, "Preparing to delete a colu account.");
//            deleteDialog.setView(checkBoxView);
//        }
        deleteDialog.setPositiveButton(
            R.string.yes,
            DialogInterface.OnClickListener { arg0: DialogInterface?, arg1: Int ->
                Log.d(TAG, "Entering onClick delete")
                if (accountToDelete.id == localTraderManager!!.localTraderAccountId) {
                    localTraderManager!!.unsetLocalTraderAccount()
                }
                if (hasPrivateData) {
                    val potentialBalance = getPotentialBalance(accountToDelete)
                    val confirmDeleteDialog = AlertDialog.Builder(requireActivity())
                    confirmDeleteDialog.setTitle(R.string.confirm_delete_pk_title)

                    // Set the message. There are four combinations, with and without label, with and without BTC amount.
                    var label = _mbwManager!!.metadataStorage.getLabelByAccount(accountToDelete.id)
                    var labelCount = 1
                    if (!linkedAccounts.isEmpty()) {
                        label += ", " + _mbwManager!!.metadataStorage.getLabelByAccount(linkedAccounts[0].id)
                        labelCount++
                    }
                    val message: String?

                    // For active accounts we check whether there is money on them before deleting. we don't know if there
                    // is money on archived accounts
                    val address: String?
                    if (accountToDelete is SingleAddressAccount) {
                        address = accountToDelete.getPublicKey()
                            ?.getAllSupportedAddresses(_mbwManager!!.network)
                            ?.values?.joinToString("\n\n")
                    } else {
                        val receivingAddress: Address? = accountToDelete.receiveAddress
                        if (receivingAddress != null) {
                            address = AddressUtils.toMultiLineString(receivingAddress.toString())
                        } else {
                            address = ""
                        }
                    }
                    if (accountToDelete.isActive && potentialBalance != null && potentialBalance.moreThanZero()) {
                        if (!label.isEmpty()) {
                            message = resources.getQuantityString(
                                R.plurals.confirm_delete_pk_with_balance_with_label,
                                if (accountToDelete !is SingleAddressAccount) 1 else 0,
                                resources.getQuantityString(R.plurals.account_label, labelCount, label),
                                address, getBalanceString(accountToDelete.coinType, accountToDelete.accountBalance)
                            )
                        } else {
                            message = resources.getQuantityString(
                                R.plurals.confirm_delete_pk_with_balance,
                                if (accountToDelete !is SingleAddressAccount) 1 else 0,
                                getBalanceString(accountToDelete.coinType, accountToDelete.accountBalance)
                            )
                        }
                    } else {
                        if (!label.isEmpty()) {
                            message = resources.getQuantityString(
                                R.plurals.confirm_delete_pk_without_balance_with_label,
                                if (accountToDelete !is SingleAddressAccount) 1 else 0,
                                resources.getQuantityString(R.plurals.account_label, labelCount, label), address
                            )
                        } else {
                            message = resources.getQuantityString(
                                R.plurals.confirm_delete_pk_without_balance,
                                if (accountToDelete !is SingleAddressAccount) 1 else 0, address
                            )
                        }
                    }
                    confirmDeleteDialog.setMessage(
                        (message + "\n"
                                + getString(R.string.confirm_delete_account_message_end))
                    )

                    confirmDeleteDialog.setPositiveButton(
                        R.string.yes,
                        DialogInterface.OnClickListener { arg2: DialogInterface?, arg3: Int ->
                            Log.d(TAG, "In deleteFragment onClick")
                            if ( /*keepAddrCheckbox.isChecked()*/false && accountToDelete is SingleAddressAccount) {
                                try {
                                    //Check if this SingleAddress account is related with ColuAccount
                                    val linkedColuAccount =
                                        Utils.getLinkedAccount(accountToDelete, walletManager!!.getAccounts())
                                    if (linkedColuAccount is ColuAccount) {
                                        walletManager!!.deleteAccount(linkedColuAccount.id)
                                        walletManager!!.deleteAccount(accountToDelete.id)
                                        val context = linkedColuAccount.context
                                        val coluMain = linkedColuAccount.coinType as ColuMain
                                        val config: Config =
                                            AddressColuConfig(context.address!!.get(AddressType.P2PKH)!!, coluMain)
                                        _storage!!.deleteAccountMetadata(linkedColuAccount.id)
                                        walletManager!!.createAccounts(config)
                                    } else {
                                        (accountToDelete as SingleAddressAccount).forgetPrivateKey(AesKeyCipher.defaultKeyCipher())
                                    }
                                    toaster!!.toast(R.string.private_key_deleted, false)
                                } catch (e: InvalidKeyCipher) {
                                    throw RuntimeException(e)
                                }
                            } else {
                                if (accountToDelete is ColuAccount) {
                                    try {
                                        walletManager!!.deleteAccount(accountToDelete.id)
                                        val linkedAccount =
                                            Utils.getLinkedAccount(accountToDelete, walletManager!!.getAccounts())
                                        if (linkedAccount != null) {
                                            walletManager!!.deleteAccount(linkedAccount.id)
                                            _storage!!.deleteAccountMetadata(linkedAccount.id)
                                        }
                                        if (false /*keepAddrCheckbox.isChecked()*/) {
                                            val context = accountToDelete.context
                                            val coluMain = accountToDelete.coinType as ColuMain
                                            val config: Config =
                                                AddressColuConfig(context.address!!.get(AddressType.P2PKH)!!, coluMain)
                                            _storage!!.deleteAccountMetadata(accountToDelete.id)
                                            walletManager!!.createAccounts(config)
                                        } else {
                                            _storage!!.deleteAccountMetadata(accountToDelete.id)
                                            toaster!!.toast("Deleting account.", false)
                                            _mbwManager!!.setSelectedAccount(
                                                _mbwManager!!.getWalletManager(false).getActiveSpendingAccounts()
                                                    .get(0).id
                                            )
                                        }
                                    } catch (e: Exception) {
                                        // make a message !
                                        Log.e(TAG, getString(R.string.colu_error_deleting), e)
                                        toaster!!.toast(getString(R.string.colu_error_deleting), false)
                                    }
                                } else {
                                    //Check if this SingleAddress account is related with ColuAccount
                                    val linkedColuAccount =
                                        Utils.getLinkedAccount(accountToDelete, walletManager!!.getAccounts())
                                    if (linkedColuAccount is ColuAccount) {
                                        walletManager!!.deleteAccount(linkedColuAccount.id)
                                        _storage!!.deleteAccountMetadata(linkedColuAccount.id)
                                    }
                                    walletManager!!.deleteAccount(accountToDelete.id)
                                    _storage!!.deleteAccountMetadata(accountToDelete.id)
                                    // ETH/TRON accounts carry their tokens: delete them too, otherwise the
                                    // orphaned ERC-20/TRC-20 contexts crash the wallet on the next start
                                    deleteLinkedTokenAccounts(accountToDelete)
                                    _mbwManager!!.setSelectedAccount(
                                        _mbwManager!!.getWalletManager(false).getActiveSpendingAccounts().get(0).id
                                    )
                                    toaster!!.toast(R.string.account_deleted, false)
                                }
                            }
                            finishCurrentActionMode()
                            eventBus!!.post(AccountChanged(accountToDelete.id))
                        })
                    confirmDeleteDialog.setNegativeButton(R.string.no, null)
                    confirmDeleteDialog.show()
                } else {
                    // account has no private data - dont make a fuzz about it and just delete it
                    walletManager!!.deleteAccount(accountToDelete.id)
                    _storage!!.deleteAccountMetadata(accountToDelete.id)
                    // remove linked accounts if necessary
                    if (accountToDelete is EthAccount) {
                        for (walletAccount in getLinkedERC20Accounts(accountToDelete)) {
                            walletManager!!.deleteAccount(walletAccount.id)
                            _storage!!.deleteAccountMetadata(walletAccount.id)
                        }
                    } else if (accountToDelete is ERC20Account) {
                        val ethAccount = getLinkedEthAccount(accountToDelete)
                        ethAccount.updateEnabledTokens()
                    } else if (accountToDelete is TronAccount) {
                        for (walletAccount in getLinkedTRC20Accounts(accountToDelete)) {
                            walletManager!!.deleteAccount(walletAccount.id)
                            _storage!!.deleteAccountMetadata(walletAccount.id)
                        }
                    } else if (accountToDelete is TRC20Account) {
                        accountToDelete.tronAcc.updateEnabledTokens()
                    } else {
                        //Check if this SingleAddress account is related with ColuAccount
                        val linkedColuAccount = Utils.getLinkedAccount(accountToDelete, walletManager!!.getAccounts())
                        if (linkedColuAccount != null) {
                            walletManager!!.deleteAccount(linkedColuAccount.id)
                            _storage!!.deleteAccountMetadata(linkedColuAccount.id)
                        }
                    }
                    finishCurrentActionMode()
                    eventBus!!.post(AccountChanged(accountToDelete.id))
                    toaster!!.toast(R.string.account_deleted, false)
                }
            })
        deleteDialog.setNegativeButton(R.string.no, null)
        val dialog = deleteDialog.create()
        // Disable the positive button until the checkbox is checked
        if (deleteCheckbox) {
            dialog.setOnShowListener(DialogInterface.OnShowListener { dialogInterface: DialogInterface? ->
                val yesButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                yesButton.isEnabled = false
                keepAddrCheckbox.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
                    yesButton.isEnabled = isChecked
                })
            })
        }
        dialog.show()
    }

    private fun makeWatchOnly(accountToDelete: WalletAccount<*>?) {
        Preconditions.checkNotNull<WalletAccount<*>?>(accountToDelete)
        val linkedAccounts = getLinkedAccounts(accountToDelete)


        val deleteDialog = AlertDialog.Builder(requireActivity())
        deleteDialog.setTitle(R.string.delete_private_key_title)
        deleteDialog.setMessage(R.string.delete_private_key_message)

        deleteDialog.setPositiveButton(
            R.string.button_continue,
            DialogInterface.OnClickListener { arg0: DialogInterface?, arg1: Int ->
                val checkBoxView = View.inflate(activity, R.layout.delkey_checkbox, null)
                val keepAddrCheckbox = checkBoxView.findViewById<CheckBox>(R.id.checkbox)
                keepAddrCheckbox.text = getString(R.string.delete_private_key_checkbox)
                keepAddrCheckbox.isChecked = false

                // an archived account throws on balance access
                val valueString = if (accountToDelete!!.isArchived) "?"
                    else getBalanceString(accountToDelete.coinType, accountToDelete.accountBalance)
                val deleteDialog2 = AlertDialog.Builder(requireActivity())
                    .setMessage(
                        Html.fromHtml(
                            (getString(R.string.delete_pk_message, TextUtils.htmlEncode(accountToDelete.label), valueString)
                                    + "<br/><br/>" +
                                    getString(R.string.confirm_delete_private_key_message))
                        )
                    )
                    .setView(checkBoxView)
                    .setPositiveButton(
                        R.string.yes,
                        DialogInterface.OnClickListener { arg2: DialogInterface?, arg3: Int ->
                            Log.d(TAG, "Entering onClick delete")
                            if (accountToDelete.id == localTraderManager!!.localTraderAccountId) {
                                localTraderManager!!.unsetLocalTraderAccount()
                            }

                            val potentialBalance = getPotentialBalance(accountToDelete)
                            val confirmDeleteDialog = AlertDialog.Builder(requireActivity())
                            confirmDeleteDialog.setTitle(R.string.confirm_delete_pk_title)

                            // Set the message. There are four combinations, with and without label, with and without BTC amount.
                            var label = _mbwManager!!.metadataStorage.getLabelByAccount(accountToDelete.id)
                            var labelCount = 1
                            if (!linkedAccounts.isEmpty()) {
                                label += ", " + _mbwManager!!.metadataStorage
                                    .getLabelByAccount(linkedAccounts.get(0).id)
                                labelCount++
                            }
                            val message: String?

                            // For active accounts we check whether there is money on them before deleting. we don't know if there
                            // is money on archived accounts
                            val address: String?
                            if (accountToDelete is SingleAddressAccount) {
                                address = accountToDelete.getPublicKey()
                                    ?.getAllSupportedAddresses(_mbwManager!!.network)
                                    ?.values?.joinToString("\n\n")
                            } else {
                                val receivingAddress: Address? = accountToDelete.receiveAddress
                                if (receivingAddress != null) {
                                    address = AddressUtils.toMultiLineString(receivingAddress.toString())
                                } else {
                                    address = ""
                                }
                            }
                            if (accountToDelete.isActive && potentialBalance != null && potentialBalance.moreThanZero()) {
                                if (!label.isEmpty()) {
                                    message = resources.getQuantityString(
                                        R.plurals.confirm_delete_pk_with_balance_with_label,
                                        if (accountToDelete !is SingleAddressAccount) 1 else 0,
                                        resources.getQuantityString(R.plurals.account_label, labelCount, label),
                                        address,
                                        getBalanceString(accountToDelete.coinType, accountToDelete.accountBalance)
                                    )
                                } else {
                                    message = resources.getQuantityString(
                                        R.plurals.confirm_delete_pk_with_balance,
                                        if (accountToDelete !is SingleAddressAccount) 1 else 0,
                                        getBalanceString(accountToDelete.coinType, accountToDelete.accountBalance)
                                    )
                                }
                            } else {
                                if (!label.isEmpty()) {
                                    message = resources.getQuantityString(
                                        R.plurals.confirm_delete_pk_without_balance_with_label,
                                        if (accountToDelete !is SingleAddressAccount) 1 else 0,
                                        resources.getQuantityString(R.plurals.account_label, labelCount, label),
                                        address
                                    )
                                } else {
                                    message = resources.getQuantityString(
                                        R.plurals.confirm_delete_pk_without_balance,
                                        if (accountToDelete !is SingleAddressAccount) 1 else 0, address
                                    )
                                }
                            }
                            confirmDeleteDialog
                                .setMessage(
                                    (message + "\n"
                                            + getString(R.string.confirm_delete_private_key_message_end))
                                )
                                .setPositiveButton(
                                    R.string.yes,
                                    DialogInterface.OnClickListener { arg4: DialogInterface?, arg5: Int ->
                                        Log.d(TAG, "In deleteFragment onClick")
                                        if (accountToDelete is SingleAddressAccount) {
                                            try {
                                                //Check if this SingleAddress account is related with ColuAccount
                                                val linkedColuAccount = Utils.getLinkedAccount(
                                                    accountToDelete,
                                                    walletManager!!.getAccounts()
                                                )
                                                if (linkedColuAccount is ColuAccount) {
                                                    walletManager!!.deleteAccount(linkedColuAccount.id)
                                                    walletManager!!.deleteAccount(accountToDelete.id)
                                                    val context = linkedColuAccount.context
                                                    val coluMain = linkedColuAccount.coinType as ColuMain
                                                    val config: Config = AddressColuConfig(
                                                        context.address!!.get(AddressType.P2PKH)!!,
                                                        coluMain
                                                    )
                                                    _storage!!.deleteAccountMetadata(linkedColuAccount.id)
                                                    walletManager!!.createAccounts(config)
                                                } else {
                                                    accountToDelete.forgetPrivateKey(AesKeyCipher.defaultKeyCipher())
                                                }
                                                toaster!!.toast(R.string.private_key_deleted, false)
                                            } catch (e: InvalidKeyCipher) {
                                                throw RuntimeException(e)
                                            }
                                        } else {
                                            //Check if this SingleAddress account is related with ColuAccount
                                            val linkedColuAccount =
                                                Utils.getLinkedAccount(accountToDelete, walletManager!!.getAccounts())
                                            if (linkedColuAccount is ColuAccount) {
                                                walletManager!!.deleteAccount(linkedColuAccount.id)
                                                _storage!!.deleteAccountMetadata(linkedColuAccount.id)
                                            }
                                            walletManager!!.deleteAccount(accountToDelete.id)
                                            _storage!!.deleteAccountMetadata(accountToDelete.id)
                                            _mbwManager!!.setSelectedAccount(
                                                _mbwManager!!.getWalletManager(false).getActiveSpendingAccounts()
                                                    .get(0).id
                                            )
                                            toaster!!.toast(R.string.account_deleted, false)
                                        }
                                        finishCurrentActionMode()
                                        eventBus!!.post(AccountChanged(accountToDelete.id))
                                    }).setNegativeButton(R.string.no, null)
                                .show()
                        }).setNegativeButton(R.string.no, null)
                    .create()


                deleteDialog2.setOnShowListener(DialogInterface.OnShowListener { dialogInterface: DialogInterface? ->
                    val yesButton = deleteDialog2.getButton(AlertDialog.BUTTON_POSITIVE)
                    yesButton.isEnabled = false
                    keepAddrCheckbox.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
                        yesButton.isEnabled = isChecked
                    })
                })
                deleteDialog2.show()
            })
        deleteDialog.setNegativeButton(R.string.button_cancel, null).show()
    }

    /** Removes the ERC-20 / TRC-20 accounts that belong to an ETH / TRON account being deleted. */
    private fun deleteLinkedTokenAccounts(accountToDelete: WalletAccount<*>) {
        val tokens: List<WalletAccount<*>> = when (accountToDelete) {
            is EthAccount -> getLinkedERC20Accounts(accountToDelete)
            is TronAccount -> getLinkedTRC20Accounts(accountToDelete)
            else -> emptyList()
        }
        for (token in tokens) {
            walletManager!!.deleteAccount(token.id)
            _storage!!.deleteAccountMetadata(token.id)
        }
    }

    private fun getLinkedAccounts(accountToDelete: WalletAccount<*>?): MutableList<WalletAccount<*>> {
        val linkedAccounts: MutableList<WalletAccount<*>> = ArrayList<WalletAccount<*>>()
        if (accountToDelete is EthAccount) {
            linkedAccounts.addAll(getLinkedERC20Accounts(accountToDelete))
        } else if (accountToDelete is ERC20Account) {
            linkedAccounts.add(getLinkedEthAccount(accountToDelete))
        } else if (accountToDelete is TronAccount) {
            linkedAccounts.addAll(getLinkedTRC20Accounts(accountToDelete))
        } else if (accountToDelete is TRC20Account) {
            linkedAccounts.add(accountToDelete.tronAcc)
        } else {
            if (getLinkedAccount(accountToDelete) != null) {
                linkedAccounts.add(getLinkedAccount(accountToDelete)!!)
            }
        }
        return linkedAccounts
    }

    private fun getLinkedEthAccount(account: WalletAccount<*>?): EthAccount {
        return Utils.getLinkedAccount(account, walletManager!!.getEthAccounts()) as EthAccount
    }

    private fun getLinkedERC20Accounts(account: WalletAccount<*>?): MutableList<WalletAccount<*>> {
        return Utils.getLinkedAccounts(account, walletManager!!.getERC20Accounts())
    }

    private fun getActiveLinkedERC20Accounts(account: WalletAccount<*>?): MutableList<WalletAccount<*>> {
        return Utils.getLinkedAccounts(account, walletManager!!.getActiveERC20Accounts())
    }

    private fun getLinkedTRC20Accounts(account: WalletAccount<*>?): MutableList<WalletAccount<*>> {
        return Utils.getLinkedAccounts(account, walletManager!!.getTRC20Accounts())
    }

    private fun getActiveLinkedTRC20Accounts(account: WalletAccount<*>?): MutableList<WalletAccount<*>> {
        return Utils.getLinkedAccounts(account, walletManager!!.getActiveTRC20Accounts())
    }

    private fun createDeleteDialogText(
        accountToDelete: WalletAccount<*>,
        linkedAccounts: MutableList<WalletAccount<*>>
    ): String {
        // the dialog text is rendered as HTML, the label is user input
        val accountName = TextUtils.htmlEncode(_mbwManager!!.metadataStorage.getLabelByAccount(accountToDelete.id))
        val dialogText: String

        if (accountToDelete.isActive) {
            dialogText = getActiveAccountDeleteText(accountToDelete, linkedAccounts, accountName)
        } else {
            dialogText = getArchivedAccountDeleteText(linkedAccounts, accountName)
        }
        return dialogText
    }

    private fun getArchivedAccountDeleteText(
        linkedAccounts: MutableList<WalletAccount<*>>,
        accountName: String?
    ): String {
        val dialogText: String
        if (linkedAccounts.size > 1) {
            dialogText = getString(R.string.delete_archived_account_message_s, accountName)
        } else if (!linkedAccounts.isEmpty() && linkedAccounts.get(0).isVisible()) {
            val linkedAccountName = TextUtils.htmlEncode(_mbwManager!!.metadataStorage.getLabelByAccount(linkedAccounts.get(0).id))
            dialogText = getString(R.string.delete_archived_account_message, accountName, linkedAccountName)
        } else {
            dialogText = getString(R.string.delete_archived_account_message_s, accountName)
        }
        return dialogText
    }

    private fun getActiveAccountDeleteText(
        accountToDelete: WalletAccount<*>,
        linkedAccounts: MutableList<WalletAccount<*>>,
        accountName: String?
    ): String {
        val dialogText: String
        val balance = Preconditions.checkNotNull<Balance>(accountToDelete.accountBalance)
        val valueString = getBalanceString(accountToDelete.coinType, balance)

        // TODO sort linkedAccounts for visible only
        if (linkedAccounts.size > 1 || (accountToDelete is EthAccount || accountToDelete is TronAccount) && linkedAccounts.size > 0) {
            val linkedAccountStrings: MutableList<String?> = ArrayList<String?>()
            for (linkedAccount in linkedAccounts) {
                val linkedBalance = linkedAccount.accountBalance
                val linkedAccountName = TextUtils.htmlEncode(_mbwManager!!.metadataStorage.getLabelByAccount(linkedAccount.id))
                val linkedValueString = getBalanceString(linkedAccount.coinType, linkedBalance)
                linkedAccountStrings.add("<b>" + linkedAccountName + "</b> holding <b>" + linkedValueString + "</b>")
            }
            val linkedAccountsString = TextUtils.join(", ", linkedAccountStrings) + "?"
            dialogText = getString(
                R.string.delete_accounts_message, accountName, valueString,
                linkedAccountsString
            ) + "\n" + getString(R.string.both_eth_and_tokens_will_deleted, accountName)
        } else if (!linkedAccounts.isEmpty() && linkedAccounts.get(0)
                .isVisible() && (accountToDelete !is ERC20Account) && (accountToDelete !is TRC20Account)
        ) {
            val linkedBalance = linkedAccounts.get(0).accountBalance
            val linkedValueString = getBalanceString(linkedAccounts.get(0).coinType, linkedBalance)
            val linkedAccountName = TextUtils.htmlEncode(_mbwManager!!.metadataStorage.getLabelByAccount(linkedAccounts.get(0).id))
            dialogText = getString(
                R.string.delete_account_message, accountName, valueString,
                linkedAccountName, linkedValueString
            ) + "\n" +
                    getString(R.string.both_rmc_will_deleted, accountName, linkedAccountName)
        } else {
            dialogText = getString(R.string.delete_account_message_s, accountName, valueString)
        }
        return dialogText
    }

    private fun getBalanceString(coinType: AssetInfo?, balance: Balance): String {
        return balance.spendable.toStringFriendlyWithUnit(_mbwManager!!.getDenomination(coinType))
    }

    /**
     * If account is colu we are asking for linked BTC. Else we are searching if any colu attached.
     */
    private fun getLinkedAccount(account: WalletAccount<*>?): WalletAccount<*>? {
        return Utils.getLinkedAccount(account, walletManager!!.getAccounts())
    }

    private fun finishCurrentActionMode() {
        if (currentActionMode != null) {
            currentActionMode!!.finish()
        }
    }

    private fun update() {
        if (!isAdded) {
            return
        }

        if (_mbwManager!!.isKeyManagementLocked) {
            // Key management is locked
            binding?.rvRecords?.visibility = View.GONE
            binding?.llLocked?.visibility = View.VISIBLE
        } else {
            // Make all the key management functionality available to experts
            binding?.rvRecords!!.visibility = View.VISIBLE
            binding?.llLocked?.visibility = View.GONE
        }
        eventBus!!.post(AccountListChanged())
    }

    private var currentActionMode: ActionMode? = null

    // The account the current action mode was opened for. Menu actions must act on it even when the
    // adapter's focus points elsewhere (e.g. at an account that was just deleted).
    private var actionModeAccount: WalletAccount<out Address>? = null

    private val recordAddressClickListener: AccountListAdapter.ItemClickListener =
        object : AccountListAdapter.ItemClickListener {
            override fun onItemClick(account: WalletAccount<*>) {
                // Check whether a new account was selected
                if (_mbwManager!!.getSelectedAccount() != account && account.isActive) {
                    _mbwManager!!.setSelectedAccount(account.id)
                }
                toastSelectedAccountChanged(account)
                updateIncludingMenus()
            }
        }

    private fun updateIncludingMenus() {
        // the newly focused item, not the account of a still-open action mode
        val account = accountListAdapter!!.focusedAccount ?: return
        val isBch = account is SingleAddressBCHAccount
                || account is Bip44BCHAccount

        val menus = mutableListOf<Int>()
        if (account !is ColuAccount && !Utils.checkIsLinked(account, walletManager!!.getColuAccounts())) {
            menus.add(R.menu.record_options_menu)
        }

        if (account is SingleAddressAccount || account.isDerivedFromInternalMasterseed()) {
            menus.add(R.menu.record_options_menu_backup)
        }

        if (account is SingleAddressAccount) {
            menus.add(R.menu.record_options_menu_backup_verify)
        }

        if (account is ColuAccount) {
            //TODO: distinguish between ColuAccount in single address mode and HD mode
            menus.add(R.menu.record_options_menu_backup)
            menus.add(R.menu.record_options_menu_backup_verify)
        }

        if (_mbwManager!!.isAccountCanBeDeleted(account)) {
            menus.add(R.menu.record_options_menu_delete)
        }

        if (account.isActive && account.canSpend() && !isBch && account.canSign()) {
            menus.add(R.menu.record_options_menu_sign)
        }

        if (account.isActive && !isBch) {
            menus.add(R.menu.record_options_menu_active)
        }

        if (account.isActive && !isBch && (account !is AbstractEthERC20Account) && (account !is AbstractTronAccount)) {
            menus.add(R.menu.record_options_menu_outputs)
        }

        if ((account !is Bip44BCHAccount) && (account !is SingleAddressBCHAccount) && account.isArchived) {
            menus.add(R.menu.record_options_menu_archive)
        }

        if (account.isActive && account is ExportableAccount && !isBch) {
            menus.add(R.menu.record_options_menu_export)
        }

        if (account is SingleAddressAccount && account.canSpend()) {
            menus.add(R.menu.record_options_menu_shamir)
        }

        if (account.isActive && account is HDAccount && (account !is HDPubOnlyAccount) && walletManager!!.getActiveMasterseedHDAccounts().size > 1 && !isBch) {
            val bitcoinHDModule = walletManager!!.getModuleById(BitcoinHDModule.ID) as BitcoinHDModule?
            if (!account.hasHadActivity() && account.accountIndex == bitcoinHDModule!!.getCurrentBip44Index()) {
                //only allow to remove unused HD accounts from the view
                menus.add(R.menu.record_options_menu_hide_unused)
            }
        }

        if (account.isActive && account is HDAccount) {
            menus.add(R.menu.record_options_boost_gap)
        }

        if (account.id == _mbwManager!!.localTraderManager.localTraderAccountId) {
            menus.add(R.menu.record_options_menu_detach)
        }

        val parent = requireActivity() as AppCompatActivity

        actionModeAccount = account
        currentActionMode = parent.startSupportActionMode(
            AccountsActionModeCallback(
                requireContext(),
                menus,
                _mbwManager!!,
                account,
                { action: Runnable? ->
                    runPinProtected(action!!)
                    Unit
                },
                label@{ itemId: Int? ->
                    when (itemId) {
                        R.id.miActivate -> {
                            activateSelected()
                            return@label true
                        }

                        R.id.miSetLabel -> {
                            setLabelOnAccount(accountListAdapter!!.focusedAccount, "", true)
                            return@label true
                        }

                        R.id.miDropPrivateKey -> {
                            dropPrivateKey()
                            return@label true
                        }

                        R.id.miDeleteRecord -> {
                            deleteSelectedAccount()
                            return@label true
                        }

                        R.id.miArchive -> {
                            archiveSelected()
                            return@label true
                        }

                        R.id.miHideUnusedAccount -> {
                            hideSelected()
                            return@label true
                        }

                        R.id.miExport -> {
                            exportSelectedPrivateKey()
                            return@label true
                        }

                        R.id.miSignMessage -> {
                            signMessage()
                            return@label true
                        }

                        R.id.miDetach -> {
                            detachFromLocalTrader()
                            return@label true
                        }

                        R.id.miMakeBackup -> {
                            makeBackup()
                            return@label true
                        }

                        R.id.miSingleKeyBackupVerify -> {
                            verifySingleKeyBackup()
                            return@label true
                        }

                        R.id.miBoostGap -> {
                            this@AccountsFragment.boostGapLimitDialog(_mbwManager!!, account)
                            return@label true
                        }
                    }
                    false
                },
                {
                    currentActionMode = null
                    actionModeAccount = null
                    // also clear a focus that points at a deleted account, otherwise it stays stale
                    accountListAdapter!!.setFocusedAccountId(null)
                    Unit
                })
        )

        // Late set the focused record. We have to do this after
        // startSupportActionMode above, as it calls onDestroyActionMode when
        // starting for some reason, and this would clear the focus and force
        // an update.
        accountListAdapter!!.setFocusedAccountId(account.id)
    }

    private fun verifySingleKeyBackup() {
        if (!isAdded) {
            return
        }
        val account = requireFocusedAccount()
        account.interruptSync()
        if (account is SingleAddressAccount || account is ColuAccount) {
            //start legacy backup verification
            VerifyBackupActivity.callMe(activity)
        }
    }

    private fun makeBackup() {
        if (!isAdded) {
            return
        }
        val account = requireFocusedAccount()
        account.interruptSync()
        if (account is ColuAccount) {
            //ColuAccount class can be single or HD
            //TODO: test if account is single address or HD and do wordlist backup instead
            //start legacy backup if a single key or watch only was selected
            Utils.pinProtectedBackup(activity)
        } else {
            if (account.isDerivedFromInternalMasterseed()) {
                //start wordlist backup if a HD account or derived account was selected
                Utils.pinProtectedWordlistBackup(activity)
            } else if (account is SingleAddressAccount) {
                //start legacy backup if a single key or watch only was selected
                Utils.pinProtectedBackup(activity)
            }
        }
    }

    private fun signMessage() {
        if (!isAdded) {
            return
        }
        runPinProtected(Runnable {
            val account = accountListAdapter!!.focusedAccount
            account!!.interruptSync()
            MessageSigningActivity.callMe(requireContext(), account)
        })
    }

    /**
     * Show a message to the user explaining what it means to select a different
     * address.
     */
    private fun toastSelectedAccountChanged(account: WalletAccount<*>) {
        if (account.isArchived) {
            toaster!!.toast(getString(R.string.selected_archived_warning), true)
        } else if (account is HDAccount) {
            toaster!!.toast(getString(R.string.selected_hd_info), true)
        } else if (account is SingleAddressAccount) {
            toaster!!.toast(getString(R.string.selected_single_info), true)
        } else if (account is ColuAccount) {
            toaster!!.toast(
                getString(
                    R.string.selected_colu_info,
                    _mbwManager!!.metadataStorage.getLabelByAccount(account.id)
                ), true
            )
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (!isAdded) {
            return true
        }
        if (item.itemId == R.id.miAddRecord || item.itemId == R.id.miAddRecordDuplicate) {
            AddAccountActivity.callMe(this, ADD_RECORD_RESULT_CODE)
            return true
        } else if (item.itemId == R.id.miLockKeys) {
            lock()
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    private fun setLabelOnAccount(account: WalletAccount<*>?, defaultName: String?, askForPin: Boolean) {
        if (account == null || !isAdded) {
            return
        }
        if (askForPin) {
            runPinProtected(Runnable {
                EnterAddressLabelUtil.enterAccountLabel(
                    requireActivity(),
                    account.id,
                    defaultName,
                    _storage
                )
            })
        } else {
            EnterAddressLabelUtil.enterAccountLabel(requireActivity(), account.id, defaultName, _storage)
        }
    }

    private fun deleteSelectedAccount() {
        if (!isAdded) {
            return
        }
        val account = requireFocusedAccount()
        if (account.isActive && accountProtected(account)) {
            toaster!!.toast(R.string.keep_one_active, false)
            return
        }
        runPinProtected(Runnable { deleteAccount(account) })
    }

    private fun dropPrivateKey() {
        if (!isAdded) {
            return
        }
        val account = requireFocusedAccount()
        if (account.isActive && accountProtected(account)) {
            toaster!!.toast(R.string.keep_one_active, false)
            return
        }
        runPinProtected(Runnable { makeWatchOnly(account) })
    }

    private fun exportSelectedPrivateKey() {
        if (!isAdded) {
            return
        }
        runPinProtected(Runnable { Utils.exportSelectedAccount(activity) })
    }

    private fun detachFromLocalTrader() {
        if (!isAdded) {
            return
        }
        val ltm = _mbwManager!!.localTraderManager
        val hasLt = ltm.hasLocalTraderAccount()
        if (!hasLt) {
            toaster!!.toast("No LT configured.", true)
            return
        }
        runPinProtected(Runnable {
            AlertDialog.Builder(requireActivity())
                .setTitle(R.string.lt_detaching_title)
                .setMessage(getString(R.string.lt_detaching_question))
                .setPositiveButton(R.string.yes, DialogInterface.OnClickListener { arg0: DialogInterface?, arg1: Int ->
                    val wa = walletManager!!.getAccount(ltm.localTraderAccountId)
                    wa!!.interruptSync()
                    ltm.unsetLocalTraderAccount()
                    toaster!!.toast(R.string.lt_detached, false)
                    update()
                })
                .setNegativeButton(R.string.no, null)
                .show()
        })
    }

    private fun activateSelected() {
        if (!isAdded) {
            return
        }
        // resolve before the PIN dialog: the action mode (and its account) may be gone afterwards
        val account = requireFocusedAccount()
        runPinProtected(Runnable { activate(account) })
    }

    private fun activate(account: WalletAccount<*>) {
        val accountsToActivateAndSync: MutableList<WalletAccount<*>> = ArrayList<WalletAccount<*>>()
        accountsToActivateAndSync.add(account)
        if (account is EthAccount) {
            for (walletAccount in getLinkedERC20Accounts(account)) {
                accountsToActivateAndSync.add(walletAccount)
            }
        } else if (account is ERC20Account) {
            val ethAccount = getLinkedEthAccount(account)
            if (ethAccount.isArchived) {
                accountsToActivateAndSync.add(ethAccount)
            }
        } else if (account is TronAccount) {
            for (walletAccount in getLinkedTRC20Accounts(account)) {
                accountsToActivateAndSync.add(walletAccount)
            }
        } else if (account is TRC20Account) {
            if (account.tronAcc.isArchived) {
                accountsToActivateAndSync.add(account.tronAcc)
            }
        } else {
            val linkedAccount = Utils.getLinkedAccount(account, walletManager!!.getAccounts())
            if (linkedAccount != null) {
                accountsToActivateAndSync.add(linkedAccount)
            }
        }
        for (wa in accountsToActivateAndSync) {
            wa.activateAccount()
        }
        //setselected also broadcasts AccountChanged event
        _mbwManager!!.setSelectedAccount(account.id)
        updateIncludingMenus()
        toaster!!.toast(R.string.activated, false)
        _mbwManager!!.getWalletManager(false)
            .startSynchronization(SyncMode.NORMAL_FORCED, accountsToActivateAndSync)
    }

    private fun archiveSelected() {
        if (!isAdded) {
            return
        }
        val account = requireFocusedAccount()
        if (accountProtected(account)) {
            //this is the last active hd account, we dont allow archiving it
            toaster!!.toast(R.string.keep_one_active, false)
            return
        }
        if (account is HDAccount) {
            val hdAccount = account
            if (!hdAccount.hasHadActivity() && hdAccount.isDerivedFromInternalMasterseed()) {
                // this hdAccount is unused, we don't allow archiving it
                toaster!!.toast(R.string.dont_allow_archiving_unused_notification, false)
                return
            }
        }
        runPinProtected(Runnable { archive(account) })
    }

    /**
     * Account is protected if after removal no masterseed accounts of the same coin type would stay active,
     * so it would not be possible to select an account
     */
    private fun accountProtected(toRemove: WalletAccount<*>): Boolean {
        // accounts not derived from master seed and ethereum account are not protected
        if (!(toRemove.isDerivedFromInternalMasterseed() && toRemove is HDAccount) || toRemove is EthAccount || toRemove is TronAccount) {
            return false
        }
        val accountsList = _mbwManager!!.getWalletManager(false).getActiveMasterseedAccounts()
        var cnt = 0
        for (account in accountsList) {
            if (account.javaClass == toRemove.javaClass) {
                cnt++
            }
        }
        return cnt <= 1
    }

    private fun hideSelected() {
        if (!isAdded) {
            return
        }
        val account = requireFocusedAccount()
        if (accountProtected(account)) {
            //this is the last active account, we dont allow hiding it
            toaster!!.toast(R.string.keep_one_active, false)
            return
        }
        if (account is HDAccount) {
            val hdAccount = account
            if (hdAccount.hasHadActivity() && hdAccount.isDerivedFromInternalMasterseed()) {
                // this hdAccount is used, we don't allow hiding it
                toaster!!.toast(R.string.dont_allow_hiding_used_notification, false)
                return
            }

            runPinProtected(Runnable {
                hdAccount.interruptSync()
                _mbwManager!!.getWalletManager(false).deleteAccount(hdAccount.id)
                // in case user had labeled the account, delete the stored name
                _storage!!.deleteAccountMetadata(hdAccount.id)
                eventBus!!.post(AccountChanged(hdAccount.id))
                _mbwManager!!.setSelectedAccount(
                    _mbwManager!!.getWalletManager(false).getActiveSpendingAccounts().get(0).id
                )
                //we dont want to show the context menu for the automatically selected account
                accountListAdapter!!.setFocusedAccountId(null)
                finishCurrentActionMode()
            })
        }
    }

    private fun archive(account: WalletAccount<*>) {
        val linkedAccounts: MutableList<WalletAccount<*>> = ArrayList<WalletAccount<*>>()
        if (account is EthAccount) {
            if (!getActiveLinkedERC20Accounts(account).isEmpty()) {
                linkedAccounts.addAll(getActiveLinkedERC20Accounts(account))
            }
        } else if (account is TronAccount) {
            if (!getActiveLinkedTRC20Accounts(account).isEmpty()) {
                linkedAccounts.addAll(getActiveLinkedTRC20Accounts(account))
            }
        } else if (account !is ERC20Account && account !is TRC20Account) {
            if (getLinkedAccount(account) != null) {
                linkedAccounts.add(getLinkedAccount(account)!!)
            }
        }

        val accountsToInterrupt: MutableCollection<WalletAccount<*>> = HashSet<WalletAccount<*>>()
        accountsToInterrupt.add(account)
        accountsToInterrupt.addAll(linkedAccounts)
        interruptSync(accountsToInterrupt)

        AlertDialog.Builder(requireActivity())
            .setTitle(R.string.archiving_account_title)
            .setMessage(Html.fromHtml(createArchiveDialogText(account, linkedAccounts)))
            .setPositiveButton(R.string.yes) { arg0, arg1 ->
                account.archiveAccount()
                if (account is EthAccount) {
                    for (walletAccount in getLinkedERC20Accounts(account)) {
                        walletAccount.archiveAccount()
                    }
                } else if (account is TronAccount) {
                    for (walletAccount in getLinkedTRC20Accounts(account)) {
                        walletAccount.archiveAccount()
                    }
                } else if (account !is ERC20Account && account !is TRC20Account) {
                    val linkedAccount = Utils.getLinkedAccount(account, walletManager!!.getAccounts())
                    linkedAccount?.archiveAccount()
                }
                _mbwManager!!.setSelectedAccount(
                    _mbwManager!!.getWalletManager(false).getActiveSpendingAccounts().get(0).id
                )
                eventBus!!.post(AccountChanged(account.id))
                if (!linkedAccounts.isEmpty()) {
                    for (linkedAccount in linkedAccounts) {
                        eventBus!!.post(AccountChanged(linkedAccount.id))
                    }
                }
                updateIncludingMenus()
                toaster!!.toast(R.string.archived, false)
            }
            .setNegativeButton(R.string.no, null)
            .show()
    }

    private fun createArchiveDialogText(
        account: WalletAccount<*>,
        linkedAccounts: MutableList<WalletAccount<*>>
    ): String {
        // the dialog text is rendered as HTML, the label is user input
        val accountName = TextUtils.htmlEncode(_mbwManager!!.metadataStorage.getLabelByAccount(account.id))
        return getAccountArchiveText(account, linkedAccounts, accountName)
    }

    private fun getAccountArchiveText(
        account: WalletAccount<*>,
        linkedAccounts: MutableList<WalletAccount<*>>,
        accountName: String?
    ): String {
        val dialogText: String
        val balance = Preconditions.checkNotNull<Balance>(account.accountBalance)
        val valueString = getBalanceString(account.coinType, balance)

        // TODO sort linkedAccounts for visible only
        if (linkedAccounts.size > 1 || ((account is EthAccount || account is TronAccount) && linkedAccounts.size > 0)) {
            val linkedAccountStrings: MutableList<String?> = ArrayList<String?>()
            for (linkedAccount in linkedAccounts) {
                val linkedBalance = linkedAccount.accountBalance
                val linkedAccountName = TextUtils.htmlEncode(_mbwManager!!.metadataStorage.getLabelByAccount(linkedAccount.id))
                val linkedValueString = getBalanceString(linkedAccount.coinType, linkedBalance)
                linkedAccountStrings.add("<b>" + linkedAccountName + "</b> holding <b>" + linkedValueString + "</b>")
            }
            val linkedAccountsString = TextUtils.join(", ", linkedAccountStrings) + "?"
            dialogText =
                getString(R.string.question_archive_many_accounts, accountName, valueString, linkedAccountsString)
        } else if (!linkedAccounts.isEmpty() && linkedAccounts.get(0).isVisible()) {
            val linkedBalance = linkedAccounts.get(0).accountBalance
            val linkedValueString = getBalanceString(linkedAccounts.get(0).coinType, linkedBalance)
            val linkedAccountName = TextUtils.htmlEncode(_mbwManager!!.metadataStorage.getLabelByAccount(linkedAccounts.get(0).id))
            dialogText = getString(
                R.string.question_archive_account_s, accountName, valueString,
                linkedAccountName, linkedValueString
            )
        } else {
            dialogText = getString(R.string.question_archive_account, accountName, valueString)
        }
        return dialogText
    }

    private fun lock() {
        _mbwManager!!.setKeyManagementLocked(true)
        update()
        if (isAdded) {
            requireActivity().invalidateOptionsMenu()
        }
    }

    private fun runPinProtected(runnable: Runnable) {
        _mbwManager!!.runPinProtectedFunction(requireActivity(), object : Runnable {
            override fun run() {
                if (!isAdded) {
                    return
                }
                runnable.run()
            }
        })
    }

    var unlockClickedListener: View.OnClickListener = View.OnClickListener {
        _mbwManager!!.runPinProtectedFunction(activity) {
            _mbwManager!!.setKeyManagementLocked(false)
            update()
            if (isAdded) {
                requireActivity().invalidateOptionsMenu()
            }
        }
    }

    private fun requireFocusedAccount(): WalletAccount<out Address> =
        actionModeAccount?.takeIf { walletManager!!.getAccount(it.id) != null }
            ?: accountListAdapter!!.focusedAccount!!

    @Subscribe
    fun addressChanged(event: ReceivingAddressChanged?) {
        update()
    }

    @Subscribe
    fun balanceChanged(event: BalanceChanged?) {
        update()
    }

    @Subscribe
    fun syncFailed(event: SyncFailed?) {
        update()
    }

    @Subscribe
    fun syncStarted(event: SyncStarted?) {
        update()
    }

    @Subscribe
    fun syncStopped(event: SyncStopped?) {
        update()
    }

    @Subscribe
    fun accountChanged(event: AccountChanged?) {
        update()
    }

    @Subscribe
    fun syncProgressUpdated(event: SyncProgressUpdated?) {
        update()
    }

    @Subscribe
    fun exchangeSourceChange(event: ExchangeSourceChanged?) {
        accountListAdapter!!.notifyDataSetChanged()
    }

    @Subscribe
    fun exchangeRatesRefreshed(event: ExchangeRatesRefreshed?) {
        accountListAdapter!!.notifyDataSetChanged()
    }

    @Subscribe
    fun selectedCurrencyChanged(event: SelectedCurrencyChanged?) {
        accountListAdapter!!.notifyDataSetChanged()
    }

    private val ltSubscriber: LocalTraderEventSubscriber = object : LocalTraderEventSubscriber(Handler()) {
        override fun onLtError(errorCode: Int) {
        }

        override fun onLtAccountDeleted(request: DeleteTrader?) {
            accountListAdapter!!.notifyDataSetChanged()
        }

        override fun onLtTraderCreated(request: CreateTrader?) {
            accountListAdapter!!.notifyDataSetChanged()
        }
    }

    internal inner class MenuImpl : MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.record_options_menu_global, menu)
        }

        override fun onPrepareMenu(menu: Menu) {
            super.onPrepareMenu(menu)
            val locked = _mbwManager?.isKeyManagementLocked == true
            menu.findItem(R.id.miAddRecord).isVisible = !locked
            menu.findItem(R.id.miAddRecordDuplicate).isVisible = !locked
            val hasPin = _mbwManager?.isPinProtected == true
            menu.findItem(R.id.miLockKeys).isVisible = !locked && hasPin
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean =
            when (menuItem.itemId) {
                R.id.miAddRecord, R.id.miAddRecordDuplicate -> {
                    AddAccountActivity.callMe(this@AccountsFragment, ADD_RECORD_RESULT_CODE)
                    true
                }

                R.id.miLockKeys -> {
                    lock()
                    true
                }

                else -> {
                    false
                }
            }
    }

    companion object {
        const val ADD_RECORD_RESULT_CODE: Int = 0

        const val TAG: String = "AccountsFragment"
    }
}
