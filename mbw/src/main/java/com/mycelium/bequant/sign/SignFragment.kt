package com.mycelium.bequant.sign

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayoutMediator
import com.mycelium.bequant.market.BequantMarketActivity
import com.mycelium.wallet.R
import kotlinx.android.synthetic.main.activity_bequant_market.*


class SignFragment : Fragment(R.layout.fragment_bequant_sign) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        pager.adapter = SignFragmentAdapter(this)
        TabLayoutMediator(tabs, pager) { tab, position ->
            when (position) {
                0 -> tab.text = getString(R.string.bequant_sign_up)
                1 -> tab.text = getString(R.string.bequant_sign_in)
            }
        }.attach()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_bequant_sign, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
            when (item.itemId) {
                android.R.id.home -> {
                    requireActivity().finish()
                    true
                }
                R.id.tryDemo -> {
                    requireActivity().finish()
                    startActivity(Intent(requireContext(), BequantMarketActivity::class.java))
                    true
                }
                else -> super.onOptionsItemSelected(item)
            }
}