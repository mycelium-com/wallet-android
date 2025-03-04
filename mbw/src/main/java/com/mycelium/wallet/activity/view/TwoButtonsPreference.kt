package com.mycelium.wallet.activity.view

import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.settings.ModulePreference
import com.mycelium.wallet.databinding.TwoButtonPreferenceBinding


class TwoButtonsPreference(context: Context) : Preference(context),
    ModulePreference {

    private var topButtonClickListener: View.OnClickListener? = null
    private var bottomButtonClickListener: View.OnClickListener? = null
    private var topButtonText: String? = null
    private var bottomButtonText: String? = null
    private var topButtonEnabled = false
    private var bottomButtonEnabled = false
    private var syncStateText: String? = null

    var binding: TwoButtonPreferenceBinding? = null
    var syncState: TextView? = null

    init {
        layoutResource = R.layout.preference_layout_no_icon
        widgetLayoutResource = R.layout.two_button_preference
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        binding = TwoButtonPreferenceBinding.bind(holder.itemView).apply {
            topButton.text = topButtonText
            bottomButton.text = bottomButtonText
            topButton.isEnabled = topButtonEnabled
            bottomButton.isEnabled = bottomButtonEnabled
            topButton.setOnClickListener(topButtonClickListener)
            bottomButton.setOnClickListener(bottomButtonClickListener)
        }
        syncState = holder.itemView.findViewById(R.id.sync_state)
        syncState?.text = syncStateText
    }

    fun setTopButtonClickListener(buttonClickListener: View.OnClickListener?) {
        topButtonClickListener = buttonClickListener
    }

    fun setBottomButtonClickListener(buttonClickListener: View.OnClickListener?) {
        bottomButtonClickListener = buttonClickListener
    }

    fun setEnabled(
        preferenceEnabled: Boolean,
        topButtonEnabled: Boolean,
        bottomButtonEnabled: Boolean
    ) {
        this.topButtonEnabled = topButtonEnabled
        this.bottomButtonEnabled = bottomButtonEnabled
        this.isEnabled = preferenceEnabled
        binding?.topButton?.isEnabled = topButtonEnabled
        binding?.bottomButton?.isEnabled = bottomButtonEnabled
    }

    fun setButtonsText(topButtonText: String?, bottomButtonText: String?) {
        this.bottomButtonText = bottomButtonText
        this.topButtonText = topButtonText

        binding?.bottomButton?.text = bottomButtonText
        binding?.topButton?.text = topButtonText
    }

    override fun setSyncStateText(syncStateText: String) {
        this.syncStateText = syncStateText
        syncState?.text = syncStateText
    }
}
