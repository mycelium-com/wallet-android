package com.mycelium.giftbox.cards

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.mycelium.giftbox.buycard.GiftBoxBuyActivity
import com.mycelium.giftbox.cards.adapter.StoresAdapter
import com.mycelium.giftbox.model.Card
import com.mycelium.wallet.R
import kotlinx.android.synthetic.main.fragment_giftbox_stores.*


class StoresFragment : Fragment(R.layout.fragment_giftbox_stores) {
    private val adapter = StoresAdapter()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        list.adapter = adapter
        adapter.itemClickListener = {
            startActivity(Intent(requireContext(), GiftBoxBuyActivity::class.java))
        }
        adapter.submitList(listOf(
                Card("", "Amazon UK", "Food, books, electronics", 12),
                Card("", "Nike US", "Sport Clothes", 30),
                Card("", "Ikea IT", "Furniture", 1)))
    }
}