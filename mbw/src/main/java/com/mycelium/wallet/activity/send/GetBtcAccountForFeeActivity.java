package com.mycelium.wallet.activity.send;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.google.common.base.Optional;
import com.mrd.bitlib.model.Address;
import com.mycelium.wallet.AddressBookManager;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.activity.modern.AddressBookFragment;
import com.mycelium.wallet.activity.modern.adapter.AddressBookAdapter;
import com.mycelium.wallet.activity.send.model.AccountForFee;
import com.mycelium.wallet.activity.util.ValueExtentionsKt;
import com.mycelium.wapi.wallet.WalletAccount;
import com.mycelium.wapi.wallet.btc.WalletBtcAccount;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.mycelium.wallet.activity.util.ValueExtentionsKt.isBtc;


public class GetBtcAccountForFeeActivity extends AppCompatActivity {

    private MbwManager _mbwManager;

    @BindView(R.id.lvAccounts)
    ListView lvAccounts;

    @BindView(R.id.tvNoRecords)
    TextView tvNoRecords;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_btc_account_for_fee);
        ButterKnife.bind(this);
        _mbwManager = MbwManager.getInstance(getApplication());

        List<AddressBookManager.Entry> entries = new ArrayList<>();
        WalletAccount selectedAccount = _mbwManager.getSelectedAccount();
        for (WalletAccount account : Utils.sortAccounts(_mbwManager.getWalletManager(false).getActiveAccounts(), _mbwManager.getMetadataStorage())) {
            Optional<Address> receivingAddress = ((WalletBtcAccount)(account)).getReceivingAddress();
            if (receivingAddress.isPresent() && account.canSpend()
                    && !((WalletBtcAccount)(account)).getReceivingAddress().equals(((WalletBtcAccount)(selectedAccount)).getReceivingAddress())
                    && !account.getAccountBalance().confirmed.isZero()
                    && isBtc(account.getAccountBalance().confirmed.type)) {
                String name = _mbwManager.getMetadataStorage().getLabelByAccount(account.getId());
                Drawable drawableForAccount = Utils.getDrawableForAccount(account, true, getResources());
                entries.add(new AccountForFee(receivingAddress.get(), name, drawableForAccount, account.getId(), account.getAccountBalance().confirmed));
            }
        }

        if (entries.isEmpty()) {
            tvNoRecords.setVisibility(View.VISIBLE);
            lvAccounts.setVisibility(View.GONE);
        } else {
            tvNoRecords.setVisibility(View.GONE);
            lvAccounts.setVisibility(View.VISIBLE);
            lvAccounts.setAdapter(new AddressBookAdapter(this, R.layout.btc_account_for_fee_row, entries) {
                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    View view = super.getView(position, convertView, parent);
                    AccountForFee accountForFee = (AccountForFee) getItem(position);
                    ((TextView) view.findViewById(R.id.tvBalance)).setText(ValueExtentionsKt.toStringWithUnit(accountForFee.getBalance(), _mbwManager.getBitcoinDenomination()));
                    return view;
                }
            });
            lvAccounts.setOnItemClickListener(new SelectItemListener());
        }
    }

    private class SelectItemListener implements AdapterView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Address address = (Address) view.getTag();
            Intent result = new Intent();
            result.putExtra(AddressBookFragment.ADDRESS_RESULT_NAME, address.toString());

            if (parent.getItemAtPosition(position) instanceof AddressBookManager.IconEntry) {
                AddressBookManager.IconEntry item = (AddressBookManager.IconEntry) parent.getItemAtPosition(position);
                result.putExtra(AddressBookFragment.ADDRESS_RESULT_ID, item.getId());
                result.putExtra(AddressBookFragment.ADDRESS_RESULT_LABEL, item.getName());
            }
            setResult(Activity.RESULT_OK, result);
            finish();
        }
    }
}
