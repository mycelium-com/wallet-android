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
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import com.mycelium.wallet.Constants;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;

import java.util.Timer;
import java.util.TimerTask;

public class AdditionalBackupWarningActivity extends AppCompatActivity {
   private Button btnImFine;
   private volatile int countdown;
   private Timer countdownTimer;


   public static void callMe(Activity activity) {
      Intent intent = new Intent(activity, AdditionalBackupWarningActivity.class);
      activity.startActivity(intent);
   }

   @Override
   public void onCreate(Bundle savedInstanceState) {
      this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
      this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
      super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_additional_backup_warning);

      MbwManager mbwManager = MbwManager.getInstance(this);
      btnImFine = (Button) findViewById(R.id.btOkayImFine);

      if (!mbwManager.isPinProtected()){
         // no pin protection - deny additional backup
         TextView txtView = (TextView) findViewById(R.id.tvDescriptionAdditionalNotPossibleNoPin);
         txtView.setVisibility(View.VISIBLE);

         btnImFine.setOnClickListener(backListener);
         btnImFine.setEnabled(true);

      }else if (!mbwManager.isPinOldEnough()){
         // PIN is not old enough. if you haven't set a PIN, this gives
         // you at least some time, until someone can extract the masterseed
         TextView txtView = (TextView) findViewById(R.id.tvDescriptionAdditionalNotPossiblePinTooNew);
         txtView.setVisibility(View.VISIBLE);

         // Show warning, that you have to wait for n Blocks
         Integer remainingPinLockdownDuration = mbwManager.getRemainingPinLockdownDuration().or(Constants.MIN_PIN_BLOCKHEIGHT_AGE_ADDITIONAL_BACKUP);
         String approximateDuration = Utils.formatBlockcountAsApproxDuration(this, remainingPinLockdownDuration);

         txtView.setText(String.format(this.getApplicationContext().getString(R.string.wordlist_additional_backup_not_possible_pin_too_new),
               remainingPinLockdownDuration,
               approximateDuration));

         btnImFine.setOnClickListener(backListener);
         btnImFine.setEnabled(true);
      }else {
         findViewById(R.id.tvDescriptionAdditionalWordlistBackup).setVisibility(View.VISIBLE);
         countdown = Constants.WAIT_SECONDS_BEFORE_ADDITIONAL_BACKUP;
         btnImFine.setOnClickListener(okayListener);
         startCountdown();
      }
   }

   @Override
   public void onBackPressed() {
      if(countdownTimer!=null){
         countdownTimer.cancel();
         countdownTimer = null;
      }
      super.onBackPressed();
   }

   private View.OnClickListener backListener = new View.OnClickListener() {
      @Override
      public void onClick(View v) {
         finish();
      }
   };

   private View.OnClickListener okayListener = new View.OnClickListener() {
      @Override
      public void onClick(View v) {
         Intent intent = new Intent(AdditionalBackupWarningActivity.this, BackupWordListActivity.class);
         AdditionalBackupWarningActivity.this.startActivity(intent);
         finish();
      }
   };

   private void startCountdown(){
      countdownTimer = new Timer();
      final String buttonOkayDefaultText = this.getApplicationContext().getString(R.string.wordlist_start_with_additional_wordlist_backup);

      countdownTimer.scheduleAtFixedRate(new TimerTask() {

         @Override
         public void run() {
            countdown--;
            AdditionalBackupWarningActivity.this.runOnUiThread(new Runnable() {
               @Override
               public void run() {
                  if (countdown > 0) {
                     btnImFine.setText(String.format(buttonOkayDefaultText + " (%d)", countdown));
                  } else {
                     countdownTimer.cancel();
                     btnImFine.setText(buttonOkayDefaultText);
                     btnImFine.setEnabled(true);
                  }
               }
            });
         }
      }, 0, 1000);
   }

   @Override
   public void onSaveInstanceState(Bundle savedInstanceState)
   {
      super.onSaveInstanceState(savedInstanceState);
      savedInstanceState.putInt("cnt", countdown);
   }

   @Override
   public void onRestoreInstanceState(Bundle savedInstanceState)
   {
      super.onRestoreInstanceState(savedInstanceState);
      countdown = savedInstanceState.getInt("cnt");
   }
}
