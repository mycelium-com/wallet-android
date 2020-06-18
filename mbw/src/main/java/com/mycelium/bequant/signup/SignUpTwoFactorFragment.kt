package com.mycelium.bequant.signup

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.mycelium.bequant.Constants.LINK_GOOGLE_AUTHENTICATOR
import com.mycelium.wallet.R
import kotlinx.android.synthetic.main.fragment_bequant_sign_up_two_factor.*


class SignUpTwoFactorFragment : Fragment(R.layout.fragment_bequant_sign_up_two_factor) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as AppCompatActivity?)?.supportActionBar?.title = getString(R.string.bequant_page_title_two_factor_auth)
        (activity as AppCompatActivity?)?.supportActionBar?.setHomeAsUpIndicator(resources.getDrawable(R.drawable.ic_bequant_arrow_back))
        next.setOnClickListener {
            findNavController().navigate(SignUpTwoFactorFragmentDirections.actionNext())
        }
        download.setOnClickListener {
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

}