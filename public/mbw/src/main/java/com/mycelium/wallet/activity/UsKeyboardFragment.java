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
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import com.google.common.base.Preconditions;
import com.mycelium.wallet.R;

import java.util.Locale;

public class UsKeyboardFragment extends Fragment {

   private UsKeyboardListener _listener;

   public interface UsKeyboardListener {
      public void onCharacterKeyClicked(char character);

      public void onDelClicked();
   }

   @Override
   public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
      View root = Preconditions.checkNotNull(inflater.inflate(R.layout.us_keyboard_fragment, container, false));

      // Hook up top row
      HookUpLetterKey(root, R.id.btQ);
      HookUpLetterKey(root, R.id.btW);
      HookUpLetterKey(root, R.id.btE);
      HookUpLetterKey(root, R.id.btR);
      HookUpLetterKey(root, R.id.btT);
      HookUpLetterKey(root, R.id.btY);
      HookUpLetterKey(root, R.id.btU);
      HookUpLetterKey(root, R.id.btI);
      HookUpLetterKey(root, R.id.btO);
      HookUpLetterKey(root, R.id.btP);

      // Hook up middle row
      HookUpLetterKey(root, R.id.btA);
      HookUpLetterKey(root, R.id.btS);
      HookUpLetterKey(root, R.id.btD);
      HookUpLetterKey(root, R.id.btF);
      HookUpLetterKey(root, R.id.btG);
      HookUpLetterKey(root, R.id.btH);
      HookUpLetterKey(root, R.id.btJ);
      HookUpLetterKey(root, R.id.btK);
      HookUpLetterKey(root, R.id.btL);

      // Hook up bottom row
      HookUpLetterKey(root, R.id.btZ);
      HookUpLetterKey(root, R.id.btX);
      HookUpLetterKey(root, R.id.btC);
      HookUpLetterKey(root, R.id.btV);
      HookUpLetterKey(root, R.id.btB);
      HookUpLetterKey(root, R.id.btN);
      HookUpLetterKey(root, R.id.btM);
      HookUpDelKey(root, R.id.btDel);

      return root;
   }

   public void setListener(UsKeyboardListener listener) {
      _listener = listener;
   }

   private void HookUpLetterKey(View root, int buttonId) {
      Button b = (Button) root.findViewById(buttonId);
      b.setOnClickListener(aToZClickListener);
   }

   private void HookUpDelKey(View root, int buttonId) {
      Button b = (Button) root.findViewById(buttonId);
      b.setOnClickListener(delClickListener);
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
      super.onResume();
   }

   OnClickListener aToZClickListener = new OnClickListener() {

      @Override
      public void onClick(View view) {
         if (!UsKeyboardFragment.this.isAdded() || _listener == null) {
            return;
         }
         Button b = (Button) view;
         _listener.onCharacterKeyClicked(b.getText().toString().toLowerCase(Locale.US).charAt(0));
      }
   };

   OnClickListener delClickListener = new OnClickListener() {

      @Override
      public void onClick(View view) {
         if (!UsKeyboardFragment.this.isAdded() || _listener == null) {
            return;
         }
         _listener.onDelClicked();
      }
   };

   @Override
   public void onPause() {
      super.onPause();
   }

}
