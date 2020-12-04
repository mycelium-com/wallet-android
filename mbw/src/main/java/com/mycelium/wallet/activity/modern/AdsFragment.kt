package com.mycelium.wallet.activity.modern

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.drawable.PictureDrawable
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.mycelium.wallet.R
import com.mycelium.wallet.external.partner.model.MainMenuPage
import com.mycelium.wallet.svg.GlideApp
import com.mycelium.wallet.svg.GlideRequests
import com.mycelium.wallet.svg.SvgSoftwareLayerSetter
import kotlinx.android.synthetic.main.fragment_margin_trade.*


class AdsFragment : Fragment(R.layout.fragment_margin_trade) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val pageData = arguments?.get("page") as MainMenuPage?
        val glideRequests: GlideRequests = GlideApp.with(banner)
        val glideRequest = if (pageData?.imageUrl?.endsWith(".svg") == true) glideRequests.`as`(PictureDrawable::class.java).listener(SvgSoftwareLayerSetter()) else glideRequests.asBitmap()
        glideRequest.load(pageData?.imageUrl).into(banner)
        banner.setOnClickListener {
            try {
                activity?.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(pageData?.link)))
            } catch (e: ActivityNotFoundException) {
            }
        }
    }
}