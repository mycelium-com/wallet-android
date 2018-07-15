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

package com.mycelium.wallet.activity.settings;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.activity.settings.adapter.LocalCurrencyAdapter;
import com.mycelium.wapi.api.lib.CurrencyCode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SetLocalCurrencyActivity extends AppCompatActivity {
    public static void callMe(Activity currentActivity) {
        Intent intent = new Intent(currentActivity, SetLocalCurrencyActivity.class);
        ActivityOptions options = ActivityOptions.makeCustomAnimation(currentActivity, R.anim.slide_right_in, R.anim.slide_left_out);
        currentActivity.startActivity(intent, options.toBundle());
    }

    private LocalCurrencyAdapter _adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_local_currency_activity);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setTitle(R.string.fiat_currency);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_back_arrow);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        final List<CurrencyCode> selected = new ArrayList<>();
        for (String currency : MbwManager.getInstance(this).getCurrencyList()) {
            selected.add(CurrencyCode.valueOf(currency));
        }

        List<CurrencyCode> codes = new ArrayList<>(Arrays.asList(CurrencyCode.values()));
        codes.remove(CurrencyCode.UNKNOWN); // don't know what is UNKNOWN, so hide this for user

        Collections.sort(codes, new Comparator<CurrencyCode>() {
            @Override
            public int compare(CurrencyCode currency1, CurrencyCode currency2) {
                if (selected.contains(currency1)) {
                    return -1;
                } else if (selected.contains(currency2)) {
                    return 1;
                } else {
                    return currency1.compareTo(currency2);
                }
            }
        });
        // Populate adapter - and overwrite getView to correctly set checkbox status
        _adapter = new LocalCurrencyAdapter(this, codes);
        _adapter.setSelected(selected);

        // Configure list view
        ListView listview = findViewById(R.id.lvCurrencies);
        listview.setAdapter(_adapter);
        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                CurrencyCode currencyCode = _adapter.getItem(i);
                _adapter.toggleChecked(currencyCode);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_local_currency, menu);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                _adapter.getFilter().filter("");
                return false;
            }
        });
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                findSearchResult(s);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                findSearchResult(s);

                return true;
            }

            private void findSearchResult(String s) {
                _adapter.getFilter().filter(s);
            }
        });

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Set<String> currencyList = new HashSet<>();
        for (CurrencyCode currencyCode : _adapter.getSelected()) {
            currencyList.add(currencyCode.getShortString());
        }
        if (currencyList.isEmpty()) {
            currencyList.add(CurrencyCode.USD.getShortString());
        }
        MbwManager.getInstance(this).setCurrencyList(currencyList);
    }
}
