package com.mycelium.wallet.activity.fio

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window.FEATURE_NO_TITLE
import androidx.fragment.app.DialogFragment
import com.mycelium.wallet.R
import com.mycelium.wallet.external.partner.openLink
import kotlinx.android.synthetic.main.dialog_about_fio_protocol.*


class AboutFIOProtocolDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
            super.onCreateDialog(savedInstanceState).apply {
                window?.requestFeature(FEATURE_NO_TITLE)
            }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.CustomDialog);
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.dialog_about_fio_protocol, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fioProtocolText.setOnClickListener {
            openLink("https://kb.fioprotocol.io/fio-protocol/fio-overview")
        }
        toolbar.setNavigationOnClickListener {
            dismissAllowingStateLoss()
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }
}