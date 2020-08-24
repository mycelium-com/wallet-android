package com.mycelium.bequant.common

import android.app.Dialog
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.mycelium.wallet.R
import kotlinx.android.synthetic.main.dialog_bequant_date_picker.view.*
import java.util.*


class BQDatePickerDialog(val listener: (Int, Int, Int) -> Unit) : DialogFragment() {
    private var year = 1990
    private var month = 1
    private var day = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, R.style.DatePickerDialog)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
            super.onCreateDialog(savedInstanceState).apply {
                requireActivity().let {
                    window?.setBackgroundDrawable(BitmapDrawable(it.resources, BlurBuilder.blur(it)))
                }
            }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.dialog_bequant_date_picker, container, false).apply {
                closeButton.setOnClickListener { dismissAllowingStateLoss() }
                cancelButton.setOnClickListener { dismissAllowingStateLoss() }
                datePicker.init(year, month, day) { _, y, m, d ->
                    year = y
                    month = m
                    day = d
                }
                datePicker.maxDate = Date(2003, 1, 1).time
                okButton.setOnClickListener {
                    dismissAllowingStateLoss()
                    listener.invoke(year, month, day)
                }
            }
}