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
import java.util.List;

import android.app.Activity;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.tabs.TabLayout;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.event.PageSelectedEvent;

public class TabsAdapter extends FragmentPagerAdapter implements ViewPager.OnPageChangeListener {
   private final Activity mContext;
   private final List<TabInfo> mTabs = new ArrayList<>();
   private final MbwManager _mbwManager;

   private static final class TabInfo {
      private final Class<?> clss;
      private final Bundle args;
      private final CharSequence title;
      private final String tag;

      TabInfo(Class<?> _class, Bundle _args, CharSequence title, String tag) {
         clss = _class;
         args = _args;
         this.title = title;
         this.tag = tag;
      }
   }

   public TabsAdapter(AppCompatActivity activity, ViewPager pager, MbwManager mbwManager) {
      super(activity.getSupportFragmentManager());
      mContext = activity;
      _mbwManager = mbwManager;
      pager.setAdapter(this);
      pager.addOnPageChangeListener(this);
   }

   public void addTab(TabLayout.Tab tab, Class<?> clss, Bundle args, String tabTag) {
      TabInfo info = new TabInfo(clss, args, tab.getText(), tabTag);
      tab.setTag(info);
      mTabs.add(info);
      notifyDataSetChanged();
   }

   public void addTab(int i, TabLayout.Tab tab, Class<?> clss, Bundle args, String tabTag) {
      TabInfo info = new TabInfo(clss, args, tab.getText(), tabTag);
      tab.setTag(info);
      mTabs.add(i, info);
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
      // This ensures that any cached encryption key is flushed when we swipe to
      // another tab
      _mbwManager.clearCachedEncryptionParameters();
      // redraw menu - not working yet
      ActivityCompat.invalidateOptionsMenu(mContext);
      MbwManager.getEventBus().post(new PageSelectedEvent(position, mTabs.get(position).tag));
   }

   @Override
   public void onPageScrollStateChanged(int state) {
   }

   @Nullable
   @Override
   public CharSequence getPageTitle(int position) {
      return mTabs.get(position).title;
   }

   public String getPageTag(int position) {
      return mTabs.get(position).tag;
   }

   public int indexOf(String tabTag) {
      for (int i = 0; i < mTabs.size(); i++) {
         TabInfo mTab = mTabs.get(i);
         if (tabTag.equals(mTab.tag)) {
            return i;
         }
      }
      return -1;
   }
}
