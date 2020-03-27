package com.mycelium.bequant.sign

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.mycelium.bequant.signin.SignInFragment
import com.mycelium.bequant.signup.SignUpFragment


class SignFragmentAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment =
            when (position) {
                0 -> SignUpFragment()
                1 -> SignInFragment()
                else -> TODO("not implemented")
            }
}