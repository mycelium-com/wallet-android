package com.mycelium.bequant.kyc

import android.app.Dialog
import android.app.ProgressDialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.mycelium.wallet.R

class ProgressDialogFragment : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        var message: String? = requireArguments().getString(ARG_MESSAGE, null)
        if (message == null) {
            message = getString(requireArguments().getInt(ARG_MESSAGE))
            if (message == null) {
                message = getString(R.string.loading)
            }
        }
        val indeterminate = requireArguments().getBoolean(ARG_INDETERMINATE, true)
        val progressDialog = ProgressDialog(activity)
        progressDialog.setMessage(message)
        progressDialog.isIndeterminate = indeterminate
        return progressDialog
    }

    companion object {
        private const val ARG_MESSAGE = "message"
        private const val ARG_INDETERMINATE = "indeterminate"
        var DIALOG_INDETERMINATE = true
        var DIALOG_CANCELABLE = true

        @JvmOverloads
        fun newInstance(message: Int = R.string.loading, indeterminate: Boolean = DIALOG_INDETERMINATE, cancelable: Boolean = DIALOG_CANCELABLE): ProgressDialogFragment {
            val args = Bundle()
            args.putInt(ARG_MESSAGE, message)
            args.putBoolean(ARG_INDETERMINATE, indeterminate)
            val progressDialogFragment = ProgressDialogFragment()
            progressDialogFragment.arguments = args
            progressDialogFragment.isCancelable = cancelable
            return progressDialogFragment
        }

        fun newInstance(message: String?, indeterminate: Boolean = DIALOG_INDETERMINATE, cancelable: Boolean = DIALOG_CANCELABLE): ProgressDialogFragment {
            val args = Bundle()
            args.putString(ARG_MESSAGE, message)
            args.putBoolean(ARG_INDETERMINATE, indeterminate)
            val progressDialogFragment = ProgressDialogFragment()
            progressDialogFragment.arguments = args
            progressDialogFragment.isCancelable = cancelable
            return progressDialogFragment
        }
    }
}

fun Fragment.showLoading() {
    hideProgress()
    val newInstance = ProgressDialogFragment.newInstance()
    newInstance.show(parentFragmentManager, "pd")
}

fun Fragment.hideProgress() {
    val findFragmentByTag = parentFragmentManager?.findFragmentByTag("pd")
    if (findFragmentByTag != null && findFragmentByTag.isAdded && findFragmentByTag is ProgressDialogFragment) {
        findFragmentByTag.dismiss()
    }
}