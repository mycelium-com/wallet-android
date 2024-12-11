package com.mycelium.bequant.signin

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.mycelium.bequant.common.ErrorHandler
import com.mycelium.bequant.common.loader
import com.mycelium.bequant.remote.client.models.AccountPasswordResetRequest
import com.mycelium.bequant.remote.repositories.Api
import com.mycelium.wallet.R
import com.mycelium.wallet.databinding.FragmentBequantSignInResetPasswordBinding

class ResetPasswordFragment : Fragment(R.layout.fragment_bequant_sign_in_reset_password) {

    var binding: FragmentBequantSignInResetPasswordBinding? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentBequantSignInResetPasswordBinding.inflate(inflater, container, false)
        .apply {
            binding = this
        }
        .root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as AppCompatActivity?)?.supportActionBar?.run {
            title = getString(R.string.bequant_page_title_reset_password)
            setHomeAsUpIndicator(resources.getDrawable(R.drawable.ic_bequant_arrow_back))
        }
        binding?.submit?.setOnClickListener {
            if (validate()) {
                loader(true)
                val email = binding?.email?.text.toString()
                Api.signRepository.resetPassword(lifecycleScope, AccountPasswordResetRequest(email), {
                    findNavController().navigate(ResetPasswordFragmentDirections.actionSubmit(email))
                }, error = { _, message ->
                    ErrorHandler(requireContext()).handle(message)
                }, finally = {
                    loader(false)
                })
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

    private fun validate(): Boolean {
        if (binding?.email?.text?.toString()?.isEmpty() == true) {
            binding?.emailLayout?.error = "Can't be empty"
            return false
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(binding?.email?.text.toString()).matches()) {
            binding?.emailLayout?.error = "Not email"
            return false
        }
        return true
    }
}