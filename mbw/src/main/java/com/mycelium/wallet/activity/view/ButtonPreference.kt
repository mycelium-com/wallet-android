package com.mycelium.wallet.activity.view

import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.settings.ModulePreference


class ButtonPreference(context: Context) : Preference(context), ModulePreference {
    var button: TextView? = null
    var underIconTextView: TextView? = null
    var syncState: TextView? = null


    private var buttonClickListener: View.OnClickListener? = null
    private var buttonText: String? = null
    private var syncStateText: String? = null
    private var buttonEnabled = true

    init {
        layoutResource = R.layout.preference_layout_no_icon
        widgetLayoutResource = R.layout.preference_button
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        button = holder.itemView.findViewById(R.id.preference_button)
        underIconTextView = holder.itemView.findViewById(R.id.under_icon_text)
        syncState = holder.itemView.findViewById(R.id.sync_state)

        button?.text = buttonText
        button?.setOnClickListener { btnClick(it) }

        syncState?.text = syncStateText
        setButtonEnabled(buttonEnabled)
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        button?.isEnabled = enabled
    }

    fun btnClick(view: View?) {
        buttonClickListener?.onClick(view)
    }

    fun setButtonClickListener(buttonClickListener: View.OnClickListener?) {
        this.buttonClickListener = buttonClickListener
    }

    fun setButtonText(text: String?) {
        buttonText = text
        button?.text = text
    }

    override fun setSyncStateText(syncStateText: String) {
        this.syncStateText = syncStateText
        syncState?.text = syncStateText
    }

    fun setButtonEnabled(enabled: Boolean) {
        buttonEnabled = enabled
        button?.isClickable = enabled
        button?.isEnabled = enabled
    }
}
