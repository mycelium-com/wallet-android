/*
 * Copyright 2013 Megion Research and Development GmbH
 *
 *  Licensed under the Microsoft Reference Source License (MS-RSL)
 *
 *  This license governs use of the accompanying software. If you use the software, you accept this license.
 *  If you do not accept the license, do not use the software.
 *
 *  1. Definitions
 *  The terms "reproduce," "reproduction," and "distribution" have the same meaning here as under U.S. copyright law.
 *  "You" means the licensee of the software.
 *  "Your company" means the company you worked for when you downloaded the software.
 *  "Reference use" means use of the software within your company as a reference, in read only form, for the sole purposes
 *  of debugging your products, maintaining your products, or enhancing the interoperability of your products with the
 *  software, and specifically excludes the right to distribute the software outside of your company.
 *  "Licensed patents" means any Licensor patent claims which read directly on the software as distributed by the Licensor
 *  under this license.
 *
 *  2. Grant of Rights
 *  (A) Copyright Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 *  worldwide, royalty-free copyright license to reproduce the software for reference use.
 *  (B) Patent Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 *  worldwide, royalty-free patent license under licensed patents for reference use.
 *
 *  3. Limitations
 *  (A) No Trademark License- This license does not grant you any rights to use the Licensor’s name, logo, or trademarks.
 *  (B) If you begin patent litigation against the Licensor over patents that you think may apply to the software
 *  (including a cross-claim or counterclaim in a lawsuit), your license to the software ends automatically.
 *  (C) The software is licensed "as-is." You bear the risk of using it. The Licensor gives no express warranties,
 *  guarantees or conditions. You may have additional consumer rights under your local laws which this license cannot
 *  change. To the extent permitted under your local laws, the Licensor excludes the implied warranties of merchantability,
 *  fitness for a particular purpose and non-infringement.
 *
 */

package com.mrd.mbw;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;

public class HintManager {

   private static final long TIME_BETWEEN_HINTS = 1000 * 60 * 2; // 2 minutes

   private Integer _currentHintIndex;
   private int[] _hints = new int[] { R.string.hint_1, R.string.hint_2, R.string.hint_3, R.string.hint_4,
         R.string.hint_5 };
   private Context _context;
   private MbwManager _mbwManager;
   private Long _lastHintTime;

   public HintManager(MbwManager mbwManager, Context applicationContext) {
      _mbwManager = mbwManager;
      _context = applicationContext;
      _currentHintIndex = null;
   }

   public String getNextHint() {
      int resourceId = _hints[getNextHintIndex()];
      return _context.getResources().getString(resourceId);
   }

   private synchronized int getNextHintIndex() {
      if (_currentHintIndex == null) {
         SharedPreferences preferences = _context.getSharedPreferences(Constants.SETTINGS_NAME, Activity.MODE_PRIVATE);
         _currentHintIndex = preferences.getInt(Constants.CURRENT_HINT_INDEX_SETTING, -1);
      }
      _currentHintIndex++;
      if (_currentHintIndex >= _hints.length) {
         _currentHintIndex = 0;
      }

      // Persist current hint index
      Editor editor = _context.getSharedPreferences(Constants.SETTINGS_NAME, Activity.MODE_PRIVATE).edit();
      editor.putInt(Constants.CURRENT_HINT_INDEX_SETTING, _currentHintIndex).commit();

      return _currentHintIndex;
   }

   public boolean timeForAHint() {
      if (!_mbwManager.getShowHints()) {
         return false;
      }
      if (_lastHintTime == null || _lastHintTime + TIME_BETWEEN_HINTS < System.currentTimeMillis()) {
         return true;
      }
      return false;
   }

   public AlertDialog showHint(final Context context) {

      // Reset the hint timer
      _lastHintTime = System.currentTimeMillis();

      // Create dialog
      LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
      final View layout = inflater.inflate(R.layout.hint_dialog, null);
      AlertDialog.Builder builder = new AlertDialog.Builder(context).setView(layout);
      final AlertDialog dialog = builder.create();

      // Set Hint
      final TextView tvHint = (TextView) layout.findViewById(R.id.tvHint);
      tvHint.setText(getNextHint());

      // Set title
      final TextView tvTitle = (TextView) layout.findViewById(R.id.tvTitle);
      tvTitle.setText(context.getResources().getString(R.string.hint_title, _currentHintIndex + 1, _hints.length));

      // Show Hints CheckBox
      final CheckBox cbShowHints = (CheckBox) layout.findViewById(R.id.cbShowHints);
      cbShowHints.setChecked(_mbwManager.getShowHints());
      cbShowHints.setOnCheckedChangeListener(new OnCheckedChangeListener() {

         @Override
         public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            _mbwManager.setShowHints(isChecked);
         }
      });

      // OK Button
      layout.findViewById(R.id.btOk).setOnClickListener(new OnClickListener() {

         @Override
         public void onClick(View v) {
            dialog.dismiss();
            // Reset the hint timer
            _lastHintTime = System.currentTimeMillis();
         }
      });

      // Next Button
      layout.findViewById(R.id.btNext).setOnClickListener(new OnClickListener() {

         @Override
         public void onClick(View v) {
            tvHint.setText(getNextHint());
            tvTitle.setText(context.getResources().getString(R.string.hint_title, _currentHintIndex + 1, _hints.length));
            // Reset the hint timer
            _lastHintTime = System.currentTimeMillis();
         }
      });
      dialog.show();
      return dialog;
   }
}
