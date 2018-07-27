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
import android.content.Intent;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.mycelium.wallet.*;

import java.util.ArrayList;

public class ConnectionLogsActivity extends Activity {

   public static void callMe(Activity activity){
      Intent intent = new Intent(activity, ConnectionLogsActivity.class);
      activity.startActivity(intent);
   }

   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);

      setContentView(R.layout.show_logs_activity);

      final MbwManager mbwManager = MbwManager.getInstance(this);

      // get the log entries in a new list and show them in reverse order
      ArrayList<LogEntry> logEntries = Lists.newArrayList(mbwManager.getWapiLogs());
      final String logs = Joiner.on("\n").join(Lists.reverse(logEntries));

      TextView tvLogDisplay = (TextView) findViewById(R.id.tvLogDisplay);
      tvLogDisplay.setText(logs);
      tvLogDisplay.setHorizontallyScrolling(true);
      tvLogDisplay.setMovementMethod(new ScrollingMovementMethod());

      tvLogDisplay.setOnLongClickListener(new View.OnLongClickListener() {
         @Override
         public boolean onLongClick(View view) {
            Utils.setClipboardString(logs, ConnectionLogsActivity.this);
            Toast.makeText(ConnectionLogsActivity.this, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();
            return true;
         }
      });
   }
}
