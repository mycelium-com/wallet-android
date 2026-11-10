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
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import com.mrd.bitlib.crypto.BipSss
import com.mrd.bitlib.crypto.InMemoryPrivateKey
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import com.mycelium.wallet.activity.export.adapter.SharesAdapter
import com.mycelium.wallet.activity.export.adapter.TextPrintAdapter
import com.mycelium.wallet.activity.modern.Toaster
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
    private val generatedShares: GeneratedSharesViewModel by viewModels()

    val shareLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            binding.progressOverlay.isVisible = false
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityShamirSharingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        secret = intent.getByteArrayExtra("data")!!

        binding.rvSharesContainer.adapter = sharesAdapter
        sharesAdapter.shareListener = {
            val sharedFile = File(cacheDir, SINGLE_SHARE_PDF)
            val shamirBuilder = ShamirBuilder().apply {
                shares = listOf(it)
            }
            sharedFile.writeText(shamirBuilder.build())
            shareFile(sharedFile)
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
            Utils.setSensitiveClipboardString(it.toString(), this)
        }
        binding.rvSharesContainer.addItemDecoration(
            VerticalSpaceItemDecoration(resources.getDimensionPixelOffset(R.dimen.size_x1))
        )
        binding.generate.setOnClickListener {
            it?.hideKeyboard()
            generateShares(secret)
        }
        // Rotation: show the very shares generated before, even if the fields were edited since.
        // Generating again would silently hand out a different, incompatible set.
        generatedShares.shares?.let {
            sharesAdapter.submitList(it)
            binding.pageHint.isVisible = false
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
            generatedShares.shares = null
            return
        }
        if (totalShares != null && threshold != null && totalShares > 0 && threshold in 1..totalShares) {
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
                    generatedShares.shares = listTiShow
                    binding.pageHint.isVisible = false
                }
            }
        } else {
            sharesAdapter.submitList(emptyList<BipSss.Share>())
            generatedShares.shares = null
            binding.pageHint.isVisible = true
            if (totalShares != null && threshold != null) {
                // 0/0, threshold > shares: BipSss.split would throw - tell the user instead of clearing silently
                Toaster(this).toast(R.string.shamir_threshold_invalid, false)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            deleteExportedPdfs(this)
        }
    }

    /**
     * Keeps the generated shares across configuration changes. Not in the saved instance state: the text form of a
     * share drops the total count and up to 65535 shares don't fit into a Bundle.
     */
    class GeneratedSharesViewModel : ViewModel() {
        var shares: List<BipSss.Share>? = null
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
                    val sharesToExport = sharesAdapter.currentList
                        .filterNotNull()
                        .filter { it != SharesAdapter.EMPTY }
                    when {
                        sharesToExport.isEmpty() -> {
                            // ShamirBuilder.build() calls shares.first(): nothing to export yet
                            Toaster(this@ShamirSharingActivity).toast(R.string.shamir_no_shares, false)
                        }
                        sharesToExport.size > MAX_PDF_SHARES -> {
                            // the PDF is built as one in-memory string (~20 MB per 1000 shares) -> OOM
                            AlertDialog.Builder(this@ShamirSharingActivity)
                                .setTitle(getString(R.string.not_valid_value))
                                .setMessage(getString(R.string.shamir_pdf_shares_limit, MAX_PDF_SHARES))
                                .setPositiveButton(R.string.button_ok, null)
                                .show()
                        }
                        else -> lifecycleScope.launch(Dispatchers.IO) {
                            withContext(Dispatchers.Main) {
                                binding.progressOverlay.isVisible = true
                            }
                            val sharedFile = File(cacheDir, ALL_SHARES_PDF)
                            val shamirBuilder = ShamirBuilder().apply { shares = sharesToExport }
                            try {
                                sharedFile.writeText(shamirBuilder.build())
                            } catch (e: Throwable) {
                                withContext(Dispatchers.Main) {
                                    binding.progressOverlay.isVisible = false
                                    Toaster(this@ShamirSharingActivity).toast(R.string.not_valid_value, false)
                                }
                                return@launch
                            }
                            withContext(Dispatchers.Main) {
                                // the overlay must go away, otherwise it keeps eating every touch on this screen
                                binding.progressOverlay.isVisible = false
                                shareFile(sharedFile)
                            }
                        }
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
        private const val ALL_SHARES_PDF = "shamir_shares.pdf"
        private const val SINGLE_SHARE_PDF = "shamir_share.pdf"
        /** Above this the single-string PDF builder runs out of memory (1000 shares already OOM on a 192 MB heap). */
        const val MAX_PDF_SHARES = 500

        /**
         * The exported PDFs hold the shares of a private key: don't leave them in the cache
         * (File.deleteOnExit never fires on Android - the process is not "exited").
         */
        @JvmStatic
        fun deleteExportedPdfs(context: Context) {
            File(context.cacheDir, ALL_SHARES_PDF).delete()
            File(context.cacheDir, SINGLE_SHARE_PDF).delete()
        }

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
