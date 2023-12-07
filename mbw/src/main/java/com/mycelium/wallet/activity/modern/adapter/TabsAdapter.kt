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
package com.mycelium.wallet.activity.modern.adapter

import android.app.Activity
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.event.PageSelectedEvent

class TabsAdapter(val activity: AppCompatActivity,val pager: ViewPager, val mbwManager: MbwManager) :
    FragmentPagerAdapter(activity.supportFragmentManager), ViewPager.OnPageChangeListener {
    private val mTabs = mutableListOf<TabInfo>()

    private data class TabInfo(
        val clss: Class<*>,
        val args: Bundle?,
        val title: String,
        val tag: String
    )

    init {
        pager.adapter = this
        pager.addOnPageChangeListener(this)
    }

    fun addTab(tab: TabLayout.Tab, clss: Class<*>, args: Bundle?, tabTag: String) {
        val info = TabInfo(clss, args, tab.text.toString(), tabTag)
        tab.tag = info
        mTabs.add(info)
        notifyDataSetChanged()
    }

    fun addTab(i: Int, tab: TabLayout.Tab, clss: Class<*>, args: Bundle, tabTag: String) {
        val info = TabInfo(clss, args, tab.text.toString(), tabTag)
        tab.tag = info
        mTabs.add(i, info)
        notifyDataSetChanged()
    }

    fun removeTab(tabTag: String) {
        mTabs.removeAll { it.tag == tabTag }
        notifyDataSetChanged()
    }

    override fun getCount(): Int = mTabs.size

    override fun getItemId(position: Int): Long =
        mTabs[position].tag.hashCode().toLong()

    override fun getItem(position: Int): Fragment =
        mTabs[position].let { Fragment.instantiate(activity, it.clss.name, it.args) }

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}
    override fun onPageSelected(position: Int) {
        // This ensures that any cached encryption key is flushed when we swipe to
        // another tab
        mbwManager.clearCachedEncryptionParameters()
        // redraw menu - not working yet
        ActivityCompat.invalidateOptionsMenu(activity)
        MbwManager.getEventBus().post(PageSelectedEvent(position, mTabs[position].tag))
    }

    override fun onPageScrollStateChanged(state: Int) {}
    override fun getPageTitle(position: Int): CharSequence? =
        mTabs[position].title

    fun getPageTag(position: Int): String =
        mTabs[position].tag

    fun indexOf(tabTag: String): Int =
        mTabs.indexOfFirst { it.tag == tabTag }

    override fun getItemPosition(`object`: Any): Int =
        POSITION_NONE
}