package com.mycelium.wallet.activity.fio

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.fragment.app.DialogFragment
import com.mycelium.wallet.R
import com.mycelium.wallet.databinding.DialogExpirationDetailsBinding

class ExpirationDetailsDialog : DialogFragment() {

    var binding: DialogExpirationDetailsBinding? = null
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
            super.onCreateDialog(savedInstanceState).apply {
                window?.requestFeature(Window.FEATURE_NO_TITLE)
            }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.CustomDialog)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        DialogExpirationDetailsBinding.inflate(inflater, container, false).apply {
            binding = this
        }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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