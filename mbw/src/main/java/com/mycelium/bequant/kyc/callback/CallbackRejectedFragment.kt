package com.mycelium.bequant.kyc.callback

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.mycelium.bequant.BequantPreference
import com.mycelium.bequant.BequantConstants
import com.mycelium.wallet.R
import kotlinx.android.synthetic.main.fragment_bequant_kyc_rejected_callback.*

class CallbackRejectedFragment : Fragment(R.layout.fragment_bequant_kyc_rejected_callback) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as AppCompatActivity?)?.supportActionBar?.run {
            title = getString(R.string.identity_auth)
            setHomeAsUpIndicator(resources.getDrawable(R.drawable.ic_bequant_clear))
        }
        val kycRequest = BequantPreference.getKYCRequest()
        dear_user.text = if (kycRequest.first_name != null && kycRequest.last_name != null)
            getString(R.string.dear_user_s_s, kycRequest.first_name, kycRequest.last_name)
        else getString(R.string.dear_user)
        supportCenter.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(BequantConstants.LINK_SUPPORT_CENTER)))
        }
        closeButton.setOnClickListener {
            requireActivity().finish()
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
