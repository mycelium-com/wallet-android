package com.mycelium.bequant.common

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
import com.mycelium.bequant.Constants
import com.mycelium.wallet.R


class LoaderFragment : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
            window?.requestFeature(Window.FEATURE_NO_TITLE);
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.layout_bequant_loading, container, false)

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT));
    }
}

fun Fragment.loader(show: Boolean) {
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

fun AppCompatActivity.loader(show: Boolean) {
    if (show) {
        val loader = LoaderFragment()
        loader.show(supportFragmentManager, Constants.LOADER_TAG)
    } else {
        val findFragmentByTag = supportFragmentManager.findFragmentByTag(Constants.LOADER_TAG)
        if (findFragmentByTag is LoaderFragment && findFragmentByTag.isAdded) {
            findFragmentByTag.dismissAllowingStateLoss()
        }
    }
}