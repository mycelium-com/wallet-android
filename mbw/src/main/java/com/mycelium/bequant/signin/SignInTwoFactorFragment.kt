package com.mycelium.bequant.signin

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.mycelium.bequant.common.ErrorHandler
import com.mycelium.bequant.common.loader
import com.mycelium.bequant.market.BequantMarketActivity
import com.mycelium.bequant.remote.model.BequantUserEvent
import com.mycelium.bequant.remote.repositories.Api
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import com.poovam.pinedittextfield.PinField
import kotlinx.android.synthetic.main.fragment_bequant_sign_in_two_factor.*


class SignInTwoFactorFragment : Fragment(R.layout.fragment_bequant_sign_in_two_factor) {

    val args by navArgs<SignInTwoFactorFragmentArgs>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val auth = args.auth
        (activity as AppCompatActivity?)?.supportActionBar?.run {
            title = getString(R.string.bequant_page_title_two_factor_auth)
            setHomeAsUpIndicator(resources.getDrawable(R.drawable.ic_bequant_arrow_back))
        }
        pasteFromClipboard.setOnClickListener {
            pinCode.setText(Utils.getClipboardString(requireContext()))
        }
        pinCode.onTextCompleteListener = object : PinField.OnTextCompleteListener {
            override fun onTextComplete(enteredText: String): Boolean {
                loader(true)
                Api.signRepository.authorize(this@SignInTwoFactorFragment.lifecycleScope, auth.copy(otpCode = enteredText), {
                    startActivity(Intent(requireContext(), BequantMarketActivity::class.java))
                    requireActivity().finish()
                    BequantUserEvent.SIGNIN.track()
                }, error = { _, message ->
                    ErrorHandler(requireContext()).handle(message)
                }, finally = {
                    loader(false)
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