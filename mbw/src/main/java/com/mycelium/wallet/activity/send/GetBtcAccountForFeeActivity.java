package com.mycelium.wallet.activity.send;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

import com.mycelium.wallet.R;
import com.mycelium.wallet.activity.modern.AddressBookFragment;


public class GetBtcAccountForFeeActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_btc_account_for_fee);
        AddressBookFragment fragment = new AddressBookFragment();
        Bundle ownBundle = new Bundle();
        ownBundle.putBoolean(AddressBookFragment.SPENDABLE_ONLY, true);
        ownBundle.putBoolean(AddressBookFragment.OWN, true);
        ownBundle.putBoolean(AddressBookFragment.EXCLUDE_SELECTED, true);
        ownBundle.putBoolean(AddressBookFragment.SELECT_ONLY, true);
        ownBundle.putBoolean(AddressBookFragment.FOR_FEE, true);
        fragment.setArguments(ownBundle);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commitAllowingStateLoss();
    }
}
