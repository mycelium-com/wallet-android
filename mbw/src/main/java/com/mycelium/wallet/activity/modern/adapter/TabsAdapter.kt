package com.mycelium.wallet.activity.modern.adapter

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayout
import com.mycelium.wallet.MbwManager

class TabsAdapter(val activity: AppCompatActivity, val mbwManager: MbwManager) :
    FragmentStateAdapter(activity) {
    private val mTabs = mutableListOf<TabInfo>()
    private val fragmentFactory = FragmentFactory()

    private data class TabInfo(
        val clss: Class<*>,
        val args: Bundle?,
        val title: String,
        val tag: String
    )


    fun addTab(tab: TabLayout.Tab, clss: Class<*>, args: Bundle?, tabTag: String) {
        val info = TabInfo(clss, args, tab.text.toString(), tabTag)
        tab.tag = info
        mTabs.add(info)
        notifyItemInserted(mTabs.size - 1)
    }

    fun addTab(i: Int, tab: TabLayout.Tab, clss: Class<*>, args: Bundle, tabTag: String) {
        val info = TabInfo(clss, args, tab.text.toString(), tabTag)
        tab.tag = info
        mTabs.add(i, info)
        notifyItemInserted(i)
    }

    fun removeTab(tabTag: String) {
        val index = mTabs.indexOfFirst { it.tag == tabTag }
        if (index != -1) {
            mTabs.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    override fun getItemCount(): Int = mTabs.size

    override fun createFragment(position: Int): Fragment =
        mTabs[position].let {
            fragmentFactory.instantiate(it.clss.classLoader!!, it.clss.name).apply {
                arguments = it.args
            }
        }

    override fun getItemId(position: Int): Long =
        generateStableId(mTabs[position].tag)

    override fun containsItem(itemId: Long): Boolean =
        mTabs.any { generateStableId(it.tag) == itemId }

    fun getPageTitle(position: Int): CharSequence? =
        mTabs[position].title

    fun getPageTag(position: Int): String =
        mTabs[position].tag

    fun getPageTagOrNull(position: Int): String? =
        mTabs.getOrNull(position)?.tag

    fun indexOf(tabTag: String): Int =
        mTabs.indexOfFirst { it.tag == tabTag }

//    override fun getItemPosition(`object`: Any): Int =
//        POSITION_NONE

    private fun generateStableId(tag: String): Long = tag.fold(0L) { acc, char -> acc * 31 + char.code }
}