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
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.*;
import com.mrd.bitlib.crypto.Bip39;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wapi.wallet.AesKeyCipher;
import com.mycelium.wapi.wallet.KeyCipher;

import java.util.List;

public class BackupWordListActivity extends AppCompatActivity {
   private Button btnNextWord;
   private TextView tvShowWord;
   private TextView tvShowWordNumber;
   private List<String> wordlist;
   private String passphrase;
   private int currentWordIndex;

   public static void callMe(Activity activity) {
      Intent intent = new Intent(activity, BackupWordListActivity.class);
      activity.startActivity(intent);
   }

   @Override
   public void onCreate(Bundle savedInstanceState) {
      this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
      this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
      super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_backup_words);
      Utils.preventScreenshots(this);
      MbwManager _mbwManager = MbwManager.getInstance(this);
      Bip39.MasterSeed masterSeed;
      try {
         masterSeed = _mbwManager.getWalletManager(false).getMasterSeed(AesKeyCipher.defaultKeyCipher());
      } catch (KeyCipher.InvalidKeyCipher invalidKeyCipher) {
         throw new RuntimeException(invalidKeyCipher);
      }

      wordlist = masterSeed.getBip39WordList();
      passphrase = masterSeed.getBip39Passphrase();
      currentWordIndex = 0;

      btnNextWord = (Button)findViewById(R.id.btOkay);
      btnNextWord.setOnClickListener(nextListener);

      tvShowWordNumber = (TextView)findViewById(R.id.tvShowWordNumber);
      tvShowWord = (TextView)findViewById(R.id.tvShowWord);

      AutoCompleteTextView acTextView = (AutoCompleteTextView) findViewById(R.id.tvWordCompleter);
      acTextView.setLongClickable(false);
   }

   private View.OnClickListener nextListener = new View.OnClickListener() {
      @Override
      public void onClick(View v) {
         if (currentWordIndex == wordlist.size()) {
            switchToVerify();
         } else {
            if (currentWordIndex == 0) findViewById(R.id.tvClickButtonToShowFirstWord).setVisibility(View.GONE);
            showWordNumber();
         }
      }
   };

   private void showWordNumber() {
      if (currentWordIndex == wordlist.size()-1) {
         btnNextWord.setText(R.string.start_word_list_verification);
      }else {
         btnNextWord.setText(R.string.next_word_button);
      }
      tvShowWordNumber.setText(getString(R.string.showing_word_number, Integer.toString(currentWordIndex + 1)));
      tvShowWord.setText(wordlist.get(currentWordIndex));
      currentWordIndex++;
   }

   private void switchToVerify() {
      //check whether we need to show a password
      if (passphrase.length() == 0) {
         startVerification();
      } else {
         final TextView pass = new TextView(this);
         pass.setText(passphrase);
         AlertDialog.Builder builder = new AlertDialog.Builder(this);
         builder.setTitle(R.string.note_down_password_title);
         builder.setView(pass)
               .setCancelable(false)
               .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                  public void onClick(DialogInterface dialog, int id) {
                     startVerification();
                  }
               })
               .show();
      }
   }

   private void startVerification() {
      VerifyWordListActivity.callMe(this);
      finish();
   }

   @Override
   public void onSaveInstanceState(Bundle savedInstanceState) {
      super.onSaveInstanceState(savedInstanceState);
      savedInstanceState.putInt("index", currentWordIndex);
   }

   @Override
   public void onRestoreInstanceState(Bundle savedInstanceState)
   {
      super.onRestoreInstanceState(savedInstanceState);
      currentWordIndex = savedInstanceState.getInt("index");
      if (currentWordIndex != 0) {
         currentWordIndex--;
         findViewById(R.id.tvClickButtonToShowFirstWord).setVisibility(View.GONE);
      }
      showWordNumber();
   }
}
