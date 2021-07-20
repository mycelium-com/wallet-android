package com.mycelium.wallet.activity.main

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.mycelium.giftbox.GiftBoxRootActivity.Companion.start
import com.mycelium.wallet.R
import kotlinx.android.synthetic.main.giftbox_banner_create.*

class GiftboxBannerFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? =
        inflater.inflate(R.layout.giftbox_banner_fragment, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        icClose.setOnClickListener {
           hide(requireContext())
        }
        btCreate.setOnClickListener {
            start(requireActivity())
        }
    }

    companion object {
        private const val BANNER_PREF: String = "banner_pref"
        private const val BANNER_CLOSED: String = "banner_closed"

        fun hide(context: Context){
            val sharedPreferences =
                context.getSharedPreferences(BANNER_PREF, Context.MODE_PRIVATE)
            sharedPreferences.edit()
                .putBoolean(BANNER_CLOSED, true)
                .apply()
        }

        fun isShouldBeShown(context: Context): Boolean {
            val sharedPreferences =
                context.getSharedPreferences(BANNER_PREF, Context.MODE_PRIVATE)
            return !sharedPreferences
                .getBoolean(BANNER_CLOSED, false)


        }
        @JvmStatic
        fun newInstance(): GiftboxBannerFragment {
            return GiftboxBannerFragment()
        }
    }
}