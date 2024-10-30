package com.mycelium.bequant.common

import android.app.Dialog
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.mycelium.wallet.R
import com.mycelium.wallet.databinding.DialogBequantDatePickerBinding
import java.util.*


class BQDatePickerDialog(val listener: (Int, Int, Int) -> Unit) : DialogFragment() {
    private var year = 1990
    private var month = 1
    private var day = 1
    var binding: DialogBequantDatePickerBinding? = null

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


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        DialogBequantDatePickerBinding.inflate(inflater, container, false).apply {
            binding = this
        }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.closeButton?.setOnClickListener { dismissAllowingStateLoss() }
        binding?.cancelButton?.setOnClickListener { dismissAllowingStateLoss() }
        binding?.datePicker?.init(year, month, day) { _, y, m, d ->
            year = y
            month = m
            day = d
        }
        binding?.datePicker?.maxDate = Date(2003, 1, 1).time
        binding?.okButton?.setOnClickListener {
            dismissAllowingStateLoss()
            listener.invoke(year, month, day)
        }
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}