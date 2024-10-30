package com.mycelium.wallet.activity.fio.registername

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import com.mycelium.wallet.activity.fio.registername.viewmodel.RegisterFioNameViewModel
import com.mycelium.wallet.activity.util.toStringWithUnit
import com.mycelium.wallet.activity.view.loader
import com.mycelium.wallet.databinding.FragmentRegisterFioNameStep2Binding
import com.mycelium.wapi.wallet.fio.*


class RegisterFioNameStep2Fragment : Fragment() {
    private val viewModel: RegisterFioNameViewModel by activityViewModels()
    var binding: FragmentRegisterFioNameStep2Binding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // without this the navigation through back button would finish the activity
        // but the desired behavior here is to return back to step 1
        requireActivity().onBackPressedDispatcher.addCallback(this) {
            findNavController().navigate(R.id.actionNext)
        }.apply { this.isEnabled = true }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            FragmentRegisterFioNameStep2Binding.inflate(inflater, container, false)
                    .apply {
                        binding = this
                        viewModel = this@RegisterFioNameStep2Fragment.viewModel.apply {
                            val fioAccounts = getFioAccountsToRegisterTo(this.domain.value!!)
                            spinnerFioAccounts?.adapter = ArrayAdapter<String>(requireContext(),
                                    R.layout.layout_fio_dropdown_medium_font, R.id.text, fioAccounts.map { it.label }).apply {
                                this.setDropDownViewResource(R.layout.layout_send_coin_transaction_replace_dropdown)
                            }
                            spinnerFioAccounts.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                                override fun onNothingSelected(p0: AdapterView<*>?) {}
                                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                                    viewModel!!.fioAccountToRegisterName.value = fioAccounts[p2]
                                    // temporary account to register on and to pay fee from are the same
                                    // until the ability of paying with other currencies is implemented
                                    // TODO remove next line when it's ready
                                    spinnerPayFromAccounts.setSelection((spinnerPayFromAccounts.adapter as ArrayAdapter<String>).getPosition(
                                            "${fioAccounts[p2].label} ${fioAccounts[p2].accountBalance.spendable.toStringWithUnit()}"))
                                }
                            }
                            spinnerPayFromAccounts?.adapter = ArrayAdapter<String>(requireContext(),
                                    R.layout.layout_fio_dropdown_medium_font, R.id.text,
                                    fioAccounts.map { "${it.label} ${it.accountBalance.spendable.toStringWithUnit()}" }).apply {
                                this.setDropDownViewResource(R.layout.layout_send_coin_transaction_replace_dropdown)
                            }
                            spinnerPayFromAccounts.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                                override fun onNothingSelected(p0: AdapterView<*>?) {}
                                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                                    viewModel!!.accountToPayFeeFrom.value = fioAccounts[p2]
                                    // temporary account to register on and to pay fee from are the same
                                    // until the ability of paying with other currencies is implemented
                                    // TODO remove next line when it's ready
                                    spinnerFioAccounts.setSelection((spinnerFioAccounts.adapter as ArrayAdapter<String>).getPosition(
                                            fioAccounts[p2].label))
                                }
                            }

                            // preselect account which context menu was used
                            val fioAccount = this.fioAccountToRegisterName.value
                            if (fioAccount != null) {
                                spinnerFioAccounts.setSelection((spinnerFioAccounts.adapter as ArrayAdapter<String>).getPosition(
                                        fioAccount.label))
                                // temporary account to register on and to pay fee from are the same
                                // until the ability of paying with other currencies is implemented
                                // TODO remove next line when it's ready
                                spinnerPayFromAccounts.setSelection((spinnerPayFromAccounts.adapter as ArrayAdapter<String>).getPosition(
                                        "${fioAccount.label} ${fioAccount.accountBalance.spendable.toStringWithUnit()}"))
                            }
                        }
                        lifecycleOwner = this@RegisterFioNameStep2Fragment
                    }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.tvNotEnoughFundsError?.visibility = View.GONE
        binding?.btNextButton?.setOnClickListener {
            loader(true)
            val fioModule = MbwManager.getInstance(context).getWalletManager(false).getModuleById(FioModule.ID) as FioModule
            viewModel.registerName(fioModule, { expiration ->
                loader(false)
                requireActivity().supportFragmentManager
                        .beginTransaction()
                        .replace(R.id.container,
                                RegisterFioNameCompletedFragment.newInstance(viewModel.addressWithDomain.value!!,
                                        viewModel.fioAccountToRegisterName.value!!.label, expiration))
                        .addToBackStack(null)
                        .commit()
            }, { errorMessage ->
                loader(false)
                Utils.showSimpleMessageDialog(activity, getString(R.string.fio_register_address_failed, errorMessage))
            })
        }
        viewModel.registrationFee.observe(viewLifecycleOwner, Observer {
            binding?.tvFeeInfo?.text = resources.getString(R.string.fio_annual_fee, it.toStringWithUnit())
        })
        viewModel.accountToPayFeeFrom.observe(viewLifecycleOwner, Observer {
            val isNotEnoughFunds = it.accountBalance.spendable < viewModel.registrationFee.value!!
            binding?.tvNotEnoughFundsError?.visibility = if (isNotEnoughFunds) View.VISIBLE else View.GONE
            binding?.btNextButton?.isEnabled = !isNotEnoughFunds
            (binding?.spinnerPayFromAccounts?.getChildAt(0) as? TextView)?.setTextColor(
                    if (isNotEnoughFunds) resources.getColor(R.color.fio_red) else resources.getColor(R.color.white))
        })
        binding?.icEdit?.setOnClickListener {
            findNavController().navigate(R.id.actionNext)
        }
    }

    private fun getFioAccountsToRegisterTo(fioDomain: FIODomain): List<FioAccount> {
        val walletManager = MbwManager.getInstance(context).getWalletManager(false)
        return if (fioDomain.isPublic) {
            walletManager.getActiveSpendableFioAccounts()
        } else {
            val uuid = (walletManager.getModuleById(FioModule.ID) as FioModule).getFioAccountByFioDomain(fioDomain.domain)
                    ?: throw IllegalStateException("Illegal domain ${fioDomain.domain} (Not owned by any of user's accounts)")
            listOf(walletManager.getAccount(uuid) as FioAccount)
        }
    }
}