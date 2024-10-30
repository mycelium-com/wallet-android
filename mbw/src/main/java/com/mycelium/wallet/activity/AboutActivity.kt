package com.mycelium.wallet.activity

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.View
import android.widget.TextView
import androidx.annotation.RawRes
import androidx.appcompat.app.AppCompatActivity
import com.google.common.base.Joiner
import com.mycelium.wallet.activity.modern.Toaster
import com.google.common.io.ByteSource
import com.mycelium.wallet.*
import com.mycelium.wallet.activity.changelog.ChangeLog
import com.mycelium.wallet.databinding.AboutActivityBinding
import com.mycelium.wapi.api.WapiException
import com.mycelium.wapi.api.response.VersionInfoExResponse
import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.*

class AboutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = AboutActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val mbwManager = MbwManager.getInstance(this)
        val versionManager = mbwManager.versionManager
        binding.tvVersionNumber.text = BuildConfig.VERSION_NAME + if (BuildConfig.DEBUG) "\nDebug Build" else ""
        binding.tvVersionCode.text = String.format(Locale.US, "(%d)", BuildConfig.VERSION_CODE)
        setLicenseForButton(binding.btTouMycelium, R.raw.tou_mycelium)
        setLicenseForButton(binding.btLicenseMycelium, R.raw.license_mycelium)
        setLicenseForButton(binding.btLicenseZxing, R.raw.license_zxing)
        setLicenseForButton(binding.btLicensePdfwriter, R.raw.license_pdfwriter)
        setLicenseForButton(binding.btSpecialThanks, R.raw.special_thanks)
        binding.btShowChangelog.setOnClickListener { ChangeLog.show(supportFragmentManager) }
        binding.btCheckUpdate.setOnClickListener { v: View? ->
            val progress = ProgressDialog.show(this, getString(R.string.update_check),
                    getString(R.string.please_wait), true)
            versionManager.checkForUpdateSync { response: VersionInfoExResponse?, exception: WapiException? ->
                progress.dismiss()
                if (exception != null) {
                    Toaster(this).toast(R.string.version_check_failed, false)
                    mbwManager.reportIgnoredException(RuntimeException("WapiException: " + exception.errorCode))
                } else {
                    showVersionInfo(versionManager, response)
                }
            }
        }
        setLinkTo(binding.tvSourceUrl, R.string.source_url)
        setLinkTo(binding.tvHomepageUrl, R.string.homepage_url)
        setMailTo(binding.tvContactEmail)

        //set playstore link to qr code
        val packageName = applicationContext.packageName
        val playstoreUrl = Constants.PLAYSTORE_BASE_URL + packageName
        binding.ivPlaystoreQR.apply {
            qrCode = playstoreUrl
            tapToCycleBrightness = false
            setOnClickListener {
                startActivity(Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse(playstoreUrl)
                })
            }
        }
        // show direct apk link for the - very unlikely - case that google blocks our playstore entry
        binding.ivDirectApkQR.apply {
            qrCode = Constants.DIRECT_APK_URL
            tapToCycleBrightness = false
            setOnClickListener {
                startActivity(Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse(Constants.DIRECT_APK_URL)
                })
            }
        }
    }

    private fun showVersionInfo(versionManager: VersionManager, response: VersionInfoExResponse?) {
        if (response == null || versionManager.isSameVersion(response.versionNumber)) {
            AlertDialog.Builder(this).setMessage(getString(R.string.version_uptodate, BuildConfig.VERSION_NAME))
                    .setTitle(getString(R.string.update_check))
                    .setNeutralButton(R.string.ok) { dialog: DialogInterface, which: Int -> dialog.dismiss() }
                    .show()
        } else {
            versionManager.showVersionDialog(response, this)
        }
    }

    private fun setLinkTo(textView: TextView, res: Int) {
        val httpLink = Uri.parse(resources.getString(res))
        textView.text = Html.fromHtml(hrefLink(httpLink))
        textView.movementMethod = LinkMovementMethod.getInstance()
    }

    private fun setMailTo(textView: TextView) {
        val mailAddress = resources.getString(R.string.contact_email)
        textView.text = Html.fromHtml("<a href=\"mailto:$mailAddress\">$mailAddress</a>")
        textView.movementMethod = LinkMovementMethod.getInstance()
    }

    private fun hrefLink(githubLink: Uri) = """<a href="$githubLink">$githubLink</a>"""

    private fun setLicenseForButton(button: View, @RawRes fileId: Int) {
        button.setOnClickListener { v: View? ->
            val message: String
            message = try {
                Joiner.on("\n").join(
                        object : ByteSource() {
                            override fun openStream(): InputStream {
                                return resources.openRawResource(fileId)
                            }
                        }.asCharSource(StandardCharsets.UTF_8).readLines())
            } catch (e: IOException) {
                throw RuntimeException(e)
            }
            AlertDialog.Builder(this)
                    .setMessage(message).setCancelable(true)
                    .setPositiveButton("Okay") { dialog: DialogInterface, id: Int -> dialog.cancel() }
                    .show()
        }
    }
}