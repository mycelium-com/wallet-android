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

package com.mycelium.wallet.activity.modern.adapter;

import java.util.ArrayList;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;

import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.event.PageSelectedEvent;

public class TabsAdapter extends FragmentPagerAdapter implements ActionBar.TabListener, ViewPager.OnPageChangeListener {
   private final Activity mContext;
   private final ActionBar mActionBar;
   private final ViewPager mViewPager;
   private final ArrayList<TabInfo> mTabs = new ArrayList<TabInfo>();
   private final MbwManager _mbwManager;

   private static final class TabInfo {
      private final Class<?> clss;
      private final Bundle args;

      TabInfo(Class<?> _class, Bundle _args) {
         clss = _class;
         args = _args;
      }
   }

   public TabsAdapter(AppCompatActivity activity, ViewPager pager, MbwManager mbwManager) {
      super(activity.getSupportFragmentManager());
      mContext = activity;
      _mbwManager = mbwManager;
      mActionBar = activity.getSupportActionBar();
      mViewPager = pager;
      mViewPager.setAdapter(this);
      mViewPager.setOnPageChangeListener(this);
   }

   public void addTab(ActionBar.Tab tab, Class<?> clss, Bundle args) {
      TabInfo info = new TabInfo(clss, args);
      tab.setTag(info);
      tab.setTabListener(this);
      mTabs.add(info);
      mActionBar.addTab(tab);
      notifyDataSetChanged();
   }

   @Override
   public int getCount() {
      return mTabs.size();
   }

   @Override
   public Fragment getItem(int position) {
      TabInfo info = mTabs.get(position);
      return Fragment.instantiate(mContext, info.clss.getName(), info.args);
   }

   @Override
   public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
   }

   @Override
   public void onPageSelected(int position) {
      mActionBar.setSelectedNavigationItem(position);
      // This ensures that any cached encryption key is flushed when we swipe to
      // another tab
      _mbwManager.clearCachedEncryptionParameters();
      // redraw menu - not working yet
      ActivityCompat.invalidateOptionsMenu(mContext);
      _mbwManager.getEventBus().post(new PageSelectedEvent(position));
   }

   @Override
   public void onPageScrollStateChanged(int state) {
   }

   @Override
   public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
      Object tag = tab.getTag();
      for (int i = 0; i < mTabs.size(); i++) {
         if (mTabs.get(i) == tag) {
            mViewPager.setCurrentItem(i);
         }
      }
   }

   @Override
   public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
   }

   @Override
   public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {
   }
}
