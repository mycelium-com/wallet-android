package com.mycelium.bequant.sign

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.material.tabs.TabLayoutMediator
import com.mycelium.bequant.Constants.ACTION_BEQUANT_SHOW_REGISTER
import com.mycelium.bequant.market.BequantMarketActivity
import com.mycelium.wallet.R
import kotlinx.android.synthetic.main.fragment_bequant_sign.*
import kotlinx.android.synthetic.main.menu_bequant_try_demo.view.*


class SignFragment : Fragment(R.layout.fragment_bequant_sign) {

    var tabMediator: TabLayoutMediator? = null

    val register = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {
            pager.setCurrentItem(0, true)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as AppCompatActivity?)?.supportActionBar?.title = null
        (activity as AppCompatActivity?)?.supportActionBar?.setHomeAsUpIndicator(resources.getDrawable(R.drawable.ic_bequant_arrow_back))
        pager.adapter = SignFragmentAdapter(this)
        pager.offscreenPageLimit = 2
        tabMediator = TabLayoutMediator(tabs, pager) { tab, position ->
            when (position) {
                0 -> tab.text = getString(R.string.bequant_sign_up)
                1 -> tab.text = getString(R.string.bequant_sign_in)
            }
        }
        tabMediator?.attach()
        when (arguments?.getString("tab")) {
            "signUp" -> pager.currentItem = 0
            "signIn" -> pager.currentItem = 1
        }
    }

    override fun onResume() {
        super.onResume()
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(register, IntentFilter(ACTION_BEQUANT_SHOW_REGISTER))
    }

    override fun onPause() {
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(register)
        super.onPause()
    }

    override fun onDestroyView() {
        tabMediator?.detach()
        super.onDestroyView()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_bequant_sign, menu)
        menu.findItem(R.id.tryDemo).let { item ->
            item.actionView.tryDemoButton.setOnClickListener {
                onOptionsItemSelected(item)
            }
        }
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