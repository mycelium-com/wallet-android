package com.mycelium.wallet.activity

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.fragment.app.DialogFragment
import com.mycelium.wallet.activity.util.MasterseedPasswordSetter
import com.mycelium.wallet.databinding.PassphraseDialogBinding

class MasterseedPasswordDialog : DialogFragment() {

    var binding: PassphraseDialogBinding? = null

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("pwd", binding?.etPassphrase?.getText().toString())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View =
        PassphraseDialogBinding.inflate(inflater).apply {
            binding = this
        }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog!!.setTitle("Passphrase")
        dialog!!.setCanceledOnTouchOutside(false)

        if (savedInstanceState != null) {
            binding?.etPassphrase?.setText(savedInstanceState.getString("pwd"))
        }
        // show/hide password
        binding?.cbShowPassword?.setOnCheckedChangeListener(object :
            CompoundButton.OnCheckedChangeListener {
            override fun onCheckedChanged(compoundButton: CompoundButton?, b: Boolean) {
                binding?.etPassphrase?.let { etPassword ->
                    etPassword.setInputType(InputType.TYPE_CLASS_TEXT or (if (b) InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD else InputType.TYPE_TEXT_VARIATION_PASSWORD))
                    // Set cursor to last position
                    etPassword.setSelection(etPassword.getText().length)
                }
            }
        })

        // Okay button
        binding?.btnOkay?.setOnClickListener {
            val text = binding?.etPassphrase?.getText().toString()
            (activity as MasterseedPasswordSetter).setPassphrase(text)
            dismiss()
        }

        // Cancel button
        binding?.btnCancel?.setOnClickListener {
            (activity as MasterseedPasswordSetter).setPassphrase(null)
            dismiss()
        }
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}