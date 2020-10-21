package com.mycelium.wallet.activity.fio.registerdomain

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.fio.registername.RegisterFioNameActivity
import com.mycelium.wapi.wallet.Util.transformExpirationDate
import kotlinx.android.synthetic.main.fragment_register_fio_domain_completed.*

class RegisterFioDomainCompletedFragment : Fragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // without this the navigation through back button would return to previous fragment (name registration and the payment)
        // but the desired behavior here is to finish the activity as we have completed the registration
        requireActivity().onBackPressedDispatcher.addCallback(this) {
            requireActivity().finish()
        }.apply { this.isEnabled = true }
    }

    private val fioDomain: String by lazy {
        requireArguments().getSerializable("fioDomain") as String
    }
    private val fioAccountLabel: String by lazy {
        requireArguments().getSerializable("fioAccountLabel") as String
    }
    private val expirationDate: String by lazy {
        requireArguments().getSerializable("expirationDate") as String
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_register_fio_domain_completed, container, false)

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity() as AppCompatActivity).supportActionBar!!.apply {
            title = getString(R.string.fio_registration_complete)
            setDisplayHomeAsUpEnabled(false)
        }
        btFinish.setOnClickListener {
            requireActivity().finish()
        }
        btRegisterFioName.setOnClickListener {
            RegisterFioNameActivity.start(requireContext())
            requireActivity().finish()
        }
        tvFioDomain.text = "@$fioDomain"
        tvRegisterFioNameDesc.text = HtmlCompat.fromHtml(resources.getString(R.string.fio_create_name_desc, "@$fioDomain"),
                HtmlCompat.FROM_HTML_MODE_COMPACT)
        tvConnectedFioAccount.text = fioAccountLabel
        tvExpirationDate.text = transformExpirationDate(expirationDate)
    }

    companion object {
        @JvmStatic
        fun newInstance(fioDomain: String, fioAccountLabel: String, expirationDate: String): RegisterFioDomainCompletedFragment {
            val f = RegisterFioDomainCompletedFragment()
            val args = Bundle()

            args.putString("fioDomain", fioDomain)
            args.putString("fioAccountLabel", fioAccountLabel)
            args.putString("expirationDate", expirationDate)

            f.arguments = args
            return f
        }
    }
}