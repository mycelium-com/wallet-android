/*
 * Copyright 2013 Megion Research and Development GmbH
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

import java.io.IOException;
import java.io.InputStream;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import com.google.common.io.InputSupplier;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.NetworkParameters;
import com.mrd.mbwapi.api.ApiError;
import com.mrd.mbwapi.api.WalletVersionResponse;
import com.mycelium.wallet.Constants;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.VersionManager;
import com.mycelium.wallet.activity.modern.Toaster;
import com.mycelium.wallet.activity.send.SendInitializationActivity;
import com.mycelium.wallet.api.AbstractCallbackHandler;
import com.mycelium.wapi.wallet.WalletAccount;

public class AboutActivity extends Activity {
   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);

      setContentView(R.layout.about_activity);

      final MbwManager mbwManager = MbwManager.getInstance(this);
      final VersionManager versionManager = mbwManager.getVersionManager();
      String version = versionManager.getVersion();
      ((TextView) findViewById(R.id.tvVersionNumber)).setText(version);
      findViewById(R.id.bt_license_mycelium).setOnClickListener(new ShowLicenseListener(R.raw.license_mycelium));
      findViewById(R.id.bt_license_zxing).setOnClickListener(new ShowLicenseListener(R.raw.license_zxing));
      findViewById(R.id.bt_license_pdfwriter).setOnClickListener(new ShowLicenseListener(R.raw.license_pdfwriter));
      findViewById(R.id.bt_special_thanks).setOnClickListener(new ShowLicenseListener(R.raw.special_thanks));

      findViewById(R.id.bt_check_update).setOnClickListener(new View.OnClickListener() {
         @Override
         public void onClick(View v) {
            final ProgressDialog progress = ProgressDialog.show(AboutActivity.this, getString(R.string.update_check),
                  getString(R.string.please_wait), true);
            versionManager.forceCheckForUpdate(new AbstractCallbackHandler<WalletVersionResponse>() {
               @Override
               public void handleCallback(WalletVersionResponse response, ApiError exception) {
                  progress.dismiss();
                  if (exception != null) {
                     new Toaster(AboutActivity.this).toast(R.string.version_check_failed, false);
                     mbwManager.reportIgnoredException(new RuntimeException(exception.errorMessage));
                  } else {
                     showVersionInfo(versionManager, response);
                  }
               }
            });
         }
      });

      findViewById(R.id.btDonate).setOnClickListener(donateClickListener);

      setLinkTo((TextView) findViewById(R.id.tvSourceUrl), R.string.source_url);
      setLinkTo((TextView) findViewById(R.id.tvHomepageUrl), R.string.homepage_url);

      setMailTo((TextView) findViewById(R.id.tvContactEmail), R.string.contact_email);
   }

   OnClickListener donateClickListener = new OnClickListener() {

      @Override
      public void onClick(View v) {
         Utils.showSimpleMessageDialog(AboutActivity.this, R.string.donate_description, new Runnable() {
            
            @Override
            public void run() {
               MbwManager mbwManager = MbwManager.getInstance(AboutActivity.this);
               NetworkParameters network = mbwManager.getNetwork();
               Address address = network.isProdnet() ? Address.fromString(Constants.PRODNET_DONATION_ADDRESS) :Address.fromString(Constants.TESTNET_DONATION_ADDRESS);
               WalletAccount account = mbwManager.getSelectedAccount();
               SendInitializationActivity.callMe(AboutActivity.this, account.getId(), null, address, false);
            }
         });
      }
   };

   private void showVersionInfo(VersionManager versionManager, WalletVersionResponse response) {
      if (versionManager.isSameVersion(response.versionNumber)) {
         new AlertDialog.Builder(this).setMessage(getString(R.string.version_uptodate, response.versionNumber))
               .setTitle(getString(R.string.update_check))
               .setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
                  @Override
                  public void onClick(DialogInterface dialog, int which) {
                     dialog.dismiss();
                  }
               }).create().show();
      } else {
         versionManager.showVersionDialog(response, this);
      }
   }

   private void setLinkTo(TextView textView, int res) {
      Uri httplink = Uri.parse(getResources().getString(res));
      textView.setText(Html.fromHtml(hrefLink(httplink)));
      textView.setMovementMethod(LinkMovementMethod.getInstance());
   }

   private void setMailTo(TextView textView, int res) {
      String mail_address = getResources().getString(res);
      textView.setText(Html.fromHtml("<a href=\"mailto:" + mail_address + "\">" + mail_address + "</a>"));
      textView.setMovementMethod(LinkMovementMethod.getInstance());
   }

   private String hrefLink(Uri github_link) {
      return "<a href=\"" + github_link + "\">" + github_link + "</a>";
   }

   private class ShowLicenseListener implements View.OnClickListener {
      private final int resourceId;

      public ShowLicenseListener(int resourceId) {
         this.resourceId = resourceId;
      }

      @Override
      public void onClick(View v) {
         final String message;
         try {
            message = CharStreams.toString(CharStreams.newReaderSupplier(new InputSupplier<InputStream>() {
               @Override
               public InputStream getInput() throws IOException {

                  return getResources().openRawResource(resourceId);
               }
            }, Charsets.UTF_8));
         } catch (IOException e) {
            throw new RuntimeException(e);
         }
         AlertDialog.Builder builder = new AlertDialog.Builder(AboutActivity.this);
         builder.setMessage(message).setCancelable(true)
               .setPositiveButton("Okay", new DialogInterface.OnClickListener() {
                  public void onClick(DialogInterface dialog, int id) {
                     dialog.cancel();
                  }
               }).show();
      }
   }

   @SuppressWarnings("unused")
   private class LinkListener implements View.OnClickListener {
      private final int resource;

      public LinkListener(int resource) {
         this.resource = resource;
      }

      @Override
      public void onClick(View v) {
         Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(getResources().getString(resource)));
         AboutActivity.this.startActivity(browserIntent);
      }
   }
}
