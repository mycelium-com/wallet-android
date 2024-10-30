package com.mycelium.wallet.activity.fio.registername

import android.content.Intent
import android.os.AsyncTask
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
import com.mycelium.wallet.activity.fio.mapaccount.AccountMappingActivity
import com.mycelium.wallet.databinding.FragmentRegisterFioNameCompletedBinding
import com.mycelium.wapi.wallet.Util.convertToDate
import com.mycelium.wapi.wallet.Util.transformExpirationDate
import com.mycelium.wapi.wallet.fio.FioBlockchainService
import com.mycelium.wapi.wallet.fio.FioEndpoints
import com.mycelium.wapi.wallet.fio.FioModule
import com.mycelium.wapi.wallet.fio.RegisteredFIOName
import fiofoundation.io.fiosdk.errors.FIOError
import java.util.logging.Level
import java.util.logging.Logger

class RegisterFioNameCompletedFragment : Fragment() {

    private var binding: FragmentRegisterFioNameCompletedBinding? = null

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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        FragmentRegisterFioNameCompletedBinding.inflate(inflater, container, false).apply {
            binding = this
        }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity() as AppCompatActivity).supportActionBar!!.apply {
            title = getString(R.string.fio_registration_complete)
            setDisplayHomeAsUpEnabled(false)
        }
        binding?.btFinish?.setOnClickListener {
            requireActivity().finish()
        }
        binding?.btConnectAccounts?.setOnClickListener {
            val fioModule = MbwManager.getInstance(context).getWalletManager(false).getModuleById(FioModule.ID) as FioModule
            GetBundledTxsNumberTask(MbwManager.getInstance(requireContext()).fioEndpoints, fioName, fioModule) { bundledTxsNum ->
                startActivity(Intent(context, AccountMappingActivity::class.java)
                        .putExtra("fioName", RegisteredFIOName(fioName, convertToDate(expirationDate), bundledTxsNum)))
                activity?.finish()
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        }
        binding?.tvFioName?.text = fioName
        binding?.tvConnectAccountsDesc?.text = HtmlCompat.fromHtml(resources.getString(R.string.fio_connect_accounts_desc, fioName),
                HtmlCompat.FROM_HTML_MODE_COMPACT)
        binding?.tvConnectedFioAccount?.text = fioAccountLabel
        binding?.tvExpirationDate?.text = transformExpirationDate(expirationDate)
    }

    companion object {
        const val DEFAULT_BUNDLED_TXS_NUM = 100

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
            private val fioEndpoints: FioEndpoints,
            val fioName: String,
            private val fioModule: FioModule,
            val listener: ((Int) -> Unit)) : AsyncTask<Void, Void, Int>() {
        override fun doInBackground(vararg args: Void): Int {
            return try {
                FioBlockchainService.getBundledTxsNum(fioEndpoints, fioName) ?: DEFAULT_BUNDLED_TXS_NUM
            } catch (e: Exception) {
                if (e is FIOError) {
                    fioModule.addFioServerLog(e.toJson())
                }
                Logger.getLogger(GetBundledTxsNumberTask::class.simpleName).log(Level.WARNING, "failed to get bundled txs num: ${e.localizedMessage}")
                DEFAULT_BUNDLED_TXS_NUM
            }
        }

        override fun onPostExecute(result: Int) {
            listener(result)
        }
    }
}