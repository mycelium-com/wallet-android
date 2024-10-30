package com.mycelium.bequant.kyc.callback

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.mycelium.bequant.BequantPreference
import com.mycelium.bequant.BequantConstants
import com.mycelium.wallet.R
import com.mycelium.wallet.databinding.FragmentBequantKycRejectedCallbackBinding

class CallbackRejectedFragment : Fragment() {

    var binding: FragmentBequantKycRejectedCallbackBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentBequantKycRejectedCallbackBinding.inflate(inflater, container, false)
        .apply {
            binding = this
        }
        .root
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as AppCompatActivity?)?.supportActionBar?.run {
            title = getString(R.string.identity_auth)
            setHomeAsUpIndicator(resources.getDrawable(R.drawable.ic_bequant_clear))
        }
        val kycRequest = BequantPreference.getKYCRequest()
        binding?.dearUser?.text = if (kycRequest.first_name != null && kycRequest.last_name != null)
            getString(R.string.dear_user_s_s, kycRequest.first_name, kycRequest.last_name)
        else getString(R.string.dear_user)
        binding?.supportCenter?.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(BequantConstants.LINK_SUPPORT_CENTER)))
        }
        binding?.closeButton?.setOnClickListener {
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
