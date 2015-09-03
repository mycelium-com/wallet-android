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

import android.app.DialogFragment;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import com.mycelium.wallet.R;
import com.mycelium.wallet.activity.util.MasterseedPasswordSetter;

public class MasterseedPasswordDialog extends DialogFragment {

   @Override
   public void onActivityCreated(Bundle savedInstanceState) {
      super.onActivityCreated(savedInstanceState);
      if (savedInstanceState != null) {
         ((EditText) getDialog().findViewById(R.id.etPassphrase)).setText(savedInstanceState.getString("pwd"));
      }
   }

   @Override
   public void onSaveInstanceState(@NonNull Bundle outState) {
      super.onSaveInstanceState(outState);
      outState.putString("pwd", ((EditText) getDialog().findViewById(R.id.etPassphrase)).getText().toString());
   }

   @Override
   public View onCreateView(LayoutInflater inflater, ViewGroup container,
                            Bundle savedInstanceState) {

      getDialog().setTitle("Passphrase");
      getDialog().setCanceledOnTouchOutside(false);
      final View v = inflater.inflate(R.layout.passphrase_dialog, container, false);

      // show/hide password
      ((CheckBox)v.findViewById(R.id.cbShowPassword)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
         @Override
         public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
            EditText etPassword = (EditText) v.findViewById(R.id.etPassphrase);

            etPassword.setInputType(InputType.TYPE_CLASS_TEXT | (b ? InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD : InputType.TYPE_TEXT_VARIATION_PASSWORD));
            // Set cursor to last position
            etPassword.setSelection(etPassword.getText().length());
         }
      });

      // Okay button
      v.findViewById(R.id.btnOkay).setOnClickListener(new View.OnClickListener() {
         @Override
         public void onClick(View view) {
            String text = ((EditText) v.findViewById(R.id.etPassphrase)).getText().toString();
            ((MasterseedPasswordSetter)getActivity()).setPassphrase(text);
         }
      });

      // Cancel button
      v.findViewById(R.id.btnCancel).setOnClickListener(new View.OnClickListener() {
         @Override
         public void onClick(View view) {
            ((MasterseedPasswordSetter)getActivity()).setPassphrase(null);
         }
      });

      return v;
   }

}