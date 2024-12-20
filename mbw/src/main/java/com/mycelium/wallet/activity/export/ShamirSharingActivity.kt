package com.mycelium.wallet.activity.export

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import androidx.core.widget.doAfterTextChanged
import com.mrd.bitlib.crypto.BipSss
import com.mrd.bitlib.crypto.InMemoryPrivateKey
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import com.mycelium.wallet.activity.export.adapter.SharesAdapter
import com.mycelium.wallet.activity.view.VerticalSpaceItemDecoration
import com.mycelium.wallet.databinding.ActivityShamirSharingBinding


class ShamirSharingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityShamirSharingBinding

    private val sharesAdapter = SharesAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityShamirSharingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        val secret = intent.getByteArrayExtra("data")!!

        binding.rvSharesContainer.adapter = sharesAdapter
        binding.rvSharesContainer.addItemDecoration(
            VerticalSpaceItemDecoration(
                resources.getDimensionPixelOffset(R.dimen.fio_list_item_space)
            )
        )
        binding.etNumberOfShares.doAfterTextChanged {
            generateShares(secret)
        }
        binding.etThreshold.doAfterTextChanged {
            generateShares(secret)
        }
        Utils.preventScreenshots(this)
        addMenuProvider(MenuImpl())
    }

    private fun generateShares(secret: ByteArray) {
        sharesAdapter.submitList(emptyList<BipSss.Share>())
        val totalShares = binding.etNumberOfShares.text.toString().toIntOrNull()
        val threshold = binding.etThreshold.text.toString().toIntOrNull()

        if (totalShares != null && threshold != null && threshold <= totalShares) {
            val shares = BipSss.split(secret, totalShares, threshold)

            sharesAdapter.submitList(shares + null)
        } else {
            Toast.makeText(this, "Invalid input. Check your values.", Toast.LENGTH_SHORT).show()
        }
    }

    internal inner class MenuImpl : MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean =
            when (menuItem.itemId) {
                android.R.id.home -> {
                    onBackPressed()
                    true
                }

                else -> false
            }
    }

    companion object {
        fun getData(context: Context, privateKey: InMemoryPrivateKey): ByteArray {
            val network = MbwManager.getInstance(context).network
            return privateKey.getPrivateKeyBytes(network)
        }

        @JvmStatic
        fun callMe(current: Activity, privateKey: InMemoryPrivateKey) =
            current.startActivity(
                Intent(current, ShamirSharingActivity::class.java)
                    .putExtra("data", getData(current, privateKey))
            )
    }
}
