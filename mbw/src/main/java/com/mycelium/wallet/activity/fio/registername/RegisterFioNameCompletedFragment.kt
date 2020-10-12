package com.mycelium.wallet.activity.fio.registername

import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import com.mycelium.wallet.activity.fio.mapaccount.AccountMappingActivity
import com.mycelium.wapi.wallet.Util.convertToDate
import com.mycelium.wapi.wallet.Util.transformExpirationDate
import com.mycelium.wapi.wallet.fio.FioTransactionHistoryService
import com.mycelium.wapi.wallet.fio.RegisteredFIOName
import kotlinx.android.synthetic.main.fragment_register_fio_name_completed.*

class RegisterFioNameCompletedFragment : Fragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // without this the navigation through back button would return to previous fragment (name registration and the payment)
        // but the desired behavior here is to finish the activity as we have completed the registration
        requireActivity().onBackPressedDispatcher.addCallback(this) {
            requireActivity().finish()
        }.apply { this.isEnabled = true }
    }

    private val fioName: String by lazy {
        requireArguments().getSerializable("fioName") as String
    }
    private val fioAccountLabel: String by lazy {
        requireArguments().getSerializable("fioAccountLabel") as String
    }
    private val expirationDate: String by lazy {
        requireArguments().getSerializable("expirationDate") as String
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_register_fio_name_completed, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity() as AppCompatActivity).supportActionBar!!.apply {
            title = "Registration complete!"
            setDisplayHomeAsUpEnabled(false)
        }
        btFinish.setOnClickListener {
            requireActivity().finish()
        }
        btConnectAccounts.setOnClickListener {
            GetBundledTxsNumberTask(fioName) { bundledTxsNum ->
                startActivity(Intent(context, AccountMappingActivity::class.java)
                        .putExtra("fioName", RegisteredFIOName(fioName, convertToDate(expirationDate), bundledTxsNum)))
                activity?.finish()
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        }
        tvFioName.text = fioName
        tvConnectAccountsDesc.text = HtmlCompat.fromHtml(resources.getString(R.string.fio_connect_accounts_desc, fioName),
                HtmlCompat.FROM_HTML_MODE_COMPACT)
        tvConnectedFioAccount.text = fioAccountLabel
        tvExpirationDate.text = transformExpirationDate(expirationDate)
    }

    companion object {
        const val DEFAULT_BUNDLED_TXS_NUM = 105

        @JvmStatic
        fun newInstance(fioName: String, fioAccountLabel: String, expirationDate: String): RegisterFioNameCompletedFragment {
            val f = RegisterFioNameCompletedFragment()
            val args = Bundle()

            args.putString("fioName", fioName)
            args.putString("fioAccountLabel", fioAccountLabel)
            args.putString("expirationDate", expirationDate)

            f.arguments = args
            return f
        }
    }

    class GetBundledTxsNumberTask(
            val fioName: String,
            val listener: ((Int) -> Unit)) : AsyncTask<Void, Void, Int>() {
        override fun doInBackground(vararg args: Void): Int {
            return try {
                FioTransactionHistoryService.getBundledTxsNum(Utils.getFIOCoinType(), fioName) ?: DEFAULT_BUNDLED_TXS_NUM
            } catch (e: Exception) {
                Log.i("asdaf", "asdaf failed to get bundled txs num: ${e.localizedMessage}")
                DEFAULT_BUNDLED_TXS_NUM
            }
        }

        override fun onPostExecute(result: Int) {
            listener(result)
        }
    }
}