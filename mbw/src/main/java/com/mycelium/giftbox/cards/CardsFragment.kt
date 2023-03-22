package com.mycelium.giftbox.cards

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import com.mycelium.giftbox.GiftboxPreference
import com.mycelium.giftbox.cards.adapter.CardAdapter
import com.mycelium.giftbox.cards.adapter.CardItem
import com.mycelium.giftbox.cards.adapter.CardListItem
import com.mycelium.giftbox.cards.adapter.GroupItem
import com.mycelium.giftbox.cards.event.OrdersUpdate
import com.mycelium.giftbox.cards.event.RefreshOrdersRequest
import com.mycelium.giftbox.cards.viewmodel.GiftBoxViewModel
import com.mycelium.giftbox.client.GitboxAPI
import com.mycelium.giftbox.model.Card
import com.mycelium.giftbox.shareGiftcard
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.modern.Toaster
import com.mycelium.wallet.databinding.FragmentGiftboxPurchasedBinding
import com.mycelium.wallet.startCoroutineTimer
import com.squareup.otto.Subscribe
import java.util.concurrent.TimeUnit


class CardsFragment : Fragment() {
    private val cards = mutableListOf<Card>()
    private val adapter = CardAdapter()
    private var binding: FragmentGiftboxPurchasedBinding? = null
    private val activityViewModel: GiftBoxViewModel by activityViewModels()

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View =
            FragmentGiftboxPurchasedBinding.inflate(inflater).apply {
                binding = this
            }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.list?.adapter = adapter
        binding?.list?.addItemDecoration(
                com.mycelium.wallet.activity.view.DividerItemDecoration(
                        resources.getDrawable(R.drawable.divider_bequant),
                        DividerItemDecoration.VERTICAL
                )
        )
        binding?.noResultTitle?.text = getString(R.string.no_purchased_gift_card)
        binding?.noResultText?.text = getString(R.string.no_gift_cards_linked)
        adapter.itemClickListener = {
            findNavController().navigate(GiftBoxFragmentDirections.actionCardDetails(it))
        }
        adapter.itemShareListener = {
            shareGiftcard(it)
        }
        adapter.itemRedeemListener = {
            GitboxAPI.giftRepository.redeem(it, lifecycleScope) {
                loadData()
            }
        }
        adapter.itemDeleteListener = {
            AlertDialog.Builder(requireContext(), R.style.MyceliumModern_Dialog)
                    .setTitle(getString(R.string.delete_gift_card))
                    .setMessage(getString(R.string.delete_gift_card_msg))
                    .setNegativeButton(R.string.button_cancel) { _, _ -> }
                    .setPositiveButton(R.string.delete) { _, _ ->
                        GitboxAPI.giftRepository.remove(it, lifecycleScope) {
                            loadData()
                        }
                    }
                    .create().show()
        }
        adapter.groupListener = { group ->
            GiftboxPreference.setGroupOpen(group, !GiftboxPreference.isGroupOpen(group))
            adapter.submitList(generateList(cards))
            binding?.list?.postDelayed({
                binding?.list?.smoothScrollToPosition(adapter.currentList.indexOfFirst { it is GroupItem && it.title == group } + 5)
            }, 300)
        }
        activityViewModel.currentTab.observeForever { binding?.list?.scrollToPosition(0) }
        startCoroutineTimer(lifecycleScope, repeatMillis = TimeUnit.MINUTES.toMillis(1)) {
            loadData()
        }
        MbwManager.getEventBus().register(this)
    }

    fun loadData() {
        GitboxAPI.giftRepository.getCards(lifecycleScope, { data ->
            cards.clear()
            cards.addAll((data ?: emptyList()).sortedByDescending { it.timestamp })
            binding?.noResultText?.visibility = if (cards.isEmpty()) VISIBLE else GONE
            binding?.noResultTitle?.visibility = if (cards.isEmpty()) VISIBLE else GONE
            adapter.submitList(generateList(cards))
        }, { code, msg ->
            if(code != 400) {
                Toaster(this).toast(msg, true)
            }
        })
    }

    private fun generateList(data: List<Card>) = mutableListOf<CardListItem>().apply {
        addAll(data.filter { !it.isRedeemed }.map { CardItem(it) })
        val redeemed = data.filter { it.isRedeemed }.map { CardItem(it, true) }
        if (redeemed.isNotEmpty()) {
            val redeemedGroup = getString(R.string.redeemed_gift_cards)
            val isOpened = GiftboxPreference.isGroupOpen(redeemedGroup)
            add(GroupItem(redeemedGroup, isOpened))
            if (isOpened) {
                addAll(redeemed)
            }
        }
    }

    override fun onDestroyView() {
        MbwManager.getEventBus().unregister(this)
        binding = null
        super.onDestroyView()
    }

    @Subscribe
    internal fun updateOrder(request: RefreshOrdersRequest) {
        loadData()
    }

    @Subscribe
    internal fun updateOrder(event: OrdersUpdate) {
        loadData()
    }
}