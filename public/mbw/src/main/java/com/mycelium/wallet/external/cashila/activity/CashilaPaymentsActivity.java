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

package com.mycelium.wallet.external.cashila.activity;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import butterknife.ButterKnife;
import com.google.common.base.Strings;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.activity.send.SendMainActivity;
import com.mycelium.wallet.bitid.ExternalService;
import com.mycelium.wallet.external.cashila.ApiException;
import com.mycelium.wallet.external.cashila.ApiExceptionAuth;
import com.mycelium.wallet.external.cashila.api.CashilaService;
import com.mycelium.wallet.external.cashila.api.response.BillPay;
import com.mycelium.wallet.external.cashila.api.response.DeepLink;
import com.mycelium.wapi.api.response.Feature;
import com.squareup.otto.Subscribe;
import rx.Observer;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;


public class CashilaPaymentsActivity extends ActionBarActivity implements ActionBar.TabListener {
   private static final String CASHILA_SERVICE = "cashilaService";
   private static final int REQUEST_SEND_AMOUNT = 1;
   private static final int REQUEST_WEBSITE = 2;
   public static final String WARNINGS_SHOWN = "warningsShown";

   private ViewPager viewPager;
   private CashilaService cs;
   private MbwManager mbw;
   private boolean warningsShown;


   public static Intent getIntent(Context context, BcdCodedSepaData bcdQrCode) {
      Intent intent = new Intent(context, CashilaPaymentsActivity.class);
      intent.putExtra("bcd", bcdQrCode);
      return intent;
   }

   public static Intent getIntent(Context context) {
      return new Intent(context, CashilaPaymentsActivity.class);
   }

   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.ext_cashila_payments);
      ButterKnife.inject(this);

      mbw = MbwManager.getInstance(this);
      mbw.getEventBus().register(this);

      // Set up the action bar.
      final ActionBar actionBar = getSupportActionBar();
      actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
      actionBar.setHomeButtonEnabled(true);

      // Create the adapter that will return a fragment for each of the three
      // primary sections of the activity.
      SectionsPagerAdapter sectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

      // Set up the ViewPager with the sections adapter.
      viewPager = (ViewPager) findViewById(R.id.pager);
      viewPager.setAdapter(sectionsPagerAdapter);

      // When swiping between different sections, select the corresponding
      // tab. We can also use ActionBar.Tab#select() to do this if we have
      // a reference to the Tab.
      viewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
         @Override
         public void onPageSelected(int position) {
            actionBar.setSelectedNavigationItem(position);
            // Hide the keyboard.
            ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(viewPager.getWindowToken(), 0);
         }
      });


      try {
         cs = (CashilaService) mbw.getBackgroundObjectsCache().get(CASHILA_SERVICE, new Callable<Object>() {
            @Override
            public Object call() throws Exception {
               String api = ExternalService.CASHILA.getApi(mbw.getNetwork());
               return new CashilaService(api, "v1", mbw.getEventBus());
            }
         });
      } catch (ExecutionException e) {
         throw new RuntimeException(e);
      }

      actionBar.addTab(
            actionBar.newTab()
                  .setText(getString(R.string.cashila_tab_new).toUpperCase())
                  .setTabListener(this));

      actionBar.addTab(
            actionBar.newTab()
                  .setText(getString(R.string.cashila_tab_pending).toUpperCase())
                  .setTabListener(this));

      this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
   }


   @Override
   protected void onDestroy() {
      super.onDestroy();
      mbw.getEventBus().unregister(this);
   }

   @Override
   protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
      super.onRestoreInstanceState(savedInstanceState);
      warningsShown = savedInstanceState.getBoolean(WARNINGS_SHOWN);
   }

   @Override
   protected void onSaveInstanceState(Bundle outState) {
      outState.putBoolean(WARNINGS_SHOWN, warningsShown);
      super.onSaveInstanceState(outState);
   }

   public void setCurrentPage(int page) {
      viewPager.setCurrentItem(page);
   }

   @Override
   public boolean onCreateOptionsMenu(Menu menu) {
      // Inflate the menu; this adds items to the action bar if it is present.
      getMenuInflater().inflate(R.menu.ext_cashila_menu, menu);
      return true;
   }

   @Override
   public boolean onOptionsItemSelected(MenuItem item) {
      // Handle action bar item clicks here. The action bar will
      // automatically handle clicks on the Home/Up button, so long
      // as you specify a parent activity in AndroidManifest.xml.
      int id = item.getItemId();

      if (id == R.id.miRefresh) {
         updatePayments();
         return true;
      }

      if (id == R.id.miOpenWebsite) {
         openDashBoard();
         return true;
      }

      return super.onOptionsItemSelected(item);
   }

   public void openDashBoard() {
      openDeepLink(CashilaService.DEEP_LINK_DASHBOARD);
   }

   public void openAddRecipient() {
      openDeepLink(CashilaService.DEEP_LINK_ADD_RECIPIENT);
   }

   private void openDeepLink(String resource) {
      getCashilaService().getDeepLink(resource)
            .subscribe(new Observer<DeepLink>() {
               @Override
               public void onCompleted() {
               }

               @Override
               public void onError(Throwable e) {
               }

               @Override
               public void onNext(DeepLink deepLink) {
                  Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(deepLink.url));
                  browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                  CashilaPaymentsActivity.this.startActivity(browserIntent);
               }
            });
   }


   private String makeFragmentName(int viewPagerId, int index) {
      return "android:switcher:" + viewPagerId + ":" + index;
   }

   private CashilaNewFragment getNewFragment() {
      return (CashilaNewFragment) getSupportFragmentManager().findFragmentByTag(makeFragmentName(R.id.pager, 0));
   }

   private CashilaPendingFragment getPendingFragment() {
      return (CashilaPendingFragment) getSupportFragmentManager().findFragmentByTag(makeFragmentName(R.id.pager, 1));
   }

   public void updatePayments() {
      getNewFragment().refresh();
      getPendingFragment().refresh(true);
   }

   @Override
   public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
      // When the given tab is selected, switch to the corresponding page in
      // the ViewPager.
      viewPager.setCurrentItem(tab.getPosition());
   }

   @Override
   public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {

   }

   @Override
   public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {

   }

   @Override
   protected void onActivityResult(int requestCode, int resultCode, Intent data) {
      if (requestCode == REQUEST_SEND_AMOUNT) {
         // switch to the list tab
         setCurrentPage(1);

         // reload list immediately ...
         updatePayments();

         if (resultCode == RESULT_OK) {
            // ... and also schedule a reload of the pending list in some seconds
            // because the cashila backend takes some time to register the payment
            new Handler().postDelayed(new Runnable() {
               @Override
               public void run() {
                  CashilaPendingFragment pendingFragment = getPendingFragment();
                  // ensure that the fragment is still alive
                  if (pendingFragment != null && pendingFragment.isAdded()) {
                     pendingFragment.refresh(false);
                  }
               }
            }, 5000);
         }
      } else {
         super.onActivityResult(requestCode, resultCode, data);
      }
   }

   public boolean ignoreWarnings;

   @Subscribe()
   public synchronized void onCashilaApiException(final ApiException exception) {
      if (!ignoreWarnings) {
         // prevent that two message windows pop up
         ignoreWarnings = true;
         String message = exception.getMessage();

         if (exception instanceof ApiExceptionAuth) {
            message = getString(R.string.cashila_account_needs_pairing) + "\n\n" + exception.getMessage();
         }

         Utils.showSimpleMessageDialog(this, message, null, new Runnable() {
            @Override
            public void run() {
               if (exception instanceof ApiExceptionAuth) {
                  CashilaPaymentsActivity.this.finish();
               }
               ignoreWarnings = false;
            }
         });
      }
   }

   @Subscribe
   public void onRequestToPay(final BillPay billPay) {
      mbw.getVersionManager().showFeatureWarningIfNeeded(this, Feature.CASHILA_PAY, true, new Runnable() {

         @Override
         public void run() {
            String txLabel = "Cashila";
            if (!Strings.isNullOrEmpty(billPay.recipient.name)) {
               txLabel += ": " + billPay.recipient.name;
            }
            if (!Strings.isNullOrEmpty(billPay.payment.reference)) {
               txLabel += ", " + billPay.payment.reference;
            }
            Intent intent = SendMainActivity.getSepaIntent(CashilaPaymentsActivity.this,
                  mbw.getSelectedAccount().getId(),
                  billPay,
                  txLabel,
                  false);

            CashilaPaymentsActivity.this.startActivityForResult(intent, REQUEST_SEND_AMOUNT);
         }
      });

   }

   public CashilaService getCashilaService() {
      return cs;
   }


   /**
    * A {@link android.support.v4.app.FragmentPagerAdapter} that returns a fragment corresponding to
    * one of the sections/tabs/pages.
    */
   public class SectionsPagerAdapter extends FragmentPagerAdapter {
      public SectionsPagerAdapter(FragmentManager fm) {
         super(fm);
      }

      @Override
      public Fragment getItem(int position) {
         if (position == 0) {
            return CashilaNewFragment.newInstance();
         } else {
            return CashilaPendingFragment.newInstance();
         }
      }

      @Override
      public int getCount() {
         return 2;
      }

   }
}

