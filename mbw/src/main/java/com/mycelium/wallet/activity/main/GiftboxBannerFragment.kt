package com.mycelium.wallet.activity.main

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

//    override fun onCreate(savedInstanceState: Bundle?) {
//        setHasOptionsMenu(false)
//        super.onCreate(savedInstanceState)
//    }
//
//    override fun onStart() {
//        MbwManager.getEventBus().register(this)
//        super.onStart()
//    }
//
//    override fun onStop() {
//        MbwManager.getEventBus().unregister(this)
//        super.onStop()
//    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        icClose.setOnClickListener {
//            sharedPreferences.edit()
//                    .putBoolean(if (isAccountsListBanner) SHOW_FIO_CREATE_ACCOUNT_BANNER else
//                        "$SHOW_FIO_CREATE_NAME_BANNER${mbwManager.selectedAccount.label}", false)
//                    .apply()
//            recheckBanner()
        }
        btCreate.setOnClickListener {
            start(requireActivity())
        }
    }

    companion object {


        @JvmStatic
        fun newInstance(): GiftboxBannerFragment {
            return GiftboxBannerFragment()
        }
    }
}