package com.mycelium.wallet.activity.fio.registerdomain

import android.annotation.SuppressLint
import android.os.AsyncTask
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.activity.addCallback
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.fio.registerdomain.viewmodel.RegisterFioDomainViewModel
import com.mycelium.wallet.activity.modern.Toaster
import com.mycelium.wallet.activity.util.toStringWithUnit
import com.mycelium.wallet.activity.view.loader
import com.mycelium.wallet.databinding.FragmentRegisterFioDomainStep2BindingImpl
import com.mycelium.wapi.wallet.fio.FioAccount
import com.mycelium.wapi.wallet.fio.FioModule
import com.mycelium.wapi.wallet.fio.getActiveSpendableFioAccounts
import fiofoundation.io.fiosdk.errors.FIOError
import kotlinx.android.synthetic.main.fragment_register_fio_domain_step2.*
import java.util.logging.Level
import java.util.logging.Logger

class RegisterFioDomainStep2Fragment : Fragment() {
    private val viewModel: RegisterFioDomainViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // without this the navigation through back button would finish the activity
        // but the desired behavior here is to return back to step 1
        requireActivity().onBackPressedDispatcher.addCallback(this) {
            findNavController().navigate(R.id.actionNext)
        }.apply { this.isEnabled = true }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            DataBindingUtil.inflate<FragmentRegisterFioDomainStep2BindingImpl>(inflater, R.layout.fragment_register_fio_domain_step2, container, false)
                    .apply {
                        viewModel = this@RegisterFioDomainStep2Fragment.viewModel.apply {
                            val walletManager = MbwManager.getInstance(context).getWalletManager(false)
                            val fioAccounts = walletManager.getActiveSpendableFioAccounts()
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
                        lifecycleOwner = this@RegisterFioDomainStep2Fragment
                    }.root

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        btNextButton.setOnClickListener {
            val fioModule = MbwManager.getInstance(context).getWalletManager(false).getModuleById(FioModule.ID) as FioModule
            RegisterDomainTask(viewModel.fioAccountToRegisterName.value!!, viewModel.domain.value!!, fioModule) { expiration ->
                loader(false)
                if (expiration != null) {
                    requireActivity().supportFragmentManager
                            .beginTransaction()
                            .replace(R.id.container,
                                    RegisterFioDomainCompletedFragment.newInstance(viewModel.domain.value!!,
                                            viewModel.fioAccountToRegisterName.value!!.label,
                                            viewModel.fioAccountToRegisterName.value!!.id, expiration))
                            .commit()
                } else {
                    Toaster(this).toast("Something went wrong", true)
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
            loader(true)
        }
        icEdit.setOnClickListener {
            findNavController().popBackStack()
        }
        val mbwManager = MbwManager.getInstance(context)
        viewModel.registrationFee.observe(viewLifecycleOwner, Observer {
            tvFeeInfo.text = resources.getString(R.string.fio_annual_fee_domain, it.toStringWithUnit())
            tvAnnualFeeFiat.text = "~ ${mbwManager.exchangeRateManager.get(viewModel.registrationFee.value!!,
                    mbwManager.getFiatCurrency(viewModel.registrationFee.value!!.type)).toStringWithUnit()}"
        })
        tvFioName.text = "@${viewModel.domain.value}"
        tvNotEnoughFundsError.visibility = View.GONE
        viewModel.accountToPayFeeFrom.observe(viewLifecycleOwner, Observer {
            val isNotEnoughFunds = it.accountBalance.spendable < viewModel.registrationFee.value!!
            tvNotEnoughFundsError.visibility = if (isNotEnoughFunds) View.VISIBLE else View.GONE
            btNextButton.isEnabled = !isNotEnoughFunds
            (spinnerPayFromAccounts.getChildAt(0) as? TextView)?.setTextColor(
                    if (isNotEnoughFunds) resources.getColor(R.color.fio_red) else resources.getColor(R.color.white))
        })
    }
}

class RegisterDomainTask(
        val account: FioAccount,
        private val fioDomain: String,
        private val fioModule: FioModule,
        val listener: ((String?) -> Unit)) : AsyncTask<Void, Void, String?>() {
    override fun doInBackground(vararg args: Void): String? {
        return try {
            account.registerFIODomain(fioDomain)
        } catch (e: Exception) {
            Logger.getLogger(RegisterDomainTask::class.simpleName).log(Level.WARNING, "failed to register fio domain: ${e.localizedMessage}")
            if (e is FIOError) {
                fioModule.addFioServerLog(e.toJson())
            }
            null
        }
    }

    override fun onPostExecute(result: String?) {
        listener(result)
    }
}