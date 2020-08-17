package com.mycelium.bequant.common

import android.app.Dialog
import android.content.res.Resources
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.DatePicker
import android.widget.LinearLayout
import android.widget.NumberPicker
import androidx.fragment.app.DialogFragment
import com.mycelium.wallet.R
import kotlinx.android.synthetic.main.dialog_bequant_date_picker.view.*
import java.util.*


class BQDatePickerDialog(val listener: (Int, Int, Int) -> Unit) : DialogFragment() {
    var year = 1990
    var month = 1
    var day = 1
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, R.style.DatePickerDialog)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
            super.onCreateDialog(savedInstanceState).apply {
                requireActivity().let {
                    this.window?.setBackgroundDrawable(BitmapDrawable(it.resources, BlurBuilder.blur(it)))
                }
            }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.dialog_bequant_date_picker, container, false).apply {
                this.closeButton.setOnClickListener { dismissAllowingStateLoss() }
                this.cancelButton.setOnClickListener { dismissAllowingStateLoss() }
                this.datePicker.init(year, month, day) { _, y, m, d ->
                    year = y
                    month = m
                    day = d
                }
                this.datePicker.maxDate = Date(2003, 1, 1).time
                this.okButton.setOnClickListener {
                    dismissAllowingStateLoss()
                    listener.invoke(year, month, day)
                }
            }
}