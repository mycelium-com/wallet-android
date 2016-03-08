package com.mycelium.wallet.glidera.activities;


import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBar.Tab;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;

import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.activity.modern.ModernMain;
import com.mycelium.wallet.activity.modern.TabsAdapter;
import com.mycelium.wallet.activity.modern.Toaster;
import com.mycelium.wallet.glidera.api.GlideraService;
import com.mycelium.wallet.glidera.fragments.GlideraBuyFragment;
import com.mycelium.wallet.glidera.fragments.GlideraSellFragment;
import com.mycelium.wallet.glidera.fragments.GlideraTransactionHistoryFragment;

public class GlideraMainActivity extends ActionBarActivity {
    private MbwManager mbwManager;
    private GlideraService glideraService;

    private ViewPager viewPager;

    private TabsAdapter tabsAdapter;
    private Tab buyBitcoinTab;
    private Tab sellBitcoinTab;
    private Tab transactionHistoryTab;

    private Toaster toaster;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mbwManager = MbwManager.getInstance(this);
        glideraService = GlideraService.getInstance();

        viewPager = new ViewPager(this);
        viewPager.setId(R.id.pager);
        setContentView(viewPager);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        actionBar.setDisplayOptions(1, ActionBar.DISPLAY_SHOW_TITLE);

        tabsAdapter = new TabsAdapter(this, viewPager, mbwManager);

        buyBitcoinTab = actionBar.newTab();
        tabsAdapter.addTab(buyBitcoinTab.setText(getString(R.string.gd_buy_tab)), GlideraBuyFragment.class, null);

        sellBitcoinTab = actionBar.newTab();
        tabsAdapter.addTab(sellBitcoinTab.setText(getString(R.string.gd_sell_tab)), GlideraSellFragment.class, null);

        transactionHistoryTab = actionBar.newTab();
        tabsAdapter.addTab(transactionHistoryTab.setText(getString(R.string.gd_transaction_history_tab)), GlideraTransactionHistoryFragment.class, null);

        actionBar.selectTab(buyBitcoinTab);

        getActionBar().setDisplayHomeAsUpEnabled(true);
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
