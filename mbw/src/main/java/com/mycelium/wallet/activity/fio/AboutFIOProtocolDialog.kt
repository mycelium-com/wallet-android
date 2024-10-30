package com.mycelium.wallet.activity.fio

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window.FEATURE_NO_TITLE
import androidx.fragment.app.DialogFragment
import com.mycelium.wallet.R
import com.mycelium.wallet.databinding.DialogAboutFioProtocolBinding
import com.mycelium.wallet.external.partner.openLink


class AboutFIOProtocolDialog : DialogFragment() {

    var binding: DialogAboutFioProtocolBinding? = null
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
            super.onCreateDialog(savedInstanceState).apply {
                window?.requestFeature(FEATURE_NO_TITLE)
            }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.CustomDialog);
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        DialogAboutFioProtocolBinding.inflate(inflater, container, false).apply {
            binding = this
        }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.fioProtocolText?.setOnClickListener {
            openLink("https://kb.fioprotocol.io/fio-protocol/fio-overview")
        }
        binding?.toolbar?.setNavigationOnClickListener {
            dismissAllowingStateLoss()
        }
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }
}