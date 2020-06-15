package com.mycelium.bequant.signin

import android.os.Bundle
import android.util.Patterns
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.mycelium.bequant.remote.SignRepository
import com.mycelium.bequant.remote.client.models.AccountPasswordResetRequest
import com.mycelium.wallet.R
import kotlinx.android.synthetic.main.fragment_bequant_sign_in_reset_password.*

class ResetPasswordFragment : Fragment(R.layout.fragment_bequant_sign_in_reset_password) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as AppCompatActivity?)?.supportActionBar?.title = getString(R.string.bequant_page_title_reset_password)
        (activity as AppCompatActivity?)?.supportActionBar?.setHomeAsUpIndicator(resources.getDrawable(R.drawable.ic_bequant_arrow_back))
        submit.setOnClickListener {
            if (validate()) {
                val email = email.text.toString()
                SignRepository.repository.resetPassword(this, AccountPasswordResetRequest(email), {
                    findNavController().navigate(ResetPasswordFragmentDirections.actionSubmit(email))
                }, {})
            }
        }
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