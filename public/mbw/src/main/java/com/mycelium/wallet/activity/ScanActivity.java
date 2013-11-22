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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.google.common.base.Preconditions;
import com.mrd.bitlib.crypto.MrdExport;
import com.mrd.bitlib.crypto.MrdExport.DecodingException;
import com.mrd.bitlib.crypto.MrdExport.V1.EncryptionParameters;
import com.mrd.bitlib.crypto.MrdExport.V1.InvalidChecksumException;
import com.mycelium.wallet.BitcoinUri;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Record;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.activity.export.DecryptPrivateKeyActivity;
import com.mycelium.wallet.activity.modern.Toaster;

/**
 * This activity immediately launches the scanner, and shows no content of its
 * own. If a scan result comes back it parses it and may launch other activities
 * to decode the result. This happens for instance when decrypting private keys.
 */
public class ScanActivity extends Activity {

   public static void callMe(Activity currentActivity, int requestCode) {
      Intent intent = new Intent(currentActivity, ScanActivity.class);
      currentActivity.startActivityForResult(intent, requestCode);
   }

   private static final String RESULT_PAYLOAD = "payload";
   public static final String RESULT_ERROR = "error";

   public static final String RESULT_RECORD_KEY = "record";
   public static final String RESULT_URI_KEY = "uri";
   private static final int SCANNER_RESULT_CODE = 0;
   private static final int IMPORT_ENCRYPTED_PRIVATE_KEY_CODE = 1;

   private MbwManager _mbwManager;
   private boolean _hasLaunchedScanner;

   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      _mbwManager = MbwManager.getInstance(this);
      if (savedInstanceState != null) {
         _hasLaunchedScanner = savedInstanceState.getBoolean("hasLaunchedScanner", false);
      }
   }

   @Override
   public void onResume() {
      if (!_hasLaunchedScanner) {
         Utils.startScannerIntent(this, SCANNER_RESULT_CODE, _mbwManager.getContinuousFocus());
         _hasLaunchedScanner = true;
      }
      super.onResume();
   }

   @Override
   protected void onSaveInstanceState(Bundle outState) {
      outState.putBoolean("hasLaunchedScanner", _hasLaunchedScanner);
      super.onSaveInstanceState(outState);
   }

   @Override
   public void onDestroy() {
      super.onDestroy();
   }

   @Override
   public void onActivityResult(final int requestCode, final int resultCode, final Intent intent) {
      if (requestCode == SCANNER_RESULT_CODE && resultCode == Activity.RESULT_CANCELED) {
         finishError(R.string.cancelled, "");
         return;
      }
      if (requestCode == SCANNER_RESULT_CODE && resultCode == Activity.RESULT_OK) {
         handleScannerIntentResult(intent);
      } else if (requestCode == IMPORT_ENCRYPTED_PRIVATE_KEY_CODE && resultCode == Activity.RESULT_OK) {
         handleDecryptedPrivateKey(intent);
      } else if (requestCode == IMPORT_ENCRYPTED_PRIVATE_KEY_CODE && resultCode == Activity.RESULT_CANCELED) {
         finishError(R.string.cancelled, "");
      } else {
         super.onActivityResult(requestCode, resultCode, intent);
      }
   }

   private void handleScannerIntentResult(final Intent intent) {
      if (!"QR_CODE".equals(intent.getStringExtra("SCAN_RESULT_FORMAT"))) {
         finishError(R.string.unrecognized_format, "");
         return;
      }
      String contents = intent.getStringExtra("SCAN_RESULT").trim();
      Record record = Record.fromString(contents, _mbwManager.getNetwork());

      if (record != null) {
         // The scan result might actually be a bitcoin URI, so try to create
         // one and pass the result along
         BitcoinUri uri = BitcoinUri.parse(contents, _mbwManager.getNetwork());
         finishOk(record, uri);
         return;
      }
      // Not an address or private key, maybe encrypted private key
      if (isEncryptedPrivateKey(contents)) {
         handleEncryptedPrivateKey(contents);
      } else {
         finishError(R.string.unrecognized_format, contents);
      }

   }

   private boolean isEncryptedPrivateKey(String string) {
      int version;
      try {
         version = MrdExport.decodeVersion(string);
      } catch (DecodingException e) {
         return false;
      }
      return version == MrdExport.V1_VERSION;
   }

   private void handleEncryptedPrivateKey(String encryptedPrivateKey) {
      // Check the version
      int version = 0;
      try {
         version = MrdExport.decodeVersion(encryptedPrivateKey);
      } catch (DecodingException e) {
         // Ignore, we fail gracefully in the version check below
      }
      if (version != MrdExport.V1_VERSION) {
         finishError(R.string.unrecognized_format_version, encryptedPrivateKey);
         return;
      }

      EncryptionParameters encryptionParameters = _mbwManager.getCachedEncryptionParameters();
      // Try and decrypt with cached parameters if we have them
      if (encryptionParameters != null) {
         try {
            String key = MrdExport.V1.decrypt(encryptionParameters, encryptedPrivateKey, _mbwManager.getNetwork());
            Record record = Preconditions.checkNotNull(Record.fromString(key, _mbwManager.getNetwork()));
            finishOk(record, new BitcoinUri(record.address, null, null));
            return;
         } catch (InvalidChecksumException e) {
            // We cannot reuse the cached password, fall through and decrypt
            // with an entered password
         } catch (DecodingException e) {
            finishError(R.string.unrecognized_format, encryptedPrivateKey);
            return;
         }
      }

      // Start activity to ask the user to enter a password and decrypt the key
      DecryptPrivateKeyActivity.callMe(this, encryptedPrivateKey, IMPORT_ENCRYPTED_PRIVATE_KEY_CODE);
   }

   private void handleDecryptedPrivateKey(Intent intent) {
      String key = intent.getStringExtra("base58Key");
      // Cache the key for next import
      EncryptionParameters encryptionParameters = (MrdExport.V1.EncryptionParameters) intent
            .getSerializableExtra("encryptionParameters");
      _mbwManager.setCachedEncryptionParameters(encryptionParameters);
      // Add the key
      Record record = Preconditions.checkNotNull(Record.fromString(key, _mbwManager.getNetwork()));
      finishOk(record, new BitcoinUri(record.address, null, null));
   }

   private void finishError(int resId, String payload) {
      Intent result = new Intent();
      result.putExtra(RESULT_ERROR, getResources().getString(resId));
      result.putExtra(RESULT_PAYLOAD, payload);
      setResult(RESULT_CANCELED, result);
      finish();
   }

   private void finishOk(Record record, BitcoinUri bitcoinUri) {
      Intent result = new Intent();
      result.putExtra(RESULT_RECORD_KEY, record);
      result.putExtra(RESULT_URI_KEY, bitcoinUri);
      setResult(RESULT_OK, result);
      finish();
   }

   public static void toastScanError(int resultCode, Intent intent, Activity activity) {
      if (intent == null) {
         return; // no result, user pressed back
      }
      if (resultCode == Activity.RESULT_CANCELED) {
         String error = intent.getStringExtra(ScanActivity.RESULT_ERROR);
         if (error != null) {
            new Toaster(activity).toast(error, false);
         }
      }
   }

}
