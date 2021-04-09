package com.mycelium.bequant.common

import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.mycelium.wallet.R
import kotlinx.android.synthetic.main.dialog_bequant_modal.view.*

class ModalDialog(val title: String,
                  val message: String,
                  val actionText: String,
                  val action: () -> Unit) : DialogFragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, R.style.Theme_D1NoTitleDim)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.dialog_bequant_modal, container, false).apply {
                titleView.text = title
                messageView.text = message
                actionButton.text = actionText
                closeButton.setOnClickListener { dismissAllowingStateLoss() }
                actionButton.setOnClickListener {
                    dismissAllowingStateLoss()
                    action.invoke()
                }
                requireActivity().let {
                    root.setBackgroundDrawable(BitmapDrawable(it.resources, BlurBuilder.blur(it)))
                }
            }

}