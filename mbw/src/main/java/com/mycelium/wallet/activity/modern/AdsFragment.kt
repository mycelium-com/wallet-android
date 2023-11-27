package com.mycelium.wallet.activity.modern

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.modern.event.RemoveTab
import com.mycelium.wallet.activity.settings.SettingsPreference
import com.mycelium.wallet.databinding.FragmentMarginTradeBinding
import com.mycelium.wallet.external.partner.model.MainMenuPage
import com.mycelium.wallet.external.partner.startContentLink


class AdsFragment : Fragment() {

    private var binding: FragmentMarginTradeBinding? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentMarginTradeBinding.inflate(inflater, container, false).apply {
        binding = this
    }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val pageData = arguments?.get("page") as MainMenuPage?
        binding?.banner?.run {
            Glide.with(this)
                .load(pageData?.imageUrl)
                .into(this)
            setOnClickListener {
                startContentLink(pageData?.link)
            }
        }
        binding?.close?.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setMessage(getString(R.string.hide_this_ad))
                .setPositiveButton(getString(R.string.hide)) { _, _ ->
                    SettingsPreference.setEnabled(pageData?.parentId.orEmpty(), false)
                    MbwManager.getEventBus().post(RemoveTab(arguments?.getString("tag").orEmpty()))
                }
                .setNegativeButton(R.string.cancel) { _, _ -> }
                .show()
        }
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}