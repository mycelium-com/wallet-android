package com.mycelium.wallet.activity

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Html
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RawRes
import androidx.appcompat.app.AppCompatActivity
import com.google.common.base.Joiner
import com.mycelium.wallet.activity.modern.Toaster
import com.google.common.io.ByteSource
import com.mycelium.wallet.*
import com.mycelium.wallet.activity.changelog.ChangeLog
import com.mycelium.wapi.api.WapiException
import com.mycelium.wapi.api.response.VersionInfoExResponse
import com.mycelium.wapi.wallet.SyncMode
import com.mycelium.wapi.wallet.fio.FioModule
import kotlinx.android.synthetic.main.about_activity.*
import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.*

class AboutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.about_activity)
        val mbwManager = MbwManager.getInstance(this)
        val versionManager = mbwManager.versionManager
        tvVersionNumber.text = BuildConfig.VERSION_NAME + if (BuildConfig.DEBUG) "\nDebug Build" else ""
        tvVersionCode.text = String.format(Locale.US, "(%d)", BuildConfig.VERSION_CODE)
        setLicenseForButton(bt_tou_mycelium, R.raw.tou_mycelium)
        setLicenseForButton(bt_license_mycelium, R.raw.license_mycelium)
        setLicenseForButton(bt_license_zxing, R.raw.license_zxing)
        setLicenseForButton(bt_license_pdfwriter, R.raw.license_pdfwriter)
        setLicenseForButton(bt_special_thanks, R.raw.special_thanks)
        bt_show_changelog.setOnClickListener { ChangeLog.show(supportFragmentManager) }
        bt_check_update.setOnClickListener { v: View? ->
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
        bt_show_server_info.setOnClickListener { ConnectionLogsActivity.callMe(this) }
        if (BuildConfig.BUILD_TYPE === "debug") {
            bt_fio_server_error_logs.visibility = View.VISIBLE
            bt_fio_server_error_logs.setOnClickListener {
                val fioModule = mbwManager.getWalletManager(false).getModuleById(FioModule.ID) as FioModule?
                val logs: List<String?> = fioModule!!.getFioServerLogsListAndClear()
                if (logs.isEmpty()) {
                    Toast.makeText(this, getString(R.string.no_logs), Toast.LENGTH_SHORT).show()
                } else {
                    val joined = TextUtils.join("\n", logs)
                    Utils.setClipboardString(joined, this)
                    Toast.makeText(this, getString(R.string.copied_to_clipboard), Toast.LENGTH_SHORT).show()
                }
            }
        }
        setLinkTo(tvSourceUrl, R.string.source_url)
        setLinkTo(tvHomepageUrl, R.string.homepage_url)
        setMailTo(tvContactEmail)

        //set playstore link to qr code
        val packageName = applicationContext.packageName
        val playstoreUrl = Constants.PLAYSTORE_BASE_URL + packageName
        ivPlaystoreQR.apply {
            qrCode = playstoreUrl
            tapToCycleBrightness = false
            setOnClickListener {
                startActivity(Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse(playstoreUrl)
                })
            }
        }

        bt_boost_look_ahead.setOnClickListener {
            AlertDialog.Builder(this)
                    .setMessage(Html.fromHtml("""The most common cases of missing coins are easily
                        |resolved. Please read carefully before contacting support:<br>
                        |<br>
                        |<h4>No server connection</h4>
                        |When the wallet can't talk to the servers, it can't correctly display your
                        |balance and might even display 0BTC if you tried to reload the account.
                        |<b>Don't panic.</b> Check your internet connection or if our servers have
                        |issues, our engineers are probably already hard at work fixing the issue.
                        |In any case, as long as you don't see a transaction where your funds left
                        |your wallet, your funds should still be in your wallet, protected by your
                        |12 words backup that in the worst case works with most other Bitcoin
                        |wallets. Do not try deleting and reinstalling the app unless you are 100%
                        |sure you have all the backups.
                        |<h4>Restored wallet with "12 words + passphrase" using the PIN as
                        |  passphrase</h4>
                        |Adding
                        |the wrong passphrase doesn't result in an error but in restoring
                        |of a different, empty wallet. <b>Try restoring your wallet with only "12
                        |words".</b>
                        |<h4>Coins are in "Account 2"</h4>
                        |If you restored from a backup of a wallet that had more than just "Account
                        |1", you have to manually recreate missing accounts to re-discover them. To
                        |do so, just <b>go to the
                        |"Accounts" tab and create a "new" account</b>.
                        |<h4> Coins are beyond the standard "look-ahead" or "gap limit":</h4>
                        |If
                        |you used third party tools to receive coins into your wallet such as
                        |BTC-Pay Server, the wallet checking only 20 future addresses beyond
                        |your last unused address might not find those transactions. <b>Press
                        |"Boost Gap Limit"</b> below to check if this is the case. This will check
                        |for 200 addresses on all active accounts once.
                    """.trimMargin()))
                    .setPositiveButton("Boost Gap Limit") { _, _ ->
                        mbwManager.getWalletManager(false).startSynchronization(SyncMode.BOOSTED)
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
        }

        // show direct apk link for the - very unlikely - case that google blocks our playstore entry
        ivDirectApkQR.apply {
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