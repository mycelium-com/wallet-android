package com.mycelium.bequant.signin

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
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
import com.mycelium.wallet.databinding.FragmentBequantSignInTwoFactorBinding
import com.poovam.pinedittextfield.PinField


class SignInTwoFactorFragment : Fragment() {

    val args by navArgs<SignInTwoFactorFragmentArgs>()
    var binding: FragmentBequantSignInTwoFactorBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentBequantSignInTwoFactorBinding.inflate(inflater, container, false)
        .apply {
            binding = this
        }
        .root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val auth = args.auth
        (activity as AppCompatActivity?)?.supportActionBar?.run {
            title = getString(R.string.bequant_page_title_two_factor_auth)
            setHomeAsUpIndicator(resources.getDrawable(R.drawable.ic_bequant_arrow_back))
        }
        binding?.pasteFromClipboard?.setOnClickListener {
            binding?.pinCode?.setText(Utils.getClipboardString(requireContext()))
        }
        binding?.pinCode?.onTextCompleteListener = object : PinField.OnTextCompleteListener {
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

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}