package com.mycelium.wallet.activity.main

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.mycelium.wallet.activity.settings.SettingsPreference
import com.mycelium.wallet.databinding.LayoutTopBannerBinding
import com.mycelium.wallet.external.partner.startContentLink
import com.mycelium.wallet.randomOrNull

class AccountsTopBannerFragment : Fragment() {
    private var binding: LayoutTopBannerBinding? = null
    private val preference by lazy { requireActivity().getSharedPreferences(ACCOUNTS_TOP_BANNER_PREF, Context.MODE_PRIVATE) }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View =
            LayoutTopBannerBinding.inflate(inflater).apply {
                binding = this
            }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        SettingsPreference.getAccountsContent()?.bannersTop
                ?.filter {
                    it.isActive() && preference.getBoolean(it.parentId, true)
                            && SettingsPreference.isContentEnabled(it.parentId)
                }?.randomOrNull()?.let { banner ->
                    binding?.topBanner?.visibility = View.VISIBLE
                    Glide.with(binding?.bannerImage!!)
                            .load(banner.imageUrl)
                            .into(binding?.bannerImage!!)
                    binding?.topBanner?.setOnClickListener {
                        startContentLink(banner.link)
                    }
                    binding?.bannerClose?.setOnClickListener {
                        binding?.topBanner?.visibility = View.GONE
                        preference.edit().putBoolean(banner.parentId, false).apply()
                    }
                }
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    companion object {
        const val ACCOUNTS_TOP_BANNER_PREF = "accounts_top_banner"
    }
}