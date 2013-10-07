package com.mycelium.wallet;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Vibrator;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
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
   }

   public static void show(final Context context, int titleResourceId, String hintText,
         final String currentText, final EnterTextHandler enterNameHandler) {
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
      et.selectAll();
      InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
      imm.showSoftInput(et, InputMethodManager.SHOW_IMPLICIT);
      dialog.show();
   }
}
