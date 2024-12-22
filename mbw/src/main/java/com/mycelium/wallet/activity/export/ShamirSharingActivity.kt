package com.mycelium.wallet.activity.export

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.MenuProvider
import androidx.core.widget.doAfterTextChanged
import com.mrd.bitlib.crypto.BipSss
import com.mrd.bitlib.crypto.InMemoryPrivateKey
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import com.mycelium.wallet.activity.export.adapter.SharesAdapter
import com.mycelium.wallet.activity.util.fileProviderAuthority
import com.mycelium.wallet.activity.view.VerticalSpaceItemDecoration
import com.mycelium.wallet.databinding.ActivityShamirSharingBinding
import com.mycelium.wallet.pdf.ShamirBuilder
import java.io.File


class ShamirSharingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityShamirSharingBinding

    private val sharesAdapter = SharesAdapter()

    val shareLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
            }
        }

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
        val totalShares = binding.etNumberOfShares.text.toString().toIntOrNull()
        val threshold = binding.etThreshold.text.toString().toIntOrNull()

        if ((totalShares ?: 0) > 65535) {
            AlertDialog.Builder(this)
                .setTitle("Not valid value")
                .setMessage("Total shares must be less than 65535")
                .setPositiveButton(R.string.button_ok,  null)
                .show()
            sharesAdapter.submitList(emptyList<BipSss.Share>())
            return
        }
        if (totalShares != null && threshold != null && threshold <= totalShares) {
            val shares = BipSss.split(secret, totalShares, threshold)
            sharesAdapter.submitList(shares.sortedBy { it.shareNumber } + SharesAdapter.EMPTY)
        } else {
            sharesAdapter.submitList(emptyList<BipSss.Share>())
        }
    }

    internal inner class MenuImpl : MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.shamir, menu)
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean =
            when (menuItem.itemId) {
                android.R.id.home -> {
                    onBackPressed()
                    true
                }

                R.id.miExportPdf -> {
                    val sharedFile = File(cacheDir, "shamir_shares.pdf")
                    val shamirBuilder = ShamirBuilder().apply {
                        shares = sharesAdapter.currentList
                            .filterNotNull()
                            .filter { it != SharesAdapter.EMPTY }
                    }
                    sharedFile.writeText(shamirBuilder.build())
                    shareFile(sharedFile)
                    sharedFile.deleteOnExit()
                    true
                }

                else -> false
            }
    }

    private fun shareFile(file: File) {
        val fileUri = FileProvider.getUriForFile(this, fileProviderAuthority(), file)

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf" // Adjust MIME type as per the file
            putExtra(Intent.EXTRA_STREAM, fileUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        shareLauncher.launch(Intent.createChooser(shareIntent, "Share File"))
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
