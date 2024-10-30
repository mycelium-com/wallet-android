package com.mycelium.bequant.common

import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.mycelium.wallet.R
import com.mycelium.wallet.databinding.DialogBequantModalBinding

class ModalDialog(val title: String,
                  val message: String,
                  val actionText: String,
                  val action: () -> Unit) : DialogFragment() {

    var binding: DialogBequantModalBinding? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, R.style.Theme_D1NoTitleDim)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        DialogBequantModalBinding.inflate(inflater, container, false).apply {
            binding = this
        }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.titleView?.text = title
        binding?.messageView?.text = message
        binding?.actionButton?.text = actionText
        binding?.closeButton?.setOnClickListener { dismissAllowingStateLoss() }
        binding?.actionButton?.setOnClickListener {
            dismissAllowingStateLoss()
            action.invoke()
        }
        requireActivity().let {
            binding?.root?.setBackgroundDrawable(BitmapDrawable(it.resources, BlurBuilder.blur(it)))
        }
    }
}