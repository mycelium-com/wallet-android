package com.mycelium.bequant.sign

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.mycelium.bequant.signin.SignInFragment
import com.mycelium.bequant.signup.SignUpFragment


class SignFragmentAdapter(fragmentManager: FragmentManager, lifecycle: Lifecycle) :
        FragmentStateAdapter(fragmentManager, lifecycle) {
    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment =
            when (position) {
                0 -> SignUpFragment()
                1 -> SignInFragment()
                else -> TODO("not implemented")
            }
}