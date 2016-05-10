package com.mycelium.wallet.external.glidera.activities;


import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBar.Tab;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;

import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.activity.modern.TabsAdapter;
import com.mycelium.wallet.external.glidera.fragments.GlideraBuyFragment;
import com.mycelium.wallet.external.glidera.fragments.GlideraSellFragment;
import com.mycelium.wallet.external.glidera.fragments.GlideraTransactionHistoryFragment;

public class GlideraMainActivity extends ActionBarActivity {
    private Tab buyBitcoinTab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ViewPager viewPager = new ViewPager(this);
        viewPager.setId(R.id.pager);
        setContentView(viewPager);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        actionBar.setDisplayOptions(1, ActionBar.DISPLAY_SHOW_TITLE);
        actionBar.setDisplayHomeAsUpEnabled(true);

        TabsAdapter tabsAdapter = new TabsAdapter(this, viewPager, MbwManager.getInstance(this));

        buyBitcoinTab = actionBar.newTab();
        tabsAdapter.addTab(buyBitcoinTab.setText(getString(R.string.gd_buy_tab)), GlideraBuyFragment.class, null);

        Tab sellBitcoinTab = actionBar.newTab();
        tabsAdapter.addTab(sellBitcoinTab.setText(getString(R.string.gd_sell_tab)), GlideraSellFragment.class, null);

        Tab transactionHistoryTab = actionBar.newTab();
        tabsAdapter.addTab(transactionHistoryTab.setText(getString(R.string.gd_transaction_history_tab)),
                GlideraTransactionHistoryFragment.class, null);

        Bundle bundle = getIntent().getExtras();

        if( bundle != null ) {
            String tab = getIntent().getExtras().getString("tab");

            if (tab.equals("buy")) {
                actionBar.selectTab(buyBitcoinTab);
            } else if (tab.equals("sell")) {
                actionBar.selectTab(sellBitcoinTab);
            } else if (tab.equals("history")) {
                actionBar.selectTab(transactionHistoryTab);
            } else {
                actionBar.selectTab(buyBitcoinTab);
            }
        }
        else {
            actionBar.selectTab(buyBitcoinTab);
        }

    }

    @Override
    public void onBackPressed() {
        ActionBar bar = getSupportActionBar();
        if (bar.getSelectedTab() == buyBitcoinTab) {
            super.onBackPressed();
        } else {
            bar.selectTab(buyBitcoinTab);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
