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
import com.google.common.base.Optional;
import com.mrd.bitlib.crypto.Bip39;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.persistence.MetadataStorage;
import com.mycelium.wapi.wallet.AesKeyCipher;
import com.mycelium.wapi.wallet.KeyCipher;

import java.util.List;

public class VerifyWordListActivity extends AppCompatActivity implements WordAutoCompleterFragment.WordAutoCompleterListener {
   private MbwManager _mbwManager;
   private List<String> wordlist;
   private String passphrase;
   private int currentWordIndex;

   public static void callMe(Activity activity) {
      Intent intent = new Intent(activity, VerifyWordListActivity.class);
      activity.startActivity(intent);
   }

   @Override
   public void onCreate(Bundle savedInstanceState) {
      this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
      this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
      super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_verify_words);
      Utils.preventScreenshots(this);
      _mbwManager = MbwManager.getInstance(this);
      Bip39.MasterSeed masterSeed;
      try {
         masterSeed = _mbwManager.getWalletManager(false).getMasterSeed(AesKeyCipher.defaultKeyCipher());
      } catch (KeyCipher.InvalidKeyCipher invalidKeyCipher) {
         throw new RuntimeException(invalidKeyCipher);
      }

      wordlist = masterSeed.getBip39WordList();
      passphrase = masterSeed.getBip39Passphrase();
      currentWordIndex = 0;

      WordAutoCompleterFragment wordAutoCompleter = (WordAutoCompleterFragment) getSupportFragmentManager().findFragmentById(R.id.wordAutoCompleter);
      wordAutoCompleter.setListener(this);
      wordAutoCompleter.setMinimumCompletionCharacters(2);
      wordAutoCompleter.setCompletions(Bip39.ENGLISH_WORD_LIST);
      UsKeyboardFragment keyboard = (UsKeyboardFragment) getSupportFragmentManager().findFragmentById(R.id.usKeyboard);
      keyboard.setListener(wordAutoCompleter);
      setHint();
   }

   private void askForNextWord() {
      if (currentWordIndex == wordlist.size() - 1) {
         //we are done, final steps
         askForPassword(false);
      } else {
         //ask for next word
         currentWordIndex++;
         setHint();
      }
   }

   private void askForPassword(boolean wasWrong) {
      if (passphrase.length() > 0) {
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
                     if (pass.getText().toString().equals(passphrase)) {
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
      _mbwManager.getMetadataStorage().setMasterSeedBackupState(MetadataStorage.BackupState.VERIFIED);

      if (!_mbwManager.isPinProtected()) {
         AlertDialog.Builder builder = new AlertDialog.Builder(this);
         builder
               .setMessage(R.string.verify_wordlist_success)
               .setCancelable(false)
               .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                  public void onClick(DialogInterface dialog, int id) {
                     VerifyWordListActivity.this.finish();
                  }
               })
               .setNeutralButton(R.string.pref_set_pin, new DialogInterface.OnClickListener() {
                  public void onClick(DialogInterface dialog, int id) {
                     _mbwManager.showSetPinDialog(VerifyWordListActivity.this, Optional.<Runnable>of(new Runnable() {
                        @Override
                        public void run() {
                           // close this activity after the PIN code dialog was closed
                           VerifyWordListActivity.this.finish();
                        }
                     }));
                  }
               });
         AlertDialog alertDialog = builder.create();
         alertDialog.show();
      }else {
         Utils.showSimpleMessageDialog(this, R.string.verify_wordlist_success, new Runnable() {
            @Override
            public void run() {
               VerifyWordListActivity.this.finish();
            }
         });
      }
   }

   @Override
   public void onSaveInstanceState(Bundle savedInstanceState) {
      super.onSaveInstanceState(savedInstanceState);
      savedInstanceState.putInt("index", currentWordIndex);
   }

   void setHint(){
      ((TextView)findViewById(R.id.tvHint)).setText(
            getString(R.string.importing_wordlist_enter_next_word,  Integer.toString(currentWordIndex + 1), Integer.toString(wordlist.size()))
      );
      findViewById(R.id.tvHint).setVisibility(View.VISIBLE);
   }

   void setHint(boolean show){
      if (show){
         setHint();
      }else{
         findViewById(R.id.tvHint).setVisibility(View.INVISIBLE);
      }
   }

   @Override
   public void onRestoreInstanceState(Bundle savedInstanceState) {
      super.onRestoreInstanceState(savedInstanceState);
      currentWordIndex = savedInstanceState.getInt("index");
      setHint();
   }

   @Override
   public void onWordSelected(String word) {
      if (word.equals(wordlist.get(currentWordIndex))) {
         askForNextWord();
      } else {
         Toast.makeText(VerifyWordListActivity.this, R.string.verify_word_wrong, Toast.LENGTH_LONG).show();
      }
   }

   @Override
   public void onCurrentWordChanged(String currentWord) {
      ((TextView)findViewById(R.id.tvWord)).setText(currentWord);
   }
}
