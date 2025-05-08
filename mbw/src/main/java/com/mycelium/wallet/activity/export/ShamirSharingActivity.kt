package com.mycelium.wallet.activity.export

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.print.PrintManager
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import android.widget.TextView.OnEditorActionListener
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.mrd.bitlib.crypto.BipSss
import com.mrd.bitlib.crypto.InMemoryPrivateKey
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import com.mycelium.wallet.activity.export.adapter.SharesAdapter
import com.mycelium.wallet.activity.export.adapter.TextPrintAdapter
import com.mycelium.wallet.activity.util.fileProviderAuthority
import com.mycelium.wallet.activity.view.VerticalSpaceItemDecoration
import com.mycelium.wallet.activity.view.hideKeyboard
import com.mycelium.wallet.databinding.ActivityShamirSharingBinding
import com.mycelium.wallet.pdf.ShamirBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File


class ShamirSharingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityShamirSharingBinding

    private val sharesAdapter = SharesAdapter()
    lateinit var secret: ByteArray

    val shareLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityShamirSharingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        secret = intent.getByteArrayExtra("data")!!

        binding.rvSharesContainer.adapter = sharesAdapter
        sharesAdapter.shareListener = {
            val sharedFile = File(cacheDir, "shamir_share.pdf")
            val shamirBuilder = ShamirBuilder().apply {
                shares = listOf(it)
            }
            sharedFile.writeText(shamirBuilder.build())
            shareFile(sharedFile)
            sharedFile.deleteOnExit()
        }
        sharesAdapter.printListener = {
            lifecycleScope.launch(Dispatchers.IO) {
                val shamirBuilder = ShamirBuilder().apply {
                    shares = listOf(it)
                }
                val shamirString = shamirBuilder.build()
                withContext(Dispatchers.Main) {
                    val printManager = getSystemService(PRINT_SERVICE) as PrintManager
                    val jobName = "${getString(R.string.app_name)} Document"
                    printManager.print(jobName, TextPrintAdapter(this@ShamirSharingActivity, shamirString), null)
                }
            }
        }
        sharesAdapter.itemListener = {
            Utils.setClipboardString(it.toString(), this)
        }
        binding.rvSharesContainer.addItemDecoration(
            VerticalSpaceItemDecoration(resources.getDimensionPixelOffset(R.dimen.size_x1))
        )
        binding.generate.setOnClickListener {
            it?.hideKeyboard()
            generateShares(secret)
        }
        val editorListener = object : OnEditorActionListener {
            override fun onEditorAction(p0: TextView?, actionId: Int, p2: KeyEvent?): Boolean =
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    p0?.hideKeyboard()
                    true
                } else {
                    false
                }
        }
        binding.etThreshold.setOnEditorActionListener(editorListener)
        binding.etNumberOfShares.setOnEditorActionListener(editorListener)
        Utils.preventScreenshots(this)
        addMenuProvider(MenuImpl())
    }

    private fun generateShares(secret: ByteArray) {
        val totalShares = binding.etNumberOfShares.text.toString().toIntOrNull()
        val threshold = binding.etThreshold.text.toString().toIntOrNull()

        if (totalShares != null && totalShares > 65535) {
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.not_valid_value))
                .setMessage(getString(R.string.total_shares_limit))
                .setPositiveButton(R.string.button_ok, null)
                .show()
            sharesAdapter.submitList(emptyList<BipSss.Share>())
            return
        }
        if (totalShares != null && threshold != null && threshold <= totalShares) {
            lifecycleScope.launch(Dispatchers.Unconfined) {
                withContext(Dispatchers.Main) {
                    if (totalShares > 10000) {
                        binding.progressText.isVisible = true
                        binding.progressText.text = getString(R.string.total_shares_count_too_high)
                    }
                    binding.progressOverlay.isVisible = true
                }
                val shares = BipSss.split(secret, totalShares, threshold)
                val listTiShow = shares.sortedBy { it.shareNumber } + SharesAdapter.EMPTY
                withContext(Dispatchers.Main) {
                    binding.progressOverlay.isVisible = false
                    binding.progressText.isVisible = false
                    sharesAdapter.submitList(listTiShow)
                    binding.pageHint.isVisible = false
                }
            }
        } else {
            sharesAdapter.submitList(emptyList<BipSss.Share>())
            binding.pageHint.isVisible = true
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
                    lifecycleScope.launch(Dispatchers.IO) {
                        withContext(Dispatchers.Main) {
                            val totalShares = binding.etNumberOfShares.text.toString().toIntOrNull()
                            if (totalShares != null && totalShares > 1000) {
                                binding.progressText.isVisible = true
                                binding.progressText.text =
                                    getString(R.string.total_shares_count_too_high) + getString(R.string.every_1000_shares_20mb)
                            }

                            binding.progressOverlay.isVisible = true
                        }
                        val sharedFile = File(cacheDir, "shamir_shares.pdf")
                        val shamirBuilder = ShamirBuilder().apply {
                            shares = sharesAdapter.currentList
                                .filterNotNull()
                                .filter { it != SharesAdapter.EMPTY }
                        }
                        sharedFile.writeText(shamirBuilder.build())
                        withContext(Dispatchers.Main) {
                            binding.progressOverlay.isVisible = true
                            shareFile(sharedFile)
                        }
                        sharedFile.deleteOnExit()
                    }
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
        fun callMe(current: Context, privateKey: InMemoryPrivateKey) =
            current.startActivity(
                Intent(current, ShamirSharingActivity::class.java)
                    .putExtra("data", getData(current, privateKey))
            )
    }
}
