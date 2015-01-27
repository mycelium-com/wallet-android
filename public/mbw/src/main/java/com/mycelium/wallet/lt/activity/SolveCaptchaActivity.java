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

package com.mycelium.wallet.lt.activity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.mycelium.lt.api.model.Captcha;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.NumberEntry;
import com.mycelium.wallet.NumberEntry.NumberEntryListener;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.lt.LocalTraderEventSubscriber;
import com.mycelium.wallet.lt.LocalTraderManager;
import com.mycelium.wallet.lt.api.GetCaptcha;
import com.mycelium.wallet.lt.api.SolveCaptcha;

public class SolveCaptchaActivity extends Activity implements NumberEntryListener {

   public static void callMe(Activity currentActivity, int requestCode) {
      Intent intent = new Intent(currentActivity, SolveCaptchaActivity.class);
      intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
      currentActivity.startActivityForResult(intent, requestCode);
   }

   private MbwManager _mbwManager;
   private LocalTraderManager _ltManager;
   private NumberEntry _numberEntry;
   private ProgressBar _pbWait;
   private TextView _tvSolution;
   private Button _btDone;
   private ImageButton _btNew;
   private Captcha _captcha;
   private boolean _isSolving;
   private ProgressBar _pbSolving;

   /** Called when the activity is first created. */
   @Override
   public void onCreate(Bundle savedInstanceState) {
      this.requestWindowFeature(Window.FEATURE_NO_TITLE);
      this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
      super.onCreate(savedInstanceState);
      setContentView(R.layout.lt_solve_captcha_activity);
      _mbwManager = MbwManager.getInstance(this);
      _ltManager = _mbwManager.getLocalTraderManager();

      _tvSolution = ((TextView) findViewById(R.id.tvSolution));
      _btDone = (Button) findViewById(R.id.btDone);
      _btNew = (ImageButton) findViewById(R.id.btNew);
      _pbSolving = ((ProgressBar) findViewById(R.id.pbSolving));
      _pbWait = ((ProgressBar) findViewById(R.id.pbWait));
      _btNew = ((ImageButton) findViewById(R.id.btNew));

      _btDone.setOnClickListener(doneClickListener);
      _btNew.setOnClickListener(newClickListener);

      // Load saved state
      Integer solution = null;
      if (savedInstanceState != null) {
         _captcha = (Captcha) savedInstanceState.getSerializable("captcha");
         _isSolving = savedInstanceState.getBoolean("isSolving");
         solution = savedInstanceState.getInt("solution");
      }
      _numberEntry = new NumberEntry(0, this, this, solution == null ? "" : solution.toString());
   }

   OnClickListener doneClickListener = new OnClickListener() {

      @Override
      public void onClick(View arg0) {
         _isSolving = true;
         updateUi();
         _ltManager.makeRequest(new SolveCaptcha(_tvSolution.getText().toString()));
      }
   };

   OnClickListener newClickListener = new OnClickListener() {

      @Override
      public void onClick(View arg0) {
         reCaptcha();
      }
   };

   @Override
   protected void onResume() {
      _ltManager.subscribe(ltSubscriber);
      if (_captcha == null) {
         reCaptcha();
      }
      updateUi();
      super.onResume();
   }

   @Override
   protected void onSaveInstanceState(Bundle outState) {
      outState.putSerializable("captcha", _captcha);
      outState.putString("solution", _tvSolution.getText().toString());
      outState.putBoolean("isSolving", _isSolving);
      super.onSaveInstanceState(outState);
   }

   private void reCaptcha() {
      _captcha = null;
      _numberEntry.setEntry(null, 0);
      _ltManager.makeRequest(new GetCaptcha());
      updateUi();
   }

   private void updateUi() {
      Integer solution = getSolution();
      String solutionString = solution == null ? "" : solution.toString();
      _tvSolution.setText(solutionString);
      if (_captcha == null) {
         findViewById(R.id.llCaptcha).setVisibility(View.INVISIBLE);
         _pbWait.setVisibility(View.VISIBLE);
         _tvSolution.setText("");
         _btDone.setEnabled(false);
      } else {
         // Set bitmap
         ImageView iv = ((ImageView) findViewById(R.id.ivCaptcha));
         Bitmap b = BitmapFactory.decodeByteArray(_captcha.png, 0, _captcha.png.length);
         iv.setImageBitmap(b);
         // Hide progress bar and show captcha
         findViewById(R.id.llCaptcha).setVisibility(View.VISIBLE);
         _pbWait.setVisibility(View.INVISIBLE);
         // Clear solution and enable button
         if (_isSolving) {
            _btDone.setEnabled(false);
            _btNew.setEnabled(false);
            _pbSolving.setVisibility(View.VISIBLE);
         } else {
            _btDone.setEnabled(solutionString.length() == _captcha.length);
            _btNew.setEnabled(true);
            _pbSolving.setVisibility(View.INVISIBLE);
         }
      }
   }

   @Override
   protected void onPause() {
      _ltManager.unsubscribe(ltSubscriber);
      super.onPause();
   }

   private LocalTraderEventSubscriber ltSubscriber = new LocalTraderEventSubscriber(new Handler()) {

      @Override
      public void onLtError(int errorCode) {
         // Some other error
         Toast.makeText(SolveCaptchaActivity.this, R.string.lt_error_api_occurred, Toast.LENGTH_LONG).show();
         finish();
      }

      @Override
      public boolean onNoLtConnection() {
         Utils.toastConnectionError(SolveCaptchaActivity.this);
         finish();
         return true;
      }

      @Override
      public void onLtCaptchaFetched(com.mycelium.lt.api.model.Captcha captcha, GetCaptcha request) {
         _captcha = captcha;
         updateUi();
      }

      @Override
      public void onLtCaptchaSolved(boolean result, SolveCaptcha request) {
         _isSolving = false;
         if (result) {
            setResult(RESULT_OK);
            finish();
         } else {
            Toast.makeText(SolveCaptchaActivity.this, R.string.lt_captcha_wrong, Toast.LENGTH_LONG).show();
            updateUi();
         }
      }
   };

   private Integer getSolution() {
      try {
         return Integer.parseInt(_numberEntry.getEntry());
      } catch (NumberFormatException e) {
         return null;
      }
   }

   @Override
   public void onEntryChanged(String entry, boolean wasSet) {
      updateUi();
   }

}
