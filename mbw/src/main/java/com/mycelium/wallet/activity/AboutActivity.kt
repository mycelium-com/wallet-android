/*
 * Copyright 2013, 2014 Megion Research and Development GmbH
 *
 * Licensed under the Microsoft Reference Source License (MS-RSL)
 *
 * This license governs use of the accompanying software. If you use the software, you accept this license.
 * If you do not accept the license, do not use the software.
 *
 * 1. Definitions
 * The terms "reproduce," "reproduction," and "distribution" have the same meaning here as under U.S. copyright law.
 * "You" means the licensee of the software.
 * "Your company" means the company you worked for when you downloaded the software.
 * "Reference use" means use of the software within your company as a reference, in read only form, for the sole purposes
 * of debugging your products, maintaining your products, or enhancing the interoperability of your products with the
 * software, and specifically excludes the right to distribute the software outside of your company.
 * "Licensed patents" means any Licensor patent claims which read directly on the software as distributed by the Licensor
 * under this license.
 *
 * 2. Grant of Rights
 * (A) Copyright Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 * worldwide, royalty-free copyright license to reproduce the software for reference use.
 * (B) Patent Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 * worldwide, royalty-free patent license under licensed patents for reference use.
 *
 * 3. Limitations
 * (A) No Trademark License- This license does not grant you any rights to use the Licensor’s name, logo, or trademarks.
 * (B) If you begin patent litigation against the Licensor over patents that you think may apply to the software
 * (including a cross-claim or counterclaim in a lawsuit), your license to the software ends automatically.
 * (C) The software is licensed "as-is." You bear the risk of using it. The Licensor gives no express warranties,
 * guarantees or conditions. You may have additional consumer rights under your local laws which this license cannot
 * change. To the extent permitted under your local laws, the Licensor excludes the implied warranties of merchantability,
 * fitness for a particular purpose and non-infringement.
 */
package com.mycelium.wallet.activity

import android.app.Activity
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
import androidx.annotation.IdRes
import androidx.annotation.RawRes
import com.google.common.base.Joiner
import com.mycelium.wallet.activity.modern.Toaster.toast
import com.mycelium.wapi.wallet.WalletManager.getModuleById
import com.mycelium.wapi.wallet.fio.FioModule.getFioServerLogsListAndClear
import de.cketti.library.changelog.ChangeLog
import com.mycelium.wallet.activity.modern.DarkThemeChangeLog
import com.mycelium.wallet.api.AbstractCallbackHandler
import com.mycelium.wallet.activity.modern.Toaster
import com.mycelium.wallet.activity.ConnectionLogsActivity
import com.mycelium.wallet.activity.util.QrImageView
import com.google.common.io.ByteSource
import com.mycelium.wallet.*
import com.mycelium.wapi.api.WapiException
import com.mycelium.wapi.api.response.VersionInfoExResponse
import com.mycelium.wapi.wallet.fio.FioModule
import java.io.IOException
import java.io.InputStream
import java.lang.RuntimeException
import java.nio.charset.StandardCharsets
import java.util.*

class AboutActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.about_activity)
        val mbwManager = MbwManager.getInstance(this)
        val versionManager = mbwManager.versionManager
        (findViewById<View>(R.id.tvVersionNumber) as TextView).text = BuildConfig.VERSION_NAME + if (BuildConfig.DEBUG) "\nDebug Build" else ""
        (findViewById<View>(R.id.tvVersionCode) as TextView).text = String.format(Locale.US, "(%d)", BuildConfig.VERSION_CODE)
        setLicenseForButton(R.id.bt_tou_mycelium, R.raw.tou_mycelium)
        setLicenseForButton(R.id.bt_license_mycelium, R.raw.license_mycelium)
        setLicenseForButton(R.id.bt_license_zxing, R.raw.license_zxing)
        setLicenseForButton(R.id.bt_license_pdfwriter, R.raw.license_pdfwriter)
        setLicenseForButton(R.id.bt_special_thanks, R.raw.special_thanks)
        findViewById<View>(R.id.bt_show_changelog).setOnClickListener { view: View? ->
            val cl: ChangeLog = DarkThemeChangeLog(this)
            cl.fullLogDialog.show()
        }
        findViewById<View>(R.id.bt_check_update).setOnClickListener { v: View? ->
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
        findViewById<View>(R.id.bt_show_server_info).setOnClickListener { view: View? -> ConnectionLogsActivity.callMe(this) }
        if (BuildConfig.BUILD_TYPE === "debug") {
            findViewById<View>(R.id.bt_fio_server_error_logs).visibility = View.VISIBLE
            findViewById<View>(R.id.bt_fio_server_error_logs).setOnClickListener { view: View? ->
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
        setLinkTo(findViewById(R.id.tvSourceUrl), R.string.source_url)
        setLinkTo(findViewById(R.id.tvHomepageUrl), R.string.homepage_url)
        setMailTo(findViewById(R.id.tvContactEmail))

        //set playstore link to qr code
        val packageName = applicationContext.packageName
        val playstoreUrl = Constants.PLAYSTORE_BASE_URL + packageName
        val playstoreQr: QrImageView = findViewById(R.id.ivPlaystoreQR)
        playstoreQr.qrCode = playstoreUrl
        playstoreQr.tapToCycleBrightness = false
        playstoreQr.setOnClickListener { v: View? ->
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(playstoreUrl)
            startActivity(intent)
        }

        // show direct apk link for the - very unlikely - case that google blocks our playstore entry
        val directApkQr: QrImageView = findViewById(R.id.ivDirectApkQR)
        directApkQr.qrCode = Constants.DIRECT_APK_URL
        directApkQr.tapToCycleBrightness = false
        directApkQr.setOnClickListener { v: View? ->
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(Constants.DIRECT_APK_URL)
            startActivity(intent)
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
        val httplink = Uri.parse(resources.getString(res))
        textView.text = Html.fromHtml(hrefLink(httplink))
        textView.movementMethod = LinkMovementMethod.getInstance()
    }

    private fun setMailTo(textView: TextView) {
        val mailAddress = resources.getString(R.string.contact_email)
        textView.text = Html.fromHtml("<a href=\"mailto:$mailAddress\">$mailAddress</a>")
        textView.movementMethod = LinkMovementMethod.getInstance()
    }

    private fun hrefLink(githubLink: Uri): String {
        return "<a href=\"$githubLink\">$githubLink</a>"
    }

    private fun setLicenseForButton(@IdRes buttonId: Int, @RawRes fileId: Int) {
        findViewById<View>(buttonId).setOnClickListener { v: View? ->
            val message: String
            message = try {
                Joiner.on("\n").join(
                        object : ByteSource() {
                            override fun openStream(): InputStream {
                                return@setOnClickListener resources.openRawResource(fileId)
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