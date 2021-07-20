package com.mycelium.wallet.activity.modern

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.mycelium.wallet.R
import com.mycelium.wallet.external.partner.model.MainMenuPage
import com.mycelium.wallet.external.partner.startContentLink
import kotlinx.android.synthetic.main.fragment_margin_trade.*


class AdsFragment : Fragment(R.layout.fragment_margin_trade) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val pageData = arguments?.get("page") as MainMenuPage?
        Glide.with(banner)
                .load(pageData?.imageUrl)
                .into(banner)
        banner.setOnClickListener {
            startContentLink(pageData?.link)
        }
    }
}