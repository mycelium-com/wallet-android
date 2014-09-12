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
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.InputType;
import android.view.View;
import android.view.WindowManager;
import android.widget.*;
import com.google.common.collect.Lists;
import com.mrd.bitlib.crypto.Bip39;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.persistence.MetadataStorage;
import com.mycelium.wapi.wallet.AesKeyCipher;
import com.mycelium.wapi.wallet.KeyCipher;

import java.util.List;

public class BackupWordListActivity extends ActionBarActivity {


   public static void callMe(Activity activity) {
      Intent intent = new Intent(activity, BackupWordListActivity.class);
      activity.startActivity(intent);
   }

   private MbwManager _mbwManager;
   private AutoCompleteTextView acTextView;
   private List<String> wordlist;
   private String password;
   private int currentWordIndex;
   private boolean startedVerification = false;

   @Override
   public void onCreate(Bundle savedInstanceState) {
      this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
      this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
      super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_backup_words);
      Utils.preventScreenshots(this);
      _mbwManager = MbwManager.getInstance(this);
      Bip39.MasterSeed masterSeed;
      try {
         masterSeed = _mbwManager.getWalletManager(false).getMasterSeed(AesKeyCipher.defaultKeyCipher());
      } catch (KeyCipher.InvalidKeyCipher invalidKeyCipher) {
         throw new RuntimeException(invalidKeyCipher);
      }
      wordlist = masterSeed.getBip39WordList();
      password = masterSeed.getBip39Password();
      currentWordIndex = 0;

      findViewById(R.id.btNextWord).setOnClickListener(nextListener);

      acTextView = (AutoCompleteTextView) findViewById(R.id.tvWordCompleter);
      acTextView.setOnItemClickListener(itemClicked);
      acTextView.setLongClickable(false);

      //first we just show
      acTextView.setEnabled(false);
      acTextView.setHint(R.string.cick_button_to_show_word);
   }

   private View.OnClickListener nextListener = new View.OnClickListener() {
      @Override
      public void onClick(View v) {
         if (currentWordIndex == wordlist.size()) {
            switchToVerify();
         } else {
            acTextView.setText(getString(R.string.showing_word_number, currentWordIndex + 1, wordlist.get(currentWordIndex)));
            currentWordIndex++;
         }
      }
   };

   private void switchToVerify() {
      startedVerification = true;
      currentWordIndex = 0;
      checkForPasswordToShow();
   }

   private void checkForPasswordToShow() {
      if (password.length() == 0) {
         startVerification();
      } else {
         final TextView pass = new TextView(this);
         pass.setText(password);
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
      findViewById(R.id.tvDescriptionWordlistBackup).setVisibility(View.GONE);
      findViewById(R.id.tvDescriptionWordlistVerify).setVisibility(View.VISIBLE);

      // Change the title.
      ((TextView) findViewById(R.id.tvTitle)).setText(R.string.verify_words_title);
      List<String> words = Lists.newArrayList(Bip39.ENGLISH_WORD_LIST);
      ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, words);
      acTextView.setInputType(InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE);
      acTextView.setThreshold(1);
      acTextView.setAdapter(adapter);
      acTextView.setEnabled(true);
      acTextView.setText("");
      acTextView.setHint(getString(R.string.enter_next_word, currentWordIndex + 1, wordlist.size()));
      findViewById(R.id.btNextWord).setVisibility(View.GONE);
   }

   AdapterView.OnItemClickListener itemClicked = new AdapterView.OnItemClickListener() {

      @Override
      public void onItemClick(AdapterView<?> parent, View view, int position, long rowId) {
         String selection = (String) parent.getItemAtPosition(position);
         if (!selection.equals(wordlist.get(currentWordIndex))) {
            Toast.makeText(BackupWordListActivity.this, R.string.verify_word_wrong, Toast.LENGTH_LONG).show();
         } else {
            if (currentWordIndex == wordlist.size() - 1) {
               //we are done, final steps
               askForPassword(false);
            } else {
               //ask for next word
               currentWordIndex++;
               acTextView.setHint(getString(R.string.enter_next_word, currentWordIndex + 1, wordlist.size()));
            }
         }
         acTextView.setText("");
      }
   };

   private void askForPassword(boolean wasWrong) {
      if (password.length() > 0) {
         final EditText pass = new EditText(this);

         final AlertDialog.Builder builder = new AlertDialog.Builder(this);
         if (wasWrong) {
            builder.setTitle(R.string.title_wrong_password);
         } else {
            builder.setTitle(R.string.type_password_title);
         }
         builder.setView(pass)
               .setCancelable(false)
               .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                  public void onClick(DialogInterface dialog, int id) {
                     if (pass.getText().toString().equals(password)) {
                        setVerified();
                     } else {
                        askForPassword(true);
                     }
                  }
               })
               .show();
      } else {
         setVerified();
      }
   }

   private void setVerified() {
      _mbwManager.getMetadataStorage().setMasterKeyBackupState(MetadataStorage.BackupState.VERIFIED);
      Utils.showSimpleMessageDialog(this, R.string.verify_wordlist_success, new Runnable() {
         @Override
         public void run() {
            BackupWordListActivity.this.finish();
         }
      });
   }

   @Override
   public void onSaveInstanceState(Bundle savedInstanceState)
   {
      super.onSaveInstanceState(savedInstanceState);
      savedInstanceState.putBoolean("verifying", startedVerification);
      savedInstanceState.putInt("index", currentWordIndex);
   }

   @Override
   public void onRestoreInstanceState(Bundle savedInstanceState)
   {
      super.onRestoreInstanceState(savedInstanceState);
      currentWordIndex = savedInstanceState.getInt("index");
      startedVerification = savedInstanceState.getBoolean("verifying");
      if (startedVerification) { startVerification(); }
   }
}
