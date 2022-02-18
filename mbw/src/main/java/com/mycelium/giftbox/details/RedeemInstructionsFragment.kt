package com.mycelium.giftbox.details

import android.os.Bundle
import android.view.View
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.mycelium.wallet.R
import kotlinx.android.synthetic.main.fragment_giftbox_redeem.*


class RedeemInstructionsFragment : Fragment(R.layout.fragment_giftbox_redeem) {
    private val args by navArgs<RedeemInstructionsFragmentArgs>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        text.text = HtmlCompat.fromHtml(args.product.redeemInstructionsHtml
                ?: "", HtmlCompat.FROM_HTML_MODE_LEGACY)
    }
}