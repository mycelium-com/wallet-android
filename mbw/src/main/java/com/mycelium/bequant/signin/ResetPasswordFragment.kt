package com.mycelium.bequant.signin

import android.os.Bundle
import android.util.Patterns
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.mycelium.bequant.common.ErrorHandler
import com.mycelium.bequant.common.loader
import com.mycelium.bequant.remote.repositories.SignRepository
import com.mycelium.bequant.remote.client.models.AccountPasswordResetRequest
import com.mycelium.bequant.remote.repositories.Api
import com.mycelium.wallet.R
import kotlinx.android.synthetic.main.fragment_bequant_sign_in_reset_password.*

class ResetPasswordFragment : Fragment(R.layout.fragment_bequant_sign_in_reset_password) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as AppCompatActivity?)?.supportActionBar?.title = getString(R.string.bequant_page_title_reset_password)
        (activity as AppCompatActivity?)?.supportActionBar?.setHomeAsUpIndicator(resources.getDrawable(R.drawable.ic_bequant_arrow_back))
        submit.setOnClickListener {
            if (validate()) {
                loader(true)
                val email = email.text.toString()
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


    private fun validate(): Boolean {
        if (email.text.toString().isEmpty()) {
            emailLayout.error = "Can't be empty"
            return false
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email.text.toString()).matches()) {
            emailLayout.error = "Not email"
            return false
        }
        return true
    }
}