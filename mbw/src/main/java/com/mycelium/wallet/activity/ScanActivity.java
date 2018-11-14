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
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.view.Surface;

import com.google.common.base.Preconditions;
import com.google.zxing.client.android.Intents;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.StringHandleConfig;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.activity.modern.Toaster;

/**
 * This activity immediately launches the scanner, and shows no content of its
 * own. If a scan result comes back it parses it and may launch other activities
 * to decode the result. This happens for instance when decrypting private keys.
 */
public class ScanActivity extends Activity {
   private boolean hasCameraPermission;

   public static void callMe(Activity currentActivity, int requestCode, StringHandleConfig stringHandleConfig) {
      Intent intent = new Intent(currentActivity, ScanActivity.class)
              .putExtra("request", stringHandleConfig);
      currentActivity.startActivityForResult(intent, requestCode);
   }

   public static void callMe(Fragment currentFragment, int requestCode, StringHandleConfig stringHandleConfig) {
      Intent intent = new Intent(currentFragment.getActivity(), ScanActivity.class)
              .putExtra("request", stringHandleConfig);
      currentFragment.startActivityForResult(intent, requestCode);
   }

   public static final int SCANNER_RESULT_CODE = 0;

   private boolean _hasLaunchedScanner;
   private int _preferredOrientation;
   private StringHandleConfig _stringHandleConfig = null;

   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      hasCameraPermission = Utils.hasOrRequestCameraAccess(this);
      if(!hasCameraPermission) {
        // finishError(R.string.no_camera_permission);
        return;
      }
      Intent intent = getIntent();
      _stringHandleConfig = Preconditions.checkNotNull((StringHandleConfig) intent.getSerializableExtra("request"));
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
      if (!_hasLaunchedScanner && hasCameraPermission) {
         startScanner();
         _hasLaunchedScanner = true;
      }
      super.onResume();
   }

   private void startScanner() {
      Intent intent = new Intent(this, CustomCaptureActivity.class)
              .putExtra(Intents.Scan.MODE, Intents.Scan.QR_CODE_MODE)
              .putExtra(Intents.Scan.BEEP_ENABLED, false);
      startActivityForResult(intent, SCANNER_RESULT_CODE);
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
   public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
      switch (requestCode) {
         case Utils.REQUEST_CAMERA: {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
               recreate();
            } else {
               finishError(R.string.no_camera_permission);
            }
         }
      }
   }

   @Override
   public void onActivityResult(final int requestCode, final int resultCode, final Intent intent) {
      if (Activity.RESULT_CANCELED == resultCode) {
         finishError(R.string.cancelled);
         return;
      }

      //since it was not the handler, it can only be the scanner
      Preconditions.checkState(SCANNER_RESULT_CODE == requestCode);

      if (!isQRCode(intent)) {
         finishError(R.string.unrecognized_format);
         return;
      }

      String content = intent.getStringExtra("SCAN_RESULT").trim();
      // Get rid of any UTF-8 BOM marker. Those should not be present, but might have slipped in nonetheless,
      if (content.length() != 0 && content.charAt(0) == '\uFEFF') content = content.substring(1);

      // Call the stringHandler activity and pass its result to our caller
      Intent handlerIntent = StringHandlerActivity.getIntent(this, _stringHandleConfig, content)
              .setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
      this.startActivity(handlerIntent);

      // we are done here...
      this.finish();
   }

   private boolean isQRCode(Intent intent) {
      return "QR_CODE".equals(intent.getStringExtra("SCAN_RESULT_FORMAT"));
   }

   public void finishError(int resId) {
      Intent result = new Intent()
              .putExtra(StringHandlerActivity.RESULT_ERROR, getResources().getString(resId));
      setResult(RESULT_CANCELED, result);
      finish();
   }

   public static void toastScanError(int resultCode, Intent intent, Activity activity) {
      if (intent == null) {
         return; // no result, user pressed back
      }
      if (resultCode == RESULT_CANCELED) {
         String error = intent.getStringExtra(StringHandlerActivity.RESULT_ERROR);
         if (error != null) {
            new Toaster(activity).toast(error, false);
         }
      }
   }
}
