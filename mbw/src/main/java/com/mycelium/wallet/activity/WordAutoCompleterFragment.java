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
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.google.common.base.Preconditions;
import com.mycelium.wallet.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class WordAutoCompleterFragment extends Fragment implements UsKeyboardFragment.UsKeyboardListener {

   private WordAutoCompleterListener _listener;
   private String _currentWord;
   private List<Button> _completionButtons;
   private String[] _completions;
   private int _minimumCharacters;

   public interface WordAutoCompleterListener {
      void onWordSelected(String word);
      void onCurrentWordChanged(String currentWord);
   }

   @Override
   public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
      View root = Preconditions.checkNotNull(inflater.inflate(R.layout.word_auto_completer_fragment, container, false));
      _currentWord = "";
      _completionButtons = new ArrayList<Button>();
      _completionButtons.add((Button) root.findViewById(R.id.btSuggestion1));
      _completionButtons.add((Button) root.findViewById(R.id.btSuggestion2));
      _completionButtons.add((Button) root.findViewById(R.id.btSuggestion3));

      for (Button b : _completionButtons) {
         b.setOnClickListener(completionClickListener);
      }
      return root;
   }

   View.OnClickListener completionClickListener = new View.OnClickListener() {

      @Override
      public void onClick(View view) {
         acceptWord(((Button) view).getText().toString());
      }
   };

   private void acceptWord(String word) {
      if (!WordAutoCompleterFragment.this.isAdded() || _listener == null) {
         return;
      }
      _listener.onWordSelected(word);

      // prepare for next word
      _currentWord="";
      setCurrentWord(_currentWord);
      // hide buttons
      showCompletionButtons();
   }

   private void setCurrentWord(String word) {
      _currentWord = word;
      _listener.onCurrentWordChanged(word);

      if ( exactMatch(word) ){
         // exact match
         acceptWord(_currentWord);

      }
   }

   private boolean exactMatch(String entered){
      // check if the word matches one entry in the wordlist exactly
      if (Arrays.asList(_completions).contains(entered)){
         // check if there is no other word starting with the same letters (eg. "sea" / "seat")
         for (String w : _completions){
            if (!w.equals(entered) && w.startsWith(entered)){
               return false;
            }
         }
         return true;
      }else{
         return false;
      }

   }

   public void setCompletions(String[] completions) {
      _completions = completions.clone();
      showCompletionButtons();
   }

   public void setMinimumCompletionCharacters(int minimumCharacters) {
      _minimumCharacters = minimumCharacters;
   }

   public void setListener(WordAutoCompleterListener listener) {
      _listener = listener;
   }

   @Override
   public void onCharacterKeyClicked(char character) {
      setCurrentWord(_currentWord + character);
      showCompletionButtons();
   }

   @Override
   public void onDelClicked() {
      if (_currentWord.length() > 0) {
         setCurrentWord(_currentWord.substring(0, _currentWord.length() - 1));
         showCompletionButtons();
      }
   }

   private void showCompletionButtons() {
      // Make the first button invisible, the rest disappear
      boolean first = true;
      for (Button b : _completionButtons) {
         if (first) {
            first = false;
            b.setVisibility(View.INVISIBLE);
         } else {
            b.setVisibility(View.GONE);
         }
         b.setText("");
      }

      List<String> completions = determineCompletions(_currentWord, _completionButtons.size());

      for (int i = 0; i < completions.size(); i++) {
         Button b = _completionButtons.get(i);
         String word = completions.get(i);
         b.setVisibility(View.VISIBLE);
         b.setText(word);
      }

   }

   private List<String> determineCompletions(String partialWord, int maxCompletions) {
      List<String> completions = new ArrayList<String>(maxCompletions);
      if (_completions == null || partialWord.length() < _minimumCharacters) {
         return completions;
      }
      for (String s : _completions) {
         if (s.startsWith(partialWord)) {
            completions.add(s);
            if (completions.size() == maxCompletions) {
               break;
            }
         }
      }
      return completions;
   }

   @Override
   public void onCreate(Bundle savedInstanceState) {
      setRetainInstance(true);
      super.onCreate(savedInstanceState);
   }

   @Override
   public void onAttach(Activity activity) {
      super.onAttach(activity);
   }

   @Override
   public void onResume() {
      showCompletionButtons();
      super.onResume();
   }

   @Override
   public void onPause() {
      super.onPause();
   }

}
