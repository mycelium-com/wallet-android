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
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.fio.registername.RegisterFioNameActivity
import com.mycelium.wallet.databinding.FragmentRegisterFioDomainCompletedBinding
import com.mycelium.wapi.wallet.Util.transformExpirationDate
import com.mycelium.wapi.wallet.fio.FioModule
import java.util.*

class RegisterFioDomainCompletedFragment : Fragment() {

    var binding: FragmentRegisterFioDomainCompletedBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // without this the navigation through back button would return to previous fragment (name registration and the payment)
        // but the desired behavior here is to finish the activity as we have completed the registration
        requireActivity().onBackPressedDispatcher.addCallback(this) {
            requireActivity().finish()
        }.apply { this.isEnabled = true }
    }

    private val domain: String by lazy {
        requireArguments().getSerializable("fioDomain") as String
    }
    private val accountId: UUID by lazy {
        requireArguments().getSerializable("fioAccountId") as UUID
    }
    private val fioAccountLabel: String by lazy {
        requireArguments().getSerializable("fioAccountLabel") as String
    }
    private val expirationDate: String by lazy {
        requireArguments().getSerializable("expirationDate") as String
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
            FragmentRegisterFioDomainCompletedBinding.inflate(inflater, container, false).apply {
                binding = this
            }.root

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity() as AppCompatActivity).supportActionBar!!.apply {
            title = getString(R.string.fio_registration_complete)
            setDisplayHomeAsUpEnabled(false)
        }
        binding?.btFinish?.setOnClickListener {
            requireActivity().finish()
        }
        binding?.btRegisterFioName?.setOnClickListener {
            val walletManager = MbwManager.getInstance(requireContext()).getWalletManager(false)
            val fioModule = walletManager.getModuleById(FioModule.ID) as FioModule
            RegisterFioNameActivity.start(requireContext(), accountId, fioModule.getFIODomainInfo(domain))
            requireActivity().finish()
        }
        binding?.tvFioDomain?.text = "@$domain"
        binding?.tvRegisterFioNameDesc?.text = HtmlCompat.fromHtml(resources.getString(R.string.fio_create_name_desc, "@$domain"),
                HtmlCompat.FROM_HTML_MODE_COMPACT)
        binding?.tvConnectedFioAccount?.text = fioAccountLabel
        binding?.tvExpirationDate?.text = transformExpirationDate(expirationDate)
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    companion object {
        @JvmStatic
        fun newInstance(fioDomain: String, fioAccountLabel: String,
                        accountId: UUID, expirationDate: String): RegisterFioDomainCompletedFragment {
            val f = RegisterFioDomainCompletedFragment()
            val args = Bundle()

            args.putString("fioDomain", fioDomain)
            args.putSerializable("fioAccountId", accountId)
            args.putString("fioAccountLabel", fioAccountLabel)
            args.putString("expirationDate", expirationDate)

            f.arguments = args
            return f
        }
    }
}