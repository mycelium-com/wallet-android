package com.mycelium.wallet.activity.view

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.mycelium.bequant.BequantConstants as Constants
import com.mycelium.wallet.R
import com.mycelium.wallet.databinding.LayoutLoadingBinding


class LoaderFragment(val message: String? = null) : DialogFragment() {

    var binding: LayoutLoadingBinding? = null
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
            window?.requestFeature(Window.FEATURE_NO_TITLE)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View =
        LayoutLoadingBinding.inflate(inflater, container, false)
            .apply {
                binding = this
            }
            .root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.loaderText?.text = message ?: getString(R.string.loading)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}

fun Fragment.loader(show: Boolean) {
    if (!isAdded) {
        return
    }
    if (show) {
        val loader = LoaderFragment()
        loader.show(childFragmentManager, Constants.LOADER_TAG)
    } else {
        childFragmentManager.executePendingTransactions()
        val findFragmentByTag = childFragmentManager.findFragmentByTag(Constants.LOADER_TAG)
        if (findFragmentByTag is LoaderFragment && findFragmentByTag.isAdded) {
            findFragmentByTag.dismissAllowingStateLoss()
        }
    }
}

fun AppCompatActivity.loader(show: Boolean, message: String? = null) {
    if (show) {
        val loader = LoaderFragment(message)
        loader.show(supportFragmentManager, Constants.LOADER_TAG)
    } else {
        val findFragmentByTag = supportFragmentManager.findFragmentByTag(Constants.LOADER_TAG)
        if (findFragmentByTag is LoaderFragment && findFragmentByTag.isAdded) {
            findFragmentByTag.dismissAllowingStateLoss()
        }
    }
}

