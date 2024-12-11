package com.mycelium.bequant.sign

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.fragment.navArgs
import com.google.android.material.tabs.TabLayoutMediator
import com.mycelium.bequant.BequantConstants.ACTION_BEQUANT_SHOW_REGISTER
import com.mycelium.bequant.market.BequantMarketActivity
import com.mycelium.wallet.R
import com.mycelium.wallet.databinding.FragmentBequantSignBinding


class SignFragment : Fragment(R.layout.fragment_bequant_sign) {

    private var tabMediator: TabLayoutMediator? = null

    val args by navArgs<SignFragmentArgs>()
    var binding: FragmentBequantSignBinding? = null

    private val showRegisterReceiver = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {
            binding?.pager?.setCurrentItem(0, true)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentBequantSignBinding.inflate(inflater, container, false)
        .apply {
            binding = this
        }
        .root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as AppCompatActivity?)?.supportActionBar?.run {
            title = null
            setHomeAsUpIndicator(resources.getDrawable(R.drawable.ic_bequant_arrow_back))
        }
        binding?.pager?.adapter = SignFragmentAdapter(childFragmentManager, viewLifecycleOwner.lifecycle)
        binding?.pager?.offscreenPageLimit = 2
        tabMediator = TabLayoutMediator(binding?.tabs!!, binding?.pager!!) { tab, position ->
            when (position) {
                0 -> tab.text = getString(R.string.bequant_sign_up)
                1 -> tab.text = getString(R.string.bequant_sign_in)
            }
        }
        tabMediator?.attach()
        binding?.pager?.postDelayed({
            when (args.tab) {
                "signUp" -> binding?.pager?.setCurrentItem(0, true)
                "signIn" -> binding?.pager?.setCurrentItem(1, true)
            }
            arguments?.remove("tab")
        }, 300)
    }

    override fun onResume() {
        super.onResume()
        LocalBroadcastManager
                .getInstance(requireContext())
                .registerReceiver(showRegisterReceiver, IntentFilter(ACTION_BEQUANT_SHOW_REGISTER))
    }

    override fun onPause() {
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(showRegisterReceiver)
        super.onPause()
    }

    override fun onDestroyView() {
        tabMediator?.detach()
        binding?.pager?.adapter = null
        binding = null
        super.onDestroyView()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_bequant_sign, menu)
        menu.findItem(R.id.tryDemo).let { item ->
            item.actionView?.findViewById<View>(R.id.tryDemoButton)?.setOnClickListener {
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
                    BequantMarketActivity.start(requireContext(),true)
                    true
                }
                else -> super.onOptionsItemSelected(item)
            }
}