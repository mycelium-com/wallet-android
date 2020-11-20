package com.mycelium.bequant.intro

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter


class IntroPagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
    val items = listOf(IntroPage1Fragment(), IntroPage2Fragment(), IntroPage3Fragment(), IntroPage4Fragment())

    override fun getItemCount(): Int = items.size

    override fun createFragment(position: Int): Fragment = items[position]
}