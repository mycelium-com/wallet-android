package com.mycelium.bequant.sign

import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.mycelium.bequant.signin.SignInFragment
import com.mycelium.bequant.signup.SignUpFragment
import com.mycelium.wallet.R


class SignFragmentAdapter(val fragment: Fragment) : FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment =
            when (position) {
                0 -> SignUpFragment().apply {
                    registerListener = { register ->
                        val direction = SignFragmentDirections.actionRegister(register)
                        fragment.findNavController().navigate(direction)
                    }
                }
                1 -> SignInFragment().apply {
                    resetPasswordListener = {
                        fragment.findNavController().navigate(R.id.actionResetPassword)
                    }
                    signListener = {
                        fragment.findNavController().navigate(R.id.actionSignIn)
                    }
                }
                else -> TODO("not implemented")
            }
}