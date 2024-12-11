package com.mycelium.bequant.signup

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.mycelium.bequant.BequantConstants.LINK_GOOGLE_AUTHENTICATOR
import com.mycelium.wallet.R
import com.mycelium.wallet.databinding.FragmentBequantSignUpTwoFactorBinding


class SignUpTwoFactorFragment : Fragment() {
    var binding: FragmentBequantSignUpTwoFactorBinding? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentBequantSignUpTwoFactorBinding.inflate(inflater, container, false)
        .apply {
            binding = this
        }
        .root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as AppCompatActivity?)?.supportActionBar?.run {
            title = getString(R.string.bequant_page_title_two_factor_auth)
            setHomeAsUpIndicator(resources.getDrawable(R.drawable.ic_bequant_arrow_back))
        }
        binding?.next?.setOnClickListener {
            findNavController().navigate(SignUpTwoFactorFragmentDirections.actionNext())
        }
        binding?.download?.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(LINK_GOOGLE_AUTHENTICATOR)))
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