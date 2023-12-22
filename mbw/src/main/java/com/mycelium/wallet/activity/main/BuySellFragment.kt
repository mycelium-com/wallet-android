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
package com.mycelium.wallet.activity.main

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.mycelium.view.ItemCentralizer
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.main.adapter.ButtonAdapter
import com.mycelium.wallet.activity.main.adapter.ButtonClickListener
import com.mycelium.wallet.activity.main.adapter.QuadAdsAdapter
import com.mycelium.wallet.activity.main.model.ActionButton
import com.mycelium.wallet.activity.settings.SettingsPreference.fioEnabled
import com.mycelium.wallet.activity.settings.SettingsPreference.getBalanceContent
import com.mycelium.wallet.activity.settings.SettingsPreference.isContentEnabled
import com.mycelium.wallet.databinding.MainBuySellFragmentBinding
import com.mycelium.wallet.event.PageSelectedEvent
import com.mycelium.wallet.event.SelectedAccountChanged
import com.mycelium.wallet.external.Ads.openFio
import com.mycelium.wallet.external.partner.model.BuySellButton
import com.mycelium.wallet.external.partner.model.check
import com.mycelium.wallet.external.partner.startContentLink
import com.mycelium.wapi.wallet.eth.AbstractEthERC20Account
import com.squareup.otto.Subscribe

class BuySellFragment : Fragment(), ButtonClickListener {
    enum class ACTION {
        FIO, ADS
    }

    private lateinit var mbwManager: MbwManager
    private val buttonAdapter = ButtonAdapter().apply {
        clickListener = {
            onClick(it)
        }
    }
    private val quadAdapter = QuadAdsAdapter()
    private var binding: MainBuySellFragmentBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        setHasOptionsMenu(false)
        super.onCreate(savedInstanceState)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mbwManager = MbwManager.getInstance(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = MainBuySellFragmentBinding.inflate(inflater).apply {
        binding = this
    }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.buttonList?.adapter = buttonAdapter
        binding?.buttonList?.addOnScrollListener(ItemCentralizer())
        binding?.quadList?.adapter = quadAdapter
        binding?.quadList?.addOnScrollListener(ItemCentralizer())
        recreateActions()
        quadAdapter.submitList(getBalanceContent()
            ?.quads
            ?.filter {
                it.isActive() && isContentEnabled(it.parentId ?: "")
                        && it.filter.check(mbwManager.selectedAccount)
            }?.sortedBy { it.index })
        quadAdapter.clickListener = { startContentLink(it.link) }
    }

    private fun recreateActions() {
        buttonAdapter.submitList(mutableListOf<ActionButton>().apply {
            addAdsContent(this)
            if (mbwManager.selectedAccount !is AbstractEthERC20Account) {
                addFio(this)
            }
        })
        binding?.buttonList?.postDelayed(ScrollToRunner(1), 500)
    }

    private fun addAdsContent(actions: MutableList<ActionButton>) {
        val balanceContent = getBalanceContent() ?: return
        actions.addAll(balanceContent.buttons
            .filter {
                it.isActive() && isContentEnabled(it.parentId ?: "")
                        && it.filter.check(mbwManager.selectedAccount)
            }
            .sortedBy { it.index }
            .map {
                ActionButton(ACTION.ADS, handleName(it.name) ?: "", 0, it.iconUrl,
                    Bundle().apply {
                        putSerializable("data", it)
                        putInt("index", it.index ?: Int.MAX_VALUE)
                    })
            })
    }

    private fun handleName(name: String?) =
        name?.replace("%%%account.type.name%%%", mbwManager.selectedAccount.coinType.name)
            ?.replace("%%%account.type.symbol%%%", mbwManager.selectedAccount.coinType.symbol)

    private fun addFio(actions: MutableList<ActionButton>) {
        if (fioEnabled) {
            actions.add(ActionButton(ACTION.FIO, getString(R.string.partner_fiopresale), R.drawable.ic_fiopresale_icon_small))
        }
    }

    override fun onClick(actionButton: ActionButton) {
        when (actionButton.id) {
            ACTION.FIO -> openFio(requireContext())
            ACTION.ADS -> if (actionButton.args.containsKey("data")) {
                (actionButton.args.getSerializable("data") as BuySellButton?)?.let { data ->
                    startContentLink(data.link, Bundle().apply {
                        putSerializable("currency", mbwManager.selectedAccount.coinType)
                    })
                }
            }
        }
    }

    internal inner class ScrollToRunner(var scrollTo: Int) : Runnable {
        override fun run() {
            binding?.buttonList?.smoothScrollToPosition(scrollTo)
        }
    }


    override fun onStart() {
        MbwManager.getEventBus().register(this)
        recreateActions()
        super.onStart()
    }

    override fun onStop() {
        MbwManager.getEventBus().unregister(this)
        super.onStop()
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    @Subscribe
    fun selectedAccountChanged(event: SelectedAccountChanged?) {
        recreateActions()
    }

    @Subscribe
    fun pageSelectedEvent(event: PageSelectedEvent) {
        if (event.position == 1) {
            recreateActions()
        }
    }
}