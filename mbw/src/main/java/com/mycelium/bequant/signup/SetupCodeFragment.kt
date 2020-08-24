package com.mycelium.bequant.signup

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.mycelium.bequant.signup.viewmodel.SetupCodeViewModel
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import com.mycelium.wallet.databinding.FragmentBequantSetupCodeBindingImpl
import kotlinx.android.synthetic.main.fragment_bequant_setup_code.*


class SetupCodeFragment : Fragment() {
    lateinit var viewModel: SetupCodeViewModel
    val args by navArgs<SetupCodeFragmentArgs>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        viewModel = ViewModelProviders.of(this).get(SetupCodeViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            DataBindingUtil.inflate<FragmentBequantSetupCodeBindingImpl>(inflater, R.layout.fragment_bequant_setup_code, container, false)
                    .apply {
                        viewModel = this@SetupCodeFragment.viewModel
                        lifecycleOwner = this@SetupCodeFragment
                    }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as AppCompatActivity?)?.supportActionBar?.run {
            title = getString(R.string.bequant_page_title_setup_code)
            setHomeAsUpIndicator(resources.getDrawable(R.drawable.ic_bequant_arrow_back))
        }
        val uri = Uri.parse(args.otp.otpLink)
        viewModel.name.value = uri.pathSegments[0]
        viewModel.secretCode.value = uri.getQueryParameter("secret")
        qrCodeView.qrCode = args.otp.otpLink
        nameCopy.setOnClickListener {
            Utils.setClipboardString(viewModel.name.value, requireContext())
            Toast.makeText(requireContext(), getString(R.string.s_copied_to_clipboard, "Name"), Toast.LENGTH_SHORT).show()
        }
        secretCodeCopy.setOnClickListener {
            Utils.setClipboardString(viewModel.secretCode.value, requireContext())
            Toast.makeText(requireContext(), getString(R.string.s_copied_to_clipboard, "Secret"), Toast.LENGTH_SHORT).show()
        }
        next.setOnClickListener {
            findNavController().navigate(SetupCodeFragmentDirections.actionNext(args.otp))
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