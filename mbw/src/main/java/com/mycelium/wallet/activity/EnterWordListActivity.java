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
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;

import com.mrd.bitlib.crypto.Bip39;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.event.SeedFromWordsCreated;
import com.mycelium.wallet.persistence.MetadataStorage;
import com.mycelium.wapi.wallet.AesKeyCipher;
import com.mycelium.wapi.wallet.KeyCipher;
import com.squareup.otto.Bus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class EnterWordListActivity extends AppCompatActivity implements WordAutoCompleterFragment.WordAutoCompleterListener,
        AccountCreatorHelper.AccountCreationObserver {
   private static final String ONLY_SEED = "onlySeed";
   public static final String MASTERSEED = "masterseed";
   public static final String PASSWORD = "password";

   private boolean _seedOnly;

   public static void callMe(Activity activity, int requestCode) {
      Intent intent = new Intent(activity, EnterWordListActivity.class);
      intent.putExtra(ONLY_SEED, false);
      activity.startActivityForResult(intent, requestCode);
   }

   // only return the masterseed as string, dont try to create a new account based on it
   public static void callMe(Activity activity, int requestCode, boolean returnMasterseedOnly) {
      Intent intent = new Intent(activity, EnterWordListActivity.class);
      intent.putExtra(ONLY_SEED, returnMasterseedOnly);
      activity.startActivityForResult(intent, requestCode);
   }

   private MbwManager _mbwManager;
   private ProgressDialog _progress;
   private TextView enterWordInfo;
   private List<String> enteredWords;
   private boolean usesPassphrase;
   private int numberOfWords;
   private int currentWordNum;
   private WordAutoCompleterFragment _wordAutoCompleter;

   @Override
   public void onCreate(Bundle savedInstanceState) {
      this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
      super.onCreate(savedInstanceState);
      setContentView(R.layout.enter_word_list_activity);
      setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
      _mbwManager = MbwManager.getInstance(this);
      _progress = new ProgressDialog(this);
      enteredWords = new ArrayList<String>();
      enterWordInfo = (TextView) findViewById(R.id.tvEnterWord);
      findViewById(R.id.btDeleteLastWord).setOnClickListener(deleteListener);
      _wordAutoCompleter = (WordAutoCompleterFragment) getSupportFragmentManager().findFragmentById(R.id.wordAutoCompleter);
      _wordAutoCompleter.setListener(this);
      _wordAutoCompleter.setMinimumCompletionCharacters(2);
      _wordAutoCompleter.setCompletions(Bip39.ENGLISH_WORD_LIST);
      UsKeyboardFragment keyboard = (UsKeyboardFragment) getSupportFragmentManager().findFragmentById(R.id.usKeyboard);
      keyboard.setListener(_wordAutoCompleter);
      currentWordNum = 1;
      _seedOnly = getIntent().getBooleanExtra(ONLY_SEED, false);
      if (savedInstanceState == null) {
         //only ask if we are not recreating the activity, because of rotation for example
         askForWordNumber();
      }

      // we don't want to proceed to enter the wordlist, we already have the master seed.
      if (!_seedOnly && _mbwManager.getWalletManager(false).hasBip32MasterSeed()) {
         new AccountCreatorHelper.CreateAccountAsyncTask(EnterWordListActivity.this, EnterWordListActivity.this).execute();
      }
   }

   private void askForWordNumber() {
      final View checkBoxView = View.inflate(this, R.layout.wordlist_checkboxes, null);
      final CheckBox checkBox = (CheckBox) checkBoxView.findViewById(R.id.checkboxWordlistPassphrase);
      final RadioButton words12 = (RadioButton) checkBoxView.findViewById(R.id.wordlist12);
      final RadioButton words18 = (RadioButton) checkBoxView.findViewById(R.id.wordlist18);
      final RadioButton words24 = (RadioButton) checkBoxView.findViewById(R.id.wordlist24);

      checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
         @Override
         public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
            if (b) {
               checkBoxView.findViewById(R.id.tvPassphraseInfo).setVisibility(View.VISIBLE);
            } else {
               checkBoxView.findViewById(R.id.tvPassphraseInfo).setVisibility(View.GONE);
            }
         }
      });

      AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.MyceliumModern_Dialog);
      builder.setTitle(R.string.import_words_title);
      builder.setMessage(R.string.import_wordlist_questions)
            .setView(checkBoxView)
            .setCancelable(false)
            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
               public void onClick(DialogInterface dialog, int id) {
                  usesPassphrase = checkBox.isChecked();
                  if (words12.isChecked()) {
                     numberOfWords = 12;
                  } else if (words18.isChecked()) {
                     numberOfWords = 18;
                  } else if (words24.isChecked()) {
                     numberOfWords = 24;
                  } else {
                     throw new IllegalStateException("No radiobutton selected in word list import");
                  }
                  setHint();
               }
            })
            .show();
   }

   private View.OnClickListener deleteListener = new View.OnClickListener() {
      @Override
      public void onClick(View v) {
         enteredWords.remove(enteredWords.size() - 1);
         --currentWordNum;
         setHint();
         enterWordInfo.setText(enteredWords.toString());
         findViewById(R.id.tvChecksumWarning).setVisibility(View.GONE);
         if (currentWordNum == 1) {
            findViewById(R.id.btDeleteLastWord).setEnabled(false);
         }
      }
   };

   void setHint(){
      ((TextView)findViewById(R.id.tvHint)).setText(
            getString(R.string.importing_wordlist_enter_next_word, Integer.toString(currentWordNum), Integer.toString(numberOfWords))
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

   private void addWordToList(String selection) {
      enteredWords.add(selection);
      enterWordInfo.setText(enteredWords.toString());
      if (checkIfDone()) {
         askForPassphrase();
      } else {
         findViewById(R.id.btDeleteLastWord).setEnabled(true);
      }
   }

   private void askForPassphrase() {
      if (usesPassphrase) {
         View view = LayoutInflater.from(this).inflate(R.layout.layout_password, null);
         final EditText pass = view.findViewById(R.id.et_password);

         AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.MyceliumModern_Dialog);
         builder.setTitle(R.string.type_password_title);
         builder.setView(view)
               .setCancelable(false)
               .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                  public void onClick(DialogInterface dialog, int id) {
                     calculateSeed(pass.getText().toString());
                  }
               })
               .show();
      } else {
         calculateSeed("");
      }
   }

   private boolean checkIfDone() {
      if (currentWordNum < numberOfWords) {
         currentWordNum++;
         setHint();
         return false;
      }
      if (checksumMatches()) {
         return true;
      } else {
         findViewById(R.id.tvChecksumWarning).setVisibility(View.VISIBLE);
         setHint(false);
         currentWordNum++; //needed for the delete button to function correctly
         return false;
      }
   }

   private boolean checksumMatches() {
      return Bip39.isValidWordList(enteredWords.toArray(new String[enteredWords.size()]));
   }

   private void calculateSeed(String password) {
      if (_seedOnly){
         Intent result = new Intent();
         result.putStringArrayListExtra(MASTERSEED, new ArrayList<String>(enteredWords));
         result.putExtra(PASSWORD, password);
         setResult(RESULT_OK, result);
         finish();
      } else {
         _progress.setCancelable(false);
         _progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
         _progress.setMessage(getString(R.string.importing_master_seed_from_wordlist));
         _progress.show();
         new MasterSeedFromWordsAsyncTask(_mbwManager.getEventBus(), enteredWords, password).execute();
      }
   }

   @Override
   public void onWordSelected(String word) {
      addWordToList(word);
   }

   @Override
   public void onCurrentWordChanged(String currentWord) {
      ((TextView)findViewById(R.id.tvWord)).setText(currentWord);
   }

   private class MasterSeedFromWordsAsyncTask extends AsyncTask<Void, Integer, UUID> {
      private Bus bus;
      private List<String> wordList;
      private String password;

      MasterSeedFromWordsAsyncTask(Bus bus, List<String> wordList, String password) {
         this.bus = bus;
         this.wordList = wordList;
         this.password = password;
      }

      @Override
      protected UUID doInBackground(Void... params) {
         try {
            Bip39.MasterSeed masterSeed = Bip39.generateSeedFromWordList(wordList, password);
            _mbwManager.getWalletManager(false).configureBip32MasterSeed(masterSeed, AesKeyCipher.defaultKeyCipher());
            _mbwManager.getMetadataStorage().setMasterSeedBackupState(MetadataStorage.BackupState.VERIFIED);
            return _mbwManager.getWalletManager(false).createAdditionalBip44Account(AesKeyCipher.defaultKeyCipher());
         } catch (KeyCipher.InvalidKeyCipher invalidKeyCipher) {
            throw new RuntimeException(invalidKeyCipher);
         }
      }

      @Override
      protected void onPostExecute(UUID account) {
         bus.post(new SeedFromWordsCreated(account));
      }
   }

   @Override
   public void onAccountCreated(UUID accountid) {
      _progress.dismiss();
      finishOk(accountid);
   }

   @com.squareup.otto.Subscribe
   public void seedCreated(SeedFromWordsCreated event) {
      _progress.dismiss();
      finishOk(event.account);
   }

   @Override
   public void onResume() {
      _mbwManager.getEventBus().register(this);
      super.onResume();
   }

   @Override
   public void onPause() {
      _progress.dismiss();
      _mbwManager.getEventBus().unregister(this);
      super.onPause();
   }

   private void finishOk(UUID account) {
      Intent result = new Intent();
      result.putExtra(AddAccountActivity.RESULT_KEY, account);
      setResult(RESULT_OK, result);
      finish();
   }


   @Override
   public void onSaveInstanceState(Bundle savedInstanceState) {
      super.onSaveInstanceState(savedInstanceState);
      savedInstanceState.putBoolean("usepass", usesPassphrase);
      savedInstanceState.putInt("index", currentWordNum);
      savedInstanceState.putInt("total", numberOfWords);
      savedInstanceState.putStringArray("entered", enteredWords.toArray(new String[enteredWords.size()]));
   }

   @Override
   public void onRestoreInstanceState(Bundle savedInstanceState) {
      super.onRestoreInstanceState(savedInstanceState);
      enteredWords = new ArrayList<String>(Arrays.asList(savedInstanceState.getStringArray("entered")));
      enterWordInfo.setText(enteredWords.toString());
      usesPassphrase = savedInstanceState.getBoolean("usepass");
      numberOfWords = savedInstanceState.getInt("total");
      currentWordNum = savedInstanceState.getInt("index");
      findViewById(R.id.btDeleteLastWord).setEnabled(currentWordNum > 1);
      if (currentWordNum < numberOfWords) {
         setHint();
      } else if (!checksumMatches()) {
         findViewById(R.id.tvChecksumWarning).setVisibility(View.VISIBLE);
         setHint(false);
      }
   }
}
