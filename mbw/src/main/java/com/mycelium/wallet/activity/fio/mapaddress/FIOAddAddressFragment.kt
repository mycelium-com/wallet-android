package com.mycelium.wallet.activity.fio.mapaddress

import android.os.AsyncTask
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.modern.Toaster
import com.mycelium.wallet.databinding.FragmentFioAddAddressBinding
import com.mycelium.wapi.wallet.fio.FioAccount
import kotlinx.android.synthetic.main.fragment_fio_add_address.*


class FIOAddAddressFragment: Fragment() {
    private val viewModel: FIORegisterAddressViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            DataBindingUtil.inflate<FragmentFioAddAddressBinding>(inflater, R.layout.fragment_fio_add_address, container, false)
                    .apply {
                        viewModel = this@FIOAddAddressFragment.viewModel
                        lifecycleOwner = this@FIOAddAddressFragment
                    }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        registerAddress.setOnClickListener {
            RegisterAddressTask(viewModel.account.value!!, viewModel.addressWithDomain.value!!) { expiration ->
                if (expiration != null) {
                    viewModel.expirationDate.value = expiration
                    findNavController().navigate(R.id.actionNext)
                } else {
                    Toaster(this).toast("Something went wrong", true)
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        }
    }

    class RegisterAddressTask(
            val account: FioAccount,
            val fioAddress: String,
            val listener: ((String?) -> Unit)) : AsyncTask<Void, Void, String?>() {
        override fun doInBackground(vararg args: Void): String? {
            return try {
                account.registerFIOAddress(fioAddress)
            } catch (e: Exception) {
                null
            }
        }

        override fun onPostExecute(result: String?) {
            listener(result)
        }
    }
}