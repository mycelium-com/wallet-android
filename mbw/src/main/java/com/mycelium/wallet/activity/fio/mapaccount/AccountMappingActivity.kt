package com.mycelium.wallet.activity.fio.mapaccount

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.NavHostFragment
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.fio.mapaccount.viewmodel.FIOMapPubAddressViewModel
import com.mycelium.wapi.wallet.fio.FioAccount
import com.mycelium.wapi.wallet.fio.FioModule
import java.util.*


class AccountMappingActivity : AppCompatActivity(R.layout.activity_account_mapping) {
    private lateinit var viewModel: FIOMapPubAddressViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.run {
            setHomeAsUpIndicator(R.drawable.ic_back_arrow)
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
        }
        (supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment?)
                ?.navController?.setGraph(R.navigation.fio_map_account, intent?.extras)
        viewModel = ViewModelProviders.of(this).get(FIOMapPubAddressViewModel::class.java)
        val walletManager = MbwManager.getInstance(this.application).getWalletManager(false)
        if (intent?.extras?.containsKey("accountId") == true) {
            val accountId = intent.getSerializableExtra("accountId") as UUID
            val account = walletManager.getAccount(accountId)
            if (account is FioAccount) {
                viewModel.accountList.value = listOf(account)
            } else {
                val fioModule = walletManager.getModuleById(FioModule.ID) as FioModule
                viewModel.accountList.value = fioModule.getFIONames(account!!)
                        .mapNotNull { fioModule.getFioAccountByFioName(it.name) }
                        .map { walletManager.getAccount(it) as FioAccount }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean =
            when (item?.itemId) {
                android.R.id.home -> {
                    onBackPressed()
                    true
                }
                else -> super.onOptionsItemSelected(item)
            }
}
