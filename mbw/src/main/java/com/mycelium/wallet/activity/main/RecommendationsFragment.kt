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

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.mycelium.wallet.R
import com.mycelium.wallet.R.string.*
import com.mycelium.wallet.activity.main.adapter.RecommendationAdapter
import com.mycelium.wallet.activity.main.model.*
import com.mycelium.wallet.activity.settings.SettingsPreference
import com.mycelium.wallet.activity.view.DividerItemDecoration
import com.mycelium.wallet.external.Ads
import com.mycelium.wallet.external.partner.model.Partner
import kotlinx.android.synthetic.main.main_recommendations_view.*

class RecommendationsFragment : Fragment() {
    private var alertDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        setHasOptionsMenu(true)
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.main_recommendations_view, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        list.addItemDecoration(DividerItemDecoration(resources.getDrawable(R.drawable.divider_account_list), LinearLayoutManager.VERTICAL)
                .apply { setFromItem(1) })

        val dataList = mutableListOf<RecommendationInfo>()
        dataList.add(RecommendationHeader(SettingsPreference.getPartnersHeaderTitle(),
                SettingsPreference.getPartnersHeaderText()))
        SettingsPreference.getPartners()?.forEach {
            dataList.add(getPartnerInfo(it))
        }
        dataList.add(RecommendationFooter())
        val adapter = RecommendationAdapter(dataList)
        adapter.setClickListener(object : RecommendationAdapter.ClickListener {
            override fun onClick(bean: PartnerInfo) {
                if (bean.action != null) {
                    bean.action?.run()
                } else if (bean.info != null && bean.info.isNotEmpty()) {
                    alertDialog = AlertDialog.Builder(activity)
                            .setMessage(bean.info)
                            .setTitle(warning_partner)
                            .setIcon(bean.smallIcon)
                            .setPositiveButton(ok) { dialog, id ->
                                if (bean.uri != null) {
                                    val intent = Intent(Intent.ACTION_VIEW)
                                            .setData(Uri.parse(bean.uri))
                                    startActivity(intent)
                                }
                            }
                            .setNegativeButton(cancel, null)
                            .create()
                    alertDialog?.show()
                } else {
                    if (bean.uri != null) {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(bean.uri)))
                    }
                }
            }

            override fun onClick(recommendationFooter: RecommendationFooter) {
                alertDialog = AlertDialog.Builder(activity)
                        .setTitle(your_privacy_out_priority)
                        .setMessage(partner_more_info_text)
                        .setPositiveButton(ok, null)
                        .setIcon(R.drawable.mycelium_logo_transp_small)
                        .create()
                alertDialog?.show()
            }

            override fun onClick(recommendationBanner: RecommendationBanner) {
                // implement when using big Banner Ad
            }
        })
        list.adapter = adapter
    }

    override fun onDestroy() {
        try {
            if (alertDialog?.isShowing == true) {
                alertDialog?.dismiss()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        super.onDestroy()
    }

    private fun getPartnerInfo(partner: Partner): PartnerInfo =
            PartnerInfo(partner.title, partner.description, partner.info, partner.link,
                    partner.imageUrl, if (partner.action?.isNotEmpty() == true) Runnable { Ads.doAction(partner.action, requireContext()) } else null)

    private fun getPartnerInfo(name: Int, description: Int, disclaimer: Int, icon: Int, action: Runnable): PartnerInfo {
        return PartnerInfo(getString(name), getString(description), getString(disclaimer), icon, action)
    }
}
