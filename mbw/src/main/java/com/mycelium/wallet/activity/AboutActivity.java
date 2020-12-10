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

package com.mycelium.wallet.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.IdRes;
import androidx.annotation.RawRes;
import com.google.common.base.Joiner;
import com.google.common.io.ByteSource;
import com.mycelium.wallet.*;
import com.mycelium.wallet.activity.modern.DarkThemeChangeLog;
import com.mycelium.wallet.activity.modern.Toaster;
import com.mycelium.wallet.activity.util.QrImageView;
import com.mycelium.wapi.api.response.VersionInfoExResponse;
import com.mycelium.wapi.wallet.fio.FioModule;

import de.cketti.library.changelog.ChangeLog;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static java.util.Locale.US;

public class AboutActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.about_activity);

        final MbwManager mbwManager = MbwManager.getInstance(this);
        final VersionManager versionManager = mbwManager.getVersionManager();
        ((TextView) findViewById(R.id.tvVersionNumber)).setText(BuildConfig.VERSION_NAME);
        ((TextView) findViewById(R.id.tvVersionCode)).setText(String.format(US, "(%d)", BuildConfig.VERSION_CODE));
        setLicenseForButton(R.id.bt_tou_mycelium, R.raw.tou_mycelium);
        setLicenseForButton(R.id.bt_license_mycelium, R.raw.license_mycelium);
        setLicenseForButton(R.id.bt_license_zxing, R.raw.license_zxing);
        setLicenseForButton(R.id.bt_license_pdfwriter, R.raw.license_pdfwriter);
        setLicenseForButton(R.id.bt_special_thanks, R.raw.special_thanks);

        findViewById(R.id.bt_show_changelog).setOnClickListener(view -> {
            ChangeLog cl = new DarkThemeChangeLog(this);
            cl.getFullLogDialog().show();
        });

        findViewById(R.id.bt_check_update).setOnClickListener(v -> {
            final ProgressDialog progress = ProgressDialog.show(this, getString(R.string.update_check),
                    getString(R.string.please_wait), true);
            versionManager.checkForUpdateSync((response, exception) -> {
                progress.dismiss();
                if (exception != null) {
                    new Toaster(this).toast(R.string.version_check_failed, false);
                    mbwManager.reportIgnoredException(new RuntimeException("WapiException: " + exception.errorCode));
                } else {
                    showVersionInfo(versionManager, response);
                }
            });
        });

        findViewById(R.id.bt_show_server_info).setOnClickListener(view -> ConnectionLogsActivity.callMe(this));

        findViewById(R.id.bt_fio_server_error_logs).setOnClickListener(view -> {
            FioModule fioModule = (FioModule) mbwManager.getWalletManager(false).getModuleById(FioModule.ID);
            List<String> logs = fioModule.getFioServerLogsListAndClear();
            if (logs.isEmpty()) {
                Toast.makeText(this, getString(R.string.no_logs), Toast.LENGTH_SHORT).show();
            } else {
                String joined = TextUtils.join("\n", logs);
                Utils.setClipboardString(joined, this);
                Toast.makeText(this, getString(R.string.copied_to_clipboard), Toast.LENGTH_SHORT).show();
            }
        });

        setLinkTo(findViewById(R.id.tvSourceUrl), R.string.source_url);
        setLinkTo(findViewById(R.id.tvHomepageUrl), R.string.homepage_url);

        setMailTo(findViewById(R.id.tvContactEmail));

        //set playstore link to qr code
        String packageName = getApplicationContext().getPackageName();
        final String playstoreUrl = Constants.PLAYSTORE_BASE_URL + packageName;
        QrImageView playstoreQr = findViewById(R.id.ivPlaystoreQR);
        playstoreQr.setQrCode(playstoreUrl);
        playstoreQr.setTapToCycleBrightness(false);
        playstoreQr.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(playstoreUrl));
            startActivity(intent);
        });

        // show direct apk link for the - very unlikely - case that google blocks our playstore entry
        QrImageView directApkQr = findViewById(R.id.ivDirectApkQR);
        directApkQr.setQrCode(Constants.DIRECT_APK_URL);
        directApkQr.setTapToCycleBrightness(false);
        directApkQr.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(Constants.DIRECT_APK_URL));
            startActivity(intent);
        });
    }

    private void showVersionInfo(VersionManager versionManager, VersionInfoExResponse response) {
        if (response == null || versionManager.isSameVersion(response.versionNumber)) {
            new AlertDialog.Builder(this).setMessage(getString(R.string.version_uptodate, BuildConfig.VERSION_NAME))
                    .setTitle(getString(R.string.update_check))
                    .setNeutralButton(R.string.ok, (dialog, which) -> dialog.dismiss())
                    .show();
        } else {
            versionManager.showVersionDialog(response, this);
        }
    }

    private void setLinkTo(TextView textView, int res) {
        Uri httplink = Uri.parse(getResources().getString(res));
        textView.setText(Html.fromHtml(hrefLink(httplink)));
        textView.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private void setMailTo(TextView textView) {
        String mailAddress = getResources().getString(R.string.contact_email);
        textView.setText(Html.fromHtml("<a href=\"mailto:" + mailAddress + "\">" + mailAddress + "</a>"));
        textView.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private String hrefLink(Uri githubLink) {
        return "<a href=\"" + githubLink + "\">" + githubLink + "</a>";
    }

    private void setLicenseForButton(@IdRes int buttonId, @RawRes int fileId) {
        findViewById(buttonId).setOnClickListener(v -> {
            final String message;
            try {
                message = Joiner.on("\n").join(
                        (new ByteSource() {
                            @Override
                            public InputStream openStream() {
                                return getResources().openRawResource(fileId);
                            }
                        }).asCharSource(StandardCharsets.UTF_8).readLines());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            new AlertDialog.Builder(this)
                    .setMessage(message).setCancelable(true)
                    .setPositiveButton("Okay", (dialog, id) -> dialog.cancel())
                    .show();
        });
    }
}
