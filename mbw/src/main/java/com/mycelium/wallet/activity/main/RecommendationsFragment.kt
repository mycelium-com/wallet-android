package com.mycelium.wallet.activity.main

import android.app.AlertDialog
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
import com.mycelium.wallet.databinding.MainRecommendationsViewBinding
import com.mycelium.wallet.external.Ads
import com.mycelium.wallet.external.partner.model.Partner
import com.mycelium.wallet.external.partner.startContentLink

class RecommendationsFragment : Fragment() {
    private var alertDialog: AlertDialog? = null
    private var binding: MainRecommendationsViewBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        setHasOptionsMenu(true)
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
            MainRecommendationsViewBinding.inflate(inflater, container, false).apply {
                binding = this
            }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.list?.addItemDecoration(DividerItemDecoration(resources.getDrawable(R.drawable.divider_account_list), LinearLayoutManager.VERTICAL)
                .apply { setFromItem(1) })

        val adapter = RecommendationAdapter(mutableListOf<RecommendationInfo>().apply {
            add(RecommendationHeader(SettingsPreference.getPartnersHeaderTitle(),
                    SettingsPreference.getPartnersHeaderText()))
            SettingsPreference.getPartners()
                    ?.filter { it.isActive() && SettingsPreference.isContentEnabled(it.parentId) }
                    ?.forEach {
                add(getPartnerInfo(it))
            }
            add(RecommendationFooter())
        })
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
                                startContentLink(bean.uri)
                            }
                            .setNegativeButton(cancel, null)
                            .create()
                    alertDialog?.show()
                } else {
                    startContentLink(bean.uri)
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
        binding?.list?.adapter = adapter
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
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
}
