package com.mycelium.wallet.activity.fio.registerdomain

import android.os.AsyncTask
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.fio.registerdomain.viewmodel.RegisterFioDomainViewModel
import com.mycelium.wallet.activity.modern.Toaster
import com.mycelium.wallet.activity.util.toStringWithUnit
import com.mycelium.wallet.databinding.FragmentRenewFioDomainBinding
import com.mycelium.wapi.wallet.Util.getRenewTill
import com.mycelium.wapi.wallet.fio.FIODomain
import com.mycelium.wapi.wallet.fio.FioAccount
import com.mycelium.wapi.wallet.fio.FioModule
import fiofoundation.io.fiosdk.errors.FIOError
import java.text.SimpleDateFormat
import java.util.logging.Level
import java.util.logging.Logger

class RenewFioDomainFragment : Fragment() {
    private val viewModel: RegisterFioDomainViewModel by activityViewModels()
    private var binding: FragmentRenewFioDomainBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
            FragmentRenewFioDomainBinding.inflate(inflater, container, false)
                    .apply {
                        binding = this
                        viewModel = this@RenewFioDomainFragment.viewModel.apply {
                            val fioAccounts = listOf(this.fioAccountToRegisterName.value!!)
                            spinnerPayFromAccounts.adapter = ArrayAdapter<String>(requireContext(),
                                    R.layout.layout_fio_dropdown_medium_font, R.id.text,
                                    fioAccounts.map { "${it.label} ${it.accountBalance.spendable.toStringWithUnit()}" }).apply {
                                this.setDropDownViewResource(R.layout.layout_send_coin_transaction_replace_dropdown)
                            }
                            spinnerPayFromAccounts.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                                override fun onNothingSelected(p0: AdapterView<*>?) {}
                                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                                    viewModel!!.accountToPayFeeFrom.value = fioAccounts[p2]
                                }
                            }
                        }
                        lifecycleOwner = this@RenewFioDomainFragment
                    }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity() as AppCompatActivity).supportActionBar?.run {
            title = "Renew FIO Domain"
        }
        binding?.tvFioName?.text = "@${viewModel.domain.value}"
        binding?.tvNotEnoughFundsError?.visibility = View.GONE
        viewModel.accountToPayFeeFrom.observe(viewLifecycleOwner, Observer {
            val isNotEnoughFunds = it.accountBalance.spendable < viewModel.registrationFee.value!!
            binding?.tvNotEnoughFundsError?.visibility = if (isNotEnoughFunds) View.VISIBLE else View.GONE
            binding?.btNextButton?.isEnabled = !isNotEnoughFunds
            (binding?.spinnerPayFromAccounts?.getChildAt(0) as? TextView)?.setTextColor(
                    if (isNotEnoughFunds) resources.getColor(R.color.fio_red) else resources.getColor(R.color.white))
        })
        val fioModule = MbwManager.getInstance(requireContext()).getWalletManager(false).getModuleById(FioModule.ID) as FioModule
        val fioDomain: FIODomain = fioModule.getAllRegisteredFioDomains().first { it.domain == viewModel.domain.value }
        binding?.tvRenewTill?.text = SimpleDateFormat("LLLL dd, yyyy 'at' hh:mm a").format(getRenewTill(fioDomain.expireDate))
        binding?.btNextButton?.setOnClickListener {
            RenewDomainTask(viewModel.accountToPayFeeFrom.value!! as FioAccount, viewModel.domain.value!!, fioModule) { expiration ->
                if (expiration != null) {
                    Toaster(this).toast("FIO Domain has been renewed", true)
                } else {
                    Toaster(this).toast("Something went wrong", true)
                }
                activity?.finish()
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        }
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    class RenewDomainTask(
            val account: FioAccount,
            private val fioDomain: String,
            private val fioModule: FioModule,
            val listener: ((String?) -> Unit)) : AsyncTask<Void, Void, String?>() {
        override fun doInBackground(vararg args: Void): String? {
            return try {
                account.renewFIODomain(fioDomain)
            } catch (e: Exception) {
                Logger.getLogger(RenewDomainTask::class.simpleName).log(Level.WARNING, "failed to renew fio domain: ${e.localizedMessage}")
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
}