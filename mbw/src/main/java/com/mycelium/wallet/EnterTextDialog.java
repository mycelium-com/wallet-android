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

package com.mycelium.wallet;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Handler;
import android.os.Vibrator;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class EnterTextDialog {

   public abstract static class EnterTextHandler implements Runnable {

      private String _newText;
      private String _oldText;

      protected EnterTextHandler() {
      }

      /**
       * Simple default validator that makes sure we don't accept empty strings
       */
      public boolean validateTextOnChange(String newText, String oldText) {
         return newText.length() != 0;
      }

      /**
       * Simple default validator that makes sure we don't accept empty strings
       */
      public boolean validateTextOnOk(String newText, String oldText) {
         return newText.length() != 0;
      }

      /**
       * Get the text to toast when OK validation failed.
       */
      public String getToastTextOnInvalidOk(String newText, String oldText) {
         return null;
      }

      /**
       * Vibrate when OK validation failed?
       */
      public boolean getVibrateOnInvalidOk(String newText, String oldText) {
         return true;
      }

      /**
       * Called when the text was changed and the user clicked OK
       */
      public abstract void onNameEntered(String newText, String oldText);

      @Override
      public final void run() {
         onNameEntered(_newText, _oldText);
      }

      public final void setResult(String newText, String oldText) {
         _newText = newText;
         _oldText = oldText;
      }
      public void onDismiss() {

      }
   }

   public static void show(final Context context, int titleResourceId, String hintText, final String currentText,
         boolean singleLine, final EnterTextHandler enterNameHandler) {
      final Handler postHandler = new Handler();

      LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
      final View layout = inflater.inflate(R.layout.enter_text_dialog, null);
      AlertDialog.Builder builder = new AlertDialog.Builder(context).setView(layout);
      final AlertDialog dialog = builder.create();
      ((TextView) layout.findViewById(R.id.tvTitle)).setText(titleResourceId);
      final EditText et = (EditText) layout.findViewById(R.id.etLabel);
      if (hintText != null) {
         et.setHint(hintText);
      }
      et.setSingleLine(singleLine);
      final Button btOk = (Button) layout.findViewById(R.id.btOk);
      et.addTextChangedListener(new TextWatcher() {

         @Override
         public void onTextChanged(CharSequence s, int start, int before, int count) {
         }

         @Override
         public void beforeTextChanged(CharSequence s, int start, int count, int after) {
         }

         @Override
         public void afterTextChanged(Editable s) {
            btOk.setEnabled(enterNameHandler.validateTextOnChange(s.toString().trim(), currentText));
         }
      });
      btOk.setOnClickListener(new OnClickListener() {

         @Override
         public void onClick(View v) {
            EditText et = (EditText) layout.findViewById(R.id.etLabel);
            String text = et.getText().toString().trim();
            if (enterNameHandler.validateTextOnOk(text, currentText)) {
               dialog.dismiss();
               enterNameHandler.setResult(text, currentText);
               postHandler.post(enterNameHandler);
            } else {
               if (enterNameHandler.getVibrateOnInvalidOk(text, currentText)) {
                  Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
                  if (vibrator != null) {
                     vibrator.vibrate(500);
                  }
               }
               String toasText = enterNameHandler.getToastTextOnInvalidOk(text, currentText);
               if (toasText != null) {
                  Toast.makeText(context, toasText, Toast.LENGTH_LONG).show();
               }
            }
         }
      });
      layout.findViewById(R.id.btCancel).setOnClickListener(new OnClickListener() {

         @Override
         public void onClick(View v) {
            dialog.dismiss();
         }
      });
      et.setText(currentText == null ? "" : currentText);

      // Some older devices need this to show the keyboard
      if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
         InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
         if (imm != null) {
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
         }
         et.setSelection(et.getText().length());

         // Hide the keyboard, when the dialog exits
         dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
               InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
               if (imm != null) {
                  imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
               }
               if(enterNameHandler != null) {
                  enterNameHandler.onDismiss();
               }
            }
         });

      }else{
         // For > Android 4.x set the input-mode for the dialog-box, so that the keyboard pops up
         dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
         et.setSelectAllOnFocus(true);
         dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
               if(enterNameHandler != null) {
                  enterNameHandler.onDismiss();
               }
            }
         });
      }


      dialog.show();

      et.requestFocus();

   }
}
