package com.mycelium.wallet.activity.fio.registername

import android.os.AsyncTask
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.fio.registername.viewmodel.RegisterFioNameViewModel
import com.mycelium.wallet.activity.modern.Toaster
import com.mycelium.wallet.activity.util.toStringWithUnit
import com.mycelium.wallet.databinding.FragmentRenewFioNameBinding
import com.mycelium.wapi.wallet.fio.FioAccount
import com.mycelium.wapi.wallet.fio.FioModule
import kotlinx.android.synthetic.main.fragment_renew_fio_name.*
import java.text.SimpleDateFormat
import java.util.*

class RenewFioNameFragment : Fragment() {
    private val viewModel: RegisterFioNameViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            DataBindingUtil.inflate<FragmentRenewFioNameBinding>(inflater, R.layout.fragment_renew_fio_name, container, false)
                    .apply {
                        viewModel = this@RenewFioNameFragment.viewModel.apply {
                            val fioAccounts = getFioAccounts(this.addressWithDomain.value!!)
                            spinnerPayFromAccounts.adapter = ArrayAdapter(context,
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
                        lifecycleOwner = this@RenewFioNameFragment
                    }.root

    private fun getFioAccounts(name: String): List<FioAccount> {
        val walletManager = MbwManager.getInstance(context).getWalletManager(false)
        val uuid = (walletManager.getModuleById(FioModule.ID) as FioModule).getFioAccountByFioName(name)
                ?: throw IllegalStateException("Illegal name $name (Not owned by any of user's accounts)")
        return listOf(walletManager.getAccount(uuid) as FioAccount)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity() as AppCompatActivity).supportActionBar?.run {
            title = "Renew FIO Name"
        }
        tvNotEnoughFundsError.visibility = View.GONE
        viewModel.accountToPayFeeFrom.observe(viewLifecycleOwner, Observer {
            val isNotEnoughFunds = it.accountBalance.spendable < viewModel.registrationFee.value!!
            tvNotEnoughFundsError.visibility = if (isNotEnoughFunds) View.VISIBLE else View.GONE
            btNextButton.isEnabled = !isNotEnoughFunds
            (spinnerPayFromAccounts.getChildAt(0) as? TextView)?.setTextColor(
                    if (isNotEnoughFunds) resources.getColor(R.color.fio_red) else resources.getColor(R.color.white))
        })
        tvRenewTill.text = getRenewTill()
        btNextButton.setOnClickListener {
            RenewAddressTask(viewModel.accountToPayFeeFrom.value!! as FioAccount, viewModel.addressWithDomain.value!!) { expiration ->
                if (expiration != null) {
                    Toaster(this).toast("FIO Name has been renewed", true)
                } else {
                    Toaster(this).toast("Something went wrong", true)
                }
                activity?.finish()
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        }
    }

    private fun getRenewTill(): CharSequence? {
        val walletManager = MbwManager.getInstance(context).getWalletManager(false)
        val fioName = (walletManager.getModuleById(FioModule.ID) as FioModule)
                .getFIONameInfo(viewModel.addressWithDomain.value ?: "")
        val date = Calendar.getInstance().apply {
            time = fioName.expireDate
            add(Calendar.DAY_OF_MONTH, 365)
        }.time
        return SimpleDateFormat("LLLL dd, yyyy 'at' hh:mm a").format(date)
    }

    class RenewAddressTask(
            val account: FioAccount,
            private val fioAddress: String,
            val listener: ((String?) -> Unit)) : AsyncTask<Void, Void, String?>() {
        override fun doInBackground(vararg args: Void): String? {
            return try {
                account.renewFIOAddress(fioAddress)
            } catch (e: Exception) {
                null
            }
        }

        override fun onPostExecute(result: String?) {
            listener(result)
        }
    }
}