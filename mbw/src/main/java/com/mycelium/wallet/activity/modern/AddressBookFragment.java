/*
 * Copyright 2013, 2014 Megion Research and Development GmbH
 *
 * Licensed under the Microsoft Reference Source License (MS-RSL)
 *
 * This license governs use of the accompanying software. If you use the software, you accept this license.
 * If you do not accept the license, do not use the software.
 *
 * 1. Definitions
 * The terms "reproduce," "reproduction," and "distribution" have the same meaning here as under U.S. copyright law.
 * "You" means the licensee of the software.
 * "Your company" means the company you worked for when you downloaded the software.
 * "Reference use" means use of the software within your company as a reference, in read only form, for the sole purposes
 * of debugging your products, maintaining your products, or enhancing the interoperability of your products with the
 * software, and specifically excludes the right to distribute the software outside of your company.
 * "Licensed patents" means any Licensor patent claims which read directly on the software as distributed by the Licensor
 * under this license.
 *
 * 2. Grant of Rights
 * (A) Copyright Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 * worldwide, royalty-free copyright license to reproduce the software for reference use.
 * (B) Patent Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 * worldwide, royalty-free patent license under licensed patents for reference use.
 *
 * 3. Limitations
 * (A) No Trademark License- This license does not grant you any rights to use the Licensor’s name, logo, or trademarks.
 * (B) If you begin patent litigation against the Licensor over patents that you think may apply to the software
 * (including a cross-claim or counterclaim in a lawsuit), your license to the software ends automatically.
 * (C) The software is licensed "as-is." You bear the risk of using it. The Licensor gives no express warranties,
 * guarantees or conditions. You may have additional consumer rights under your local laws which this license cannot
 * change. To the extent permitted under your local laws, the Licensor excludes the implied warranties of merchantability,
 * fitness for a particular purpose and non-infringement.
 */

package com.mycelium.wallet.activity.modern;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.Toast;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.mrd.bitlib.model.Address;
import com.mycelium.wallet.AddressBookManager;
import com.mycelium.wallet.AddressBookManager.Entry;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.activity.ScanActivity;
import com.mycelium.wallet.activity.StringHandlerActivity;
import com.mycelium.wallet.activity.modern.adapter.AddressBookAdapter;
import com.mycelium.wallet.activity.modern.adapter.SelectAssetDialog;
import com.mycelium.wallet.activity.receive.ReceiveCoinsActivity;
import com.mycelium.wallet.activity.util.EnterAddressLabelUtil;
import com.mycelium.wallet.activity.util.EnterAddressLabelUtil.AddressLabelChangedHandler;
import com.mycelium.wallet.content.HandleConfigFactory;
import com.mycelium.wallet.content.ResultType;
import com.mycelium.wallet.content.StringHandleConfig;
import com.mycelium.wallet.event.AddressBookChanged;
import com.mycelium.wallet.event.AssetSelected;
import com.mycelium.wapi.wallet.AddressUtils;
import com.mycelium.wapi.wallet.GenericAddress;
import com.mycelium.wapi.wallet.WalletAccount;
import com.mycelium.wapi.wallet.btc.BtcAddress;
import com.mycelium.wapi.wallet.btc.coins.BitcoinMain;
import com.mycelium.wapi.wallet.btc.coins.BitcoinTest;
import com.mycelium.wapi.wallet.coinapult.CoinapultAccount;
import com.squareup.otto.Subscribe;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.mycelium.wallet.activity.util.IntentExtentionsKt.getAddress;
import static com.mycelium.wallet.activity.util.IntentExtentionsKt.getAssetUri;

public class AddressBookFragment extends Fragment {
    public static final int SCAN_RESULT_CODE = 0;
    public static final String ADDRESS_RESULT_NAME = "address_result";
    public static final String ADDRESS_RESULT_ID = "address_result_id";
    public static final String OWN = "own";
    public static final String AVAILABLE_FOR_SENDING = "is_sending";
    public static final String SELECT_ONLY = "selectOnly";
    public static final String ADDRESS_RESULT_LABEL = "address_result_label";

    private GenericAddress mSelectedAddress;
    private MbwManager _mbwManager;
    private Dialog _addDialog;
    private ActionMode currentActionMode;
    private Boolean ownAddresses; // set to null on purpose
    private Boolean availableForSendingAddresses;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View ret = Preconditions.checkNotNull(inflater.inflate(R.layout.address_book, container, false));
        ownAddresses = getArguments().getBoolean(OWN);
        availableForSendingAddresses = getArguments().getBoolean(AVAILABLE_FOR_SENDING);
        boolean isSelectOnly = getArguments().getBoolean(SELECT_ONLY);
        setHasOptionsMenu(!isSelectOnly);
        ListView foreignList = ret.findViewById(R.id.lvForeignAddresses);
        if (isSelectOnly) {
            foreignList.setOnItemClickListener(new SelectItemListener());
        } else {
            foreignList.setOnItemClickListener(itemListClickListener);
        }
        return ret;
    }

    private View findViewById(int id) {
        return getView().findViewById(id);
    }

    @Override
    public void onAttach(Context context) {
        _mbwManager = MbwManager.getInstance(context);
        super.onAttach(context);
    }

    @Override
    public void onResume() {
        MbwManager.getEventBus().register(this);
        updateUi();
        super.onResume();
    }

    @Override
    public void onPause() {
        MbwManager.getEventBus().unregister(this);
        super.onPause();
    }

    @Override
    public void onDestroy() {
        if (_addDialog != null && _addDialog.isShowing()) {
            _addDialog.dismiss();
        }
        super.onDestroy();
    }

    private void updateUi() {
        if (!isAdded()) {
            return;
        }
        if (ownAddresses) {
            updateUiMine();
        } else {
            if(availableForSendingAddresses){
                updateUiSending();
            } else {
                updateUiForeign();
            }
        }
    }

    private void updateUiMine() {
        List<Entry> entries = new ArrayList<>();

        List<WalletAccount<?, ?>> activeAccounts = new ArrayList<>(_mbwManager.getWalletManager(false).getAllActiveAccounts());
        for (WalletAccount account : Utils.sortAccounts(activeAccounts, _mbwManager.getMetadataStorage())) {
            String name = _mbwManager.getMetadataStorage().getLabelByAccount(account.getId());
            Drawable drawableForAccount = Utils.getDrawableForAccount(account, true, getResources());
            //TODO a lot of pr
            WalletAccount selectedAccount = _mbwManager.getSelectedAccount();
            if (account.getReceiveAddress() != null) {
                if (selectedAccount instanceof CoinapultAccount
                        && (account instanceof CoinapultAccount || account.getCoinType() == BitcoinMain.get() || account.getCoinType() == BitcoinTest.get())) {
                    entries.add(new AddressBookManager.IconEntry(account.getReceiveAddress(), name, drawableForAccount, account.getId()));
                } else if ((selectedAccount.getCoinType().equals(BitcoinMain.get()) || selectedAccount.getCoinType().equals(BitcoinTest.get()))
                        && account instanceof CoinapultAccount) {
                    entries.add(new AddressBookManager.IconEntry(account.getReceiveAddress(), name, drawableForAccount, account.getId()));
                } else if (selectedAccount.getCoinType().equals(account.getCoinType())) {
                    entries.add(new AddressBookManager.IconEntry(account.getReceiveAddress(), name, drawableForAccount, account.getId()));
                }
            }
        }
        if (entries.isEmpty()) {
            findViewById(R.id.tvNoRecords).setVisibility(View.VISIBLE);
            findViewById(R.id.lvForeignAddresses).setVisibility(View.GONE);
        } else {
            findViewById(R.id.tvNoRecords).setVisibility(View.GONE);
            findViewById(R.id.lvForeignAddresses).setVisibility(View.VISIBLE);
            ListView list = (ListView) findViewById(R.id.lvForeignAddresses);
            list.setAdapter(new AddressBookAdapter(getActivity(), R.layout.address_book_my_address_row, entries));
        }
    }

    private void updateUiForeign() {
        Map<Address, String> rawentries = _mbwManager.getMetadataStorage().getAllAddressLabels();
        List<Entry> entries = new ArrayList<Entry>();
        for (Map.Entry<Address, String> e : rawentries.entrySet()) {
            entries.add(new Entry(AddressUtils.fromAddress(e.getKey()), e.getValue()));
        }
        entries = Utils.sortAddressbookEntries(entries);
        if (entries.isEmpty()) {
            findViewById(R.id.tvNoRecords).setVisibility(View.VISIBLE);
            findViewById(R.id.lvForeignAddresses).setVisibility(View.GONE);
        } else {
            findViewById(R.id.tvNoRecords).setVisibility(View.GONE);
            findViewById(R.id.lvForeignAddresses).setVisibility(View.VISIBLE);
            ListView foreignList = (ListView) findViewById(R.id.lvForeignAddresses);
            foreignList.setAdapter(new AddressBookAdapter(getActivity(), R.layout.address_book_foreign_row, entries));
        }
    }

    private void updateUiSending() {
        List<GenericAddress> addresses = _mbwManager.getMetadataStorage().getAllGenericAddress();
        Map<Address, String> rawentries = _mbwManager.getMetadataStorage().getAllAddressLabels();
        List<Entry> entries = new ArrayList<>();
        WalletAccount account = _mbwManager.getSelectedAccount();
        for (GenericAddress address : addresses) {
            if (address.getCoinType().equals(account.getCoinType())) {
                entries.add(new Entry(address, rawentries.get(Address.fromString(address.toString()))));
            }
        }
        entries = Utils.sortAddressbookEntries(entries);
        if (entries.isEmpty()) {
            findViewById(R.id.tvNoRecords).setVisibility(View.VISIBLE);
            findViewById(R.id.lvForeignAddresses).setVisibility(View.GONE);
        } else {
            findViewById(R.id.tvNoRecords).setVisibility(View.GONE);
            findViewById(R.id.lvForeignAddresses).setVisibility(View.VISIBLE);
            ListView foreignList = (ListView) findViewById(R.id.lvForeignAddresses);
            foreignList.setAdapter(new AddressBookAdapter(getActivity(), R.layout.address_book_sending_row, entries));
        }
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (!isVisibleToUser) {
            finishActionMode();
        }
    }

    private void finishActionMode() {
        if (currentActionMode != null) {
            currentActionMode.finish();
        }
    }

    OnItemClickListener itemListClickListener = new OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> listView, final View view, int position, long id) {
            mSelectedAddress = (GenericAddress) view.getTag();
            AppCompatActivity parent = (AppCompatActivity) getActivity();
            currentActionMode = parent.startSupportActionMode(new ActionMode.Callback() {
                @Override
                public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
                    actionMode.getMenuInflater().inflate(R.menu.addressbook_context_menu, menu);
                    return true;
                }

                @SuppressWarnings("deprecation")
                @Override
                public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
                    currentActionMode = actionMode;
                    view.setBackgroundDrawable(getResources().getDrawable(R.color.selectedrecord));
                    return true;
                }

                @Override
                public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
                    final int item = menuItem.getItemId();
                    if (item == R.id.miDeleteAddress) {
                        _mbwManager.runPinProtectedFunction(getActivity(), pinProtectedDeleteEntry);
                        return true;
                    } else if (item == R.id.miEditAddress) {
                        _mbwManager.runPinProtectedFunction(getActivity(), pinProtectedEditEntry);
                        return true;
                    } else if (item == R.id.miShowQrCode) {
                        doShowQrCode();
                        return true;
                    }
                    return false;
                }

                @SuppressWarnings("deprecation")
                @Override
                public void onDestroyActionMode(ActionMode actionMode) {
                    view.setBackgroundDrawable(null);
                    currentActionMode = null;
                }
            });
        }
    };

    final Runnable pinProtectedEditEntry = new Runnable() {
        @Override
        public void run() {
            doEditEntry();
        }
    };

    private void doEditEntry() {
        EnterAddressLabelUtil.enterAddressLabel(getActivity(), _mbwManager.getMetadataStorage(),
                mSelectedAddress, "", addressLabelChanged);
    }

    private void doShowQrCode() {
        if (!isAdded()) {
            return;
        }
        if (mSelectedAddress == null) {
            return;
        }
        boolean hasPrivateKey = _mbwManager.getWalletManager(false).hasPrivateKey(mSelectedAddress);
        UUID tempAccount = _mbwManager.createOnTheFlyAccount(mSelectedAddress);
        ReceiveCoinsActivity.callMe(getActivity(), _mbwManager.getWalletManager(true).getAccount(tempAccount),
                hasPrivateKey, false, true);
        finishActionMode();
    }

    final Runnable pinProtectedDeleteEntry = new Runnable() {
        @Override
        public void run() {
            doDeleteEntry();
        }
    };

    private void doDeleteEntry() {
        new AlertDialog.Builder(getActivity())
                .setMessage(R.string.delete_address_confirmation)
                .setCancelable(false)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        _mbwManager.getMetadataStorage().deleteAddressMetadata(((BtcAddress) mSelectedAddress).getAddress());
                        finishActionMode();
                        MbwManager.getEventBus().post(new AddressBookChanged());
                    }
                }).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                finishActionMode();
            }
        })
                .create()
                .show();
    }

private class AddDialog extends Dialog {
    public AddDialog(final Activity activity) {
        super(activity);
        this.setContentView(R.layout.add_to_address_book_dialog);
        this.setTitle(R.string.add_to_address_book_dialog_title);

        findViewById(R.id.btScan).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                StringHandleConfig request = HandleConfigFactory.getAddressBookScanRequest();
                ScanActivity.callMe(AddressBookFragment.this.getActivity(), SCAN_RESULT_CODE, request);
                AddDialog.this.dismiss();
            }
        });

        Optional<GenericAddress> address = Utils.addressFromString(Utils.getClipboardString(activity), _mbwManager.getNetwork());
        findViewById(R.id.btClipboard).setEnabled(address.isPresent());
        findViewById(R.id.btClipboard).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                String address = Utils.getClipboardString(activity);
                List<GenericAddress> addresses = _mbwManager.getWalletManager(false).parseAddress(address);
                if (!addresses.isEmpty()) {
                    SelectAssetDialog dialog = SelectAssetDialog.getInstance(addresses);
                    dialog.show(getFragmentManager(), "dialog");
                } else {
                    Toast.makeText(AddDialog.this.getContext(), R.string.unrecognized_format, Toast.LENGTH_SHORT).show();
                }
                AddDialog.this.dismiss();
            }
        });
    }
}

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.miAddAddress) {
            _addDialog = new AddDialog(getActivity());
            _addDialog.show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent intent) {
        if (requestCode != SCAN_RESULT_CODE) {
            super.onActivityResult(requestCode, resultCode, intent);
        }
        if (resultCode != Activity.RESULT_OK) {
            if (intent == null) {
                return; // user pressed back
            }
            String error = intent.getStringExtra(StringHandlerActivity.RESULT_ERROR);
            if (error != null) {
                Toast.makeText(getActivity(), error, Toast.LENGTH_LONG).show();
            }
            return;
        }
        ResultType type = (ResultType) intent.getSerializableExtra(StringHandlerActivity.RESULT_TYPE_KEY);
        switch (type) {
            case PRIVATE_KEY:
                Utils.showSimpleMessageDialog(getActivity(), R.string.addressbook_cannot_add_private_key);
                break;
            case ASSET_URI:
                addFromAddress(getAssetUri(intent).getAddress());
                break;
            case ADDRESS:
                getAddress(intent, _mbwManager.getWalletManager(false), getFragmentManager());
                break;
        }
    }

    private void addFromAddress(GenericAddress address) {
        EnterAddressLabelUtil.enterAddressLabel(getActivity(), _mbwManager.getMetadataStorage(), address, "", addressLabelChanged);
        _mbwManager.getMetadataStorage().storeAddressCoinType(address.toString(), address.getCoinType().getName());
    }

    private AddressLabelChangedHandler addressLabelChanged = new AddressLabelChangedHandler() {
        @Override
        public void OnAddressLabelChanged(Address address, String label) {
            finishActionMode();
            MbwManager.getEventBus().post(new AddressBookChanged());
        }
    };

    @Subscribe
    public void onAddressBookChanged(AddressBookChanged event) {
        updateUi();
    }

    @Subscribe
    public void newAddressCreating(AssetSelected event) {
        addFromAddress(event.address);
    }

private class SelectItemListener implements OnItemClickListener {
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        GenericAddress address = (GenericAddress) view.getTag();
        Intent result = new Intent();
        result.putExtra(ADDRESS_RESULT_NAME, address);

        if (parent.getItemAtPosition(position) instanceof AddressBookManager.IconEntry) {
            AddressBookManager.IconEntry item = (AddressBookManager.IconEntry) parent.getItemAtPosition(position);
            result.putExtra(ADDRESS_RESULT_ID, item.getId());
            result.putExtra(ADDRESS_RESULT_LABEL, item.getName());
        }
        getActivity().setResult(Activity.RESULT_OK, result);
        getActivity().finish();
    }
}
}
