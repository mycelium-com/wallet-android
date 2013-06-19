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

package com.mycelium.wallet.activity.export;

import java.util.List;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;

import com.mycelium.wallet.ExternalStorageManager;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Record;

public class ExportToExternalStorageActivity extends Activity {

   private MbwManager _mbwManager;
   private ExportTask _exportTask;

   /** Called when the activity is first created. */
   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.export_to_external_storage);

      _mbwManager = MbwManager.getInstance(this.getApplication());

      findViewById(R.id.btExport).setOnClickListener(new OnClickListener() {

         @Override
         public void onClick(View arg0) {
            findViewById(R.id.rbJpeg).setEnabled(false);
            findViewById(R.id.rbPng).setEnabled(false);
            findViewById(R.id.btExport).setEnabled(false);
            findViewById(R.id.pbSpinner).setVisibility(View.VISIBLE);
            TextView status = (TextView) findViewById(R.id.tvStatus);
            status.setText(getResources().getString(R.string.exporting));
            boolean usePng = ((RadioButton) findViewById(R.id.rbPng)).isChecked();
            _exportTask = new ExportTask(_mbwManager.getRecordManager().getSelectedRecord(), usePng);
            _exportTask.execute(new Void[] {});
         }

      });

      findViewById(R.id.pbSpinner).setVisibility(View.INVISIBLE);
      ExternalStorageManager ext = _mbwManager.getExternalStorageManager();
      boolean mounted = ext.hasExternalStorage();
      List<String> paths = ext.listPotentialExternalSdCardPaths();

      TextView tvStorageLocation = (TextView) findViewById(R.id.tvStatus);
      Spinner spDirectory = (Spinner) findViewById(R.id.spDirectory);

      if (mounted && !paths.isEmpty()) {
         spDirectory.setOnItemSelectedListener(directorySelected);
         ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, paths);
         adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
         spDirectory.setAdapter(adapter);
         findViewById(R.id.btExport).setEnabled(true);
      } else {
         tvStorageLocation.setText(getResources().getString(R.string.no_external_storage_found));
         spDirectory.setVisibility(View.GONE);
         findViewById(R.id.btExport).setEnabled(false);
      }

   }

   final OnItemSelectedListener directorySelected = new OnItemSelectedListener() {

      @Override
      public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
      }

      @Override
      public void onNothingSelected(AdapterView<?> arg0) {
      }
   };

   @Override
   protected void onDestroy() {
      if (_exportTask != null && _exportTask.getStatus() == Status.RUNNING) {
         _exportTask.cancel(true);
         _exportTask = null;
      }
      super.onDestroy();
   }

   private class ExportTask extends AsyncTask<Void, Void, String> {

      private Record _record;
      private boolean _usePng;
      private String _errorMessage;

      public ExportTask(Record record, boolean usePng) {
         _record = record;
         _usePng = usePng;
      }

      @Override
      protected String doInBackground(Void... params) {
         try {
            ExternalStorageManager ext = _mbwManager.getExternalStorageManager();
            Spinner spDirectory = (Spinner) findViewById(R.id.spDirectory);
            String path = spDirectory.getSelectedItem().toString();
            return ext.exportToExternalStorage(_record, path, _usePng);
         } catch (Exception e) {
            _errorMessage = e.getMessage();
            return null;
         }
      }

      @Override
      protected void onPostExecute(String fileName) {
         findViewById(R.id.btExport).setEnabled(true);
         findViewById(R.id.rbJpeg).setEnabled(true);
         findViewById(R.id.rbPng).setEnabled(true);
         findViewById(R.id.pbSpinner).setVisibility(View.INVISIBLE);
         TextView status = (TextView) findViewById(R.id.tvStatus);
         if (fileName == null) {
            status.setText(getResources().getString(R.string.export_failed, _errorMessage));
            return;
         }
         status.setText(getResources().getString(R.string.export_succeeded, fileName));
         super.onPostExecute(fileName);
      }

   }

}