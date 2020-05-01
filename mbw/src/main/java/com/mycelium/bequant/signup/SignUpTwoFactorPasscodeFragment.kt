package com.mycelium.bequant.signup

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.mycelium.bequant.common.ErrorHandler
import com.mycelium.bequant.common.LoaderFragment
import com.mycelium.bequant.remote.SignRepository
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import com.poovam.pinedittextfield.PinField
import kotlinx.android.synthetic.main.fragment_bequant_sign_in_two_factor.*


class SignUpTwoFactorPasscodeFragment : Fragment(R.layout.fragment_bequant_sign_in_two_factor) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as AppCompatActivity?)?.supportActionBar?.title = getString(R.string.bequant_page_title_two_factor_auth)
        (activity as AppCompatActivity?)?.supportActionBar?.setHomeAsUpIndicator(resources.getDrawable(R.drawable.ic_bequant_arrow_back))
        val otpId = arguments?.getInt("otpId")
        pasteFromClipboard.setOnClickListener {
            pinCode.setText(Utils.getClipboardString(requireContext()))
        }
        pinCode.onTextCompleteListener = object : PinField.OnTextCompleteListener {
            override fun onTextComplete(enteredText: String): Boolean {
                val loader = LoaderFragment()
                loader.show(parentFragmentManager, "loader")
                SignRepository.repository.totpActivate(otpId!!, enteredText, {
                    loader.dismissAllowingStateLoss()
                    findNavController().navigate(SignUpTwoFactorPasscodeFragmentDirections.actionNext())
                }, {
                    loader.dismissAllowingStateLoss()
                    ErrorHandler(requireContext()).handle(it)
                })
                return true
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
            when (item.itemId) {
                android.R.id.home -> {
                    activity?.onBackPressed()
                    true
                }
                else -> super.onOptionsItemSelected(item)
            }
}