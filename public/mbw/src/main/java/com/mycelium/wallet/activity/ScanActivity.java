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
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.view.Surface;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.zxing.client.android.CaptureActivity;
import com.google.zxing.client.android.Intents;
import com.mrd.bitlib.crypto.Bip38;
import com.mrd.bitlib.crypto.MrdExport;
import com.mrd.bitlib.crypto.MrdExport.DecodingException;
import com.mrd.bitlib.crypto.MrdExport.V1.EncryptionParameters;
import com.mrd.bitlib.crypto.MrdExport.V1.InvalidChecksumException;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.NetworkParameters;
import com.mrd.mbwapi.api.ExchangeRate;
import com.mycelium.wallet.*;
import com.mycelium.wallet.activity.export.DecryptBip38PrivateKeyActivity;
import com.mycelium.wallet.activity.export.DecryptPrivateKeyActivity;
import com.mycelium.wallet.activity.modern.Toaster;

/**
 * This activity immediately launches the scanner, and shows no content of its
 * own. If a scan result comes back it parses it and may launch other activities
 * to decode the result. This happens for instance when decrypting private keys.
 */
public class ScanActivity extends Activity {

   public static void callMe(Activity currentActivity,int requestCode, ScanRequest scanRequest) {
      Intent intent = new Intent(currentActivity, ScanActivity.class);
      intent.putExtra("request", scanRequest);
      currentActivity.startActivityForResult(intent, requestCode);
   }

   public static void callMe(Fragment currentFragment,int requestCode, ScanRequest scanRequest) {
      Intent intent = new Intent(currentFragment.getActivity(), ScanActivity.class);
      intent.putExtra("request", scanRequest);
      currentFragment.startActivityForResult(intent, requestCode);
   }
   
   public static final String RESULT_PAYLOAD = "payload";
   public static final String RESULT_ERROR = "error";
   public static final String RESULT_RECORD_KEY = "record";
   public static final String RESULT_URI_KEY = "uri";
   public static final String RESULT_ADDRESS_KEY = "address";
   public static final String RESULT_TYPE_KEY = "type";
   public static final String RESULT_AMOUNT_KEY = "amount";

   public enum ResultType {ADDRESS, RECORD, URI}

   public static final int SCANNER_RESULT_CODE = 0;
   public static final int IMPORT_ENCRYPTED_PRIVATE_KEY_CODE = 1;
   public static final int IMPORT_ENCRYPTED_BIP38_PRIVATE_KEY_CODE = 2;

   private MbwManager _mbwManager;
   private boolean _hasLaunchedScanner;
   private int _preferredOrientation;
   private ScanRequest scanRequest = null;

   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      _mbwManager = MbwManager.getInstance(this);
      Intent intent = getIntent();
      scanRequest = Preconditions.checkNotNull((ScanRequest) intent.getSerializableExtra("request"));
      // Did we already launch the scanner?
      if (savedInstanceState != null) {
         _hasLaunchedScanner = savedInstanceState.getBoolean("hasLaunchedScanner", false);
      }
      // Make sure that we make the screen rotate right after scanning
      if (_hasLaunchedScanner) {
         // the scanner has been launched earlier. This means that we have
         // stored our previous orientation and that we want to try and restore it
         Preconditions.checkNotNull(savedInstanceState);
         _preferredOrientation = savedInstanceState.getInt("lastOrientation", -1);
         if (getScreenOrientation() != _preferredOrientation) {
            //noinspection ResourceType
            setRequestedOrientation(_preferredOrientation);
         }
      } else {
         // The scanner has not been launched yet. Get our current orientation
         // so we can restore it after scanning
         _preferredOrientation = getScreenOrientation();
      }
   }

   @Override
   public void onResume() {
      if (!_hasLaunchedScanner) {
         startScanner();
         _hasLaunchedScanner = true;
      }
      super.onResume();
   }

   private void startScanner() {
      Intent intent = new Intent(this, CaptureActivity.class);
      intent.putExtra(Intents.Scan.MODE, Intents.Scan.QR_CODE_MODE);
      intent.putExtra(Intents.Scan.ENABLE_CONTINUOUS_FOCUS, MbwManager.getInstance(this).getContinuousFocus());
      this.startActivityForResult(intent, SCANNER_RESULT_CODE);
   }

   @Override
   protected void onSaveInstanceState(Bundle outState) {
      outState.putInt("lastOrientation", _preferredOrientation);
      outState.putBoolean("hasLaunchedScanner", _hasLaunchedScanner);
      super.onSaveInstanceState(outState);
   }

   private int getScreenOrientation() {
      int rotation = getWindowManager().getDefaultDisplay().getRotation();
      DisplayMetrics dm = new DisplayMetrics();
      getWindowManager().getDefaultDisplay().getMetrics(dm);
      int width = dm.widthPixels;
      int height = dm.heightPixels;
      int orientation;
      // if the device's natural orientation is portrait:
      if ((rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) && height > width
            || (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) && width > height) {
         switch (rotation) {
         case Surface.ROTATION_0:
            orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
            break;
         case Surface.ROTATION_90:
            orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
            break;
         case Surface.ROTATION_180:
            orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
            break;
         case Surface.ROTATION_270:
            orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
            break;
         default:
            // Unknown screen orientation. Defaulting to portrait.
            orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
            break;
         }
      }
      // if the device's natural orientation is landscape or if the device is square:
      else {
         switch (rotation) {
         case Surface.ROTATION_0:
            orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
            break;
         case Surface.ROTATION_90:
            orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
            break;
         case Surface.ROTATION_180:
            orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
            break;
         case Surface.ROTATION_270:
            orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
            break;
         default:
            // Unknown screen orientation. Defaulting to landscape.
            orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
            break;
         }
      }
      return orientation;
   }

   @Override
   public void onActivityResult(final int requestCode, final int resultCode, final Intent intent) {
      if (Activity.RESULT_CANCELED == resultCode) {
         finishError(R.string.cancelled, "");
         return;
      }

      // If the last autofocus setting got saved in an extra-field, change the app settings accordingly
      int autoFocus = intent.getIntExtra("ENABLE_CONTINUOUS_FOCUS", -1);
      if (autoFocus != -1){
         MbwManager.getInstance(this).setContinuousFocus(autoFocus == 1);
      }

      String content;
      if (IMPORT_ENCRYPTED_BIP38_PRIVATE_KEY_CODE == requestCode) {
         content = intent.getStringExtra("base58Key");
      } else if (IMPORT_ENCRYPTED_PRIVATE_KEY_CODE == requestCode) {
         content = handleDecryptedMrdPrivateKey(intent);
      } else {
         Preconditions.checkState(SCANNER_RESULT_CODE == requestCode);
         if (!isQRCode(intent)) {
            finishError(R.string.unrecognized_format, "");
            return;
         }
         content = intent.getStringExtra("SCAN_RESULT").trim();
         //TODO: if we do not want to handle data, do not decrypt it first
         //(check for scanrequest action none on priv key, seed?
         if (isMrdEncryptedPrivateKey(content)) {
            Optional<String> key = handleMrdEncryptedPrivateKey(content);
            if (key.isPresent()) {
               content = key.get();
            } else {
               // the handleMRdEncrypted method has started the decryption, which will trigger another onActivity
               return;
            }
         } else if (Bip38.isBip38PrivateKey(content)) {
            DecryptBip38PrivateKeyActivity.callMe(this, content, ScanActivity.IMPORT_ENCRYPTED_BIP38_PRIVATE_KEY_CODE);
            //we do not finish cause after decryption another onActivityResult will be called
            return;
         }
      }

      boolean wasHandled = false;
      for (ScanRequest.Action action : scanRequest.getAllActions()) {
         if (action.handle(this, content)) {
            wasHandled = true;
            break;
         }
      }
      if (!wasHandled) {
         finishError(R.string.unrecognized_format, content);
      }
   }

   private boolean isQRCode(Intent intent) {
      if ("QR_CODE".equals(intent.getStringExtra("SCAN_RESULT_FORMAT"))) {
         return true;
      }
      return false;
   }

   private boolean isMrdEncryptedPrivateKey(String string) {
      int version;
      try {
         version = MrdExport.decodeVersion(string);
      } catch (DecodingException e) {
         return false;
      }
      return version == MrdExport.V1_VERSION;
   }

   private Optional<String> handleMrdEncryptedPrivateKey(String encryptedPrivateKey) {
      EncryptionParameters encryptionParameters = _mbwManager.getCachedEncryptionParameters();
      // Try and decrypt with cached parameters if we have them
      if (encryptionParameters != null) {
         try {
            String key = MrdExport.V1.decrypt(encryptionParameters, encryptedPrivateKey, _mbwManager.getNetwork());
            Preconditions.checkNotNull(Record.fromString(key, _mbwManager.getNetwork()));
            return Optional.of(key);
         } catch (InvalidChecksumException e) {
            // We cannot reuse the cached password, fall through and decrypt
            // with an entered password
         } catch (DecodingException e) {
            finishError(R.string.unrecognized_format, encryptedPrivateKey);
            return Optional.absent();
         }
      }
      // Start activity to ask the user to enter a password and decrypt the key
      DecryptPrivateKeyActivity.callMe(this, encryptedPrivateKey, IMPORT_ENCRYPTED_PRIVATE_KEY_CODE);
      return Optional.absent();
   }

   private String handleDecryptedMrdPrivateKey(Intent intent) {
      String key = intent.getStringExtra("base58Key");
      // Cache the encryption parameters for next import
      EncryptionParameters encryptionParameters = (MrdExport.V1.EncryptionParameters) intent
            .getSerializableExtra("encryptionParameters");
      _mbwManager.setCachedEncryptionParameters(encryptionParameters);
      return key;
   }

   public void finishError(int resId, String payload) {
      Intent result = new Intent();
      result.putExtra(RESULT_ERROR, getResources().getString(resId));
      result.putExtra(RESULT_PAYLOAD, payload);
      setResult(RESULT_CANCELED, result);
      finish();
   }

   public void finishOk(BitcoinUri bitcoinUri) {
      Intent result = new Intent();
      result.putExtra(RESULT_URI_KEY, bitcoinUri);
      result.putExtra(RESULT_TYPE_KEY, ResultType.URI);
      setResult(RESULT_OK, result);
      finish();
   }

   public void finishOk(Record record) {
      Intent result = new Intent();
      result.putExtra(RESULT_RECORD_KEY, record);
      result.putExtra(RESULT_TYPE_KEY, ResultType.RECORD);
      setResult(RESULT_OK, result);
      finish();
   }

   public void finishOk(Record record, Long amount) {
      Intent result = new Intent();
      result.putExtra(RESULT_RECORD_KEY, record);
      result.putExtra(RESULT_TYPE_KEY, ResultType.RECORD);
      if (amount != null && amount > 0) {
         result.putExtra(RESULT_AMOUNT_KEY, amount);
      }
      setResult(RESULT_OK, result);
      finish();
   }

   public void finishOk(Address address) {
      Intent result = new Intent();
      result.putExtra(RESULT_ADDRESS_KEY, address);
      result.putExtra(RESULT_TYPE_KEY, ResultType.ADDRESS);
      setResult(RESULT_OK, result);
      finish();
   }

   public void finishOk() {
      setResult(RESULT_OK);
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

   public static Record getRecord(Intent intent) {
      ScanActivity.checkType(intent, ResultType.RECORD);
      Record record = (Record) intent.getSerializableExtra(RESULT_RECORD_KEY);
      Preconditions.checkNotNull(record);
      return record;
   }

   public static Optional<Long> getAmount(Intent intent) {
      return Optional.fromNullable((Long) intent.getSerializableExtra(RESULT_AMOUNT_KEY));
   }

   public static Address getAddress(Intent intent) {
      ScanActivity.checkType(intent, ResultType.ADDRESS);
      Address address = (Address) intent.getSerializableExtra(RESULT_ADDRESS_KEY);
      Preconditions.checkNotNull(address);
      return address;
   }

   public static void checkType(Intent intent, ResultType type) {
      Preconditions.checkState(type == intent.getSerializableExtra(RESULT_TYPE_KEY));
   }

   public RecordManager getRecordManager() {
      return _mbwManager.getRecordManager();
   }

   public Wallet getWallet() {
      return getRecordManager().getWallet(_mbwManager.getWalletMode());
   }

   public ExchangeRate getExchangeRate() {
      return _mbwManager.getExchangeRateManager().getExchangeRate();
   }

   public Double getPrice() {
      return (getExchangeRate() == null ? null : getExchangeRate().price);
   }

   public NetworkParameters getNetwork() {
      return _mbwManager.getNetwork();
   }

   public BlockChainAddressTracker getBlockChainAddressTracker() {
      return _mbwManager.getBlockChainAddressTracker();
   }
}
