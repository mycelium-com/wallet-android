package com.mycelium.giftbox.details

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.mycelium.wallet.R


class RedeemInstructionsFragment : Fragment(R.layout.fragment_giftbox_redeem) {
    private val args by navArgs<RedeemInstructionsFragmentArgs>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<TextView>(R.id.text).text = HtmlCompat.fromHtml(args.product.redeemInstructionsHtml
                ?: "", HtmlCompat.FROM_HTML_MODE_LEGACY)
    }
}