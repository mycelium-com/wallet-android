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

package com.mycelium.wallet.bitid;


import android.app.AlertDialog;
import android.content.Context;

import com.mrd.bitlib.model.NetworkParameters;
import com.mycelium.wallet.R;

public enum ExternalService {
   ; // empty enum, hooray?
   private final String prodnetHost;
   private final String testnetHost;
   private final String prodnetApi;
   private final String testnetApi;
   private final int welcomeTitleId;
   private final int welcomeMessageId;

   ExternalService(String prodnetHost, String testnetHost, String prodnetApi, String testnetApi, int welcomeTitleId, int welcomeMessageId) {
      this.prodnetHost = prodnetHost;
      this.testnetHost = testnetHost;
      this.prodnetApi = prodnetApi;
      this.testnetApi = testnetApi;
      this.welcomeTitleId = welcomeTitleId;
      this.welcomeMessageId = welcomeMessageId;
   }

   public String getApi(NetworkParameters network) {
      if (network.isProdnet()) return prodnetApi;
      if (network.isTestnet()) return testnetApi;
      throw new RuntimeException("Invalid network: " + network.toString());
   }

   public String getHost(NetworkParameters network) {
      if (network.isProdnet()) return prodnetHost;
      if (network.isTestnet()) return testnetHost;
      throw new RuntimeException("Invalid network: " + network.toString());
   }

   public void showWelcomeMessage(Context context) {
      new AlertDialog.Builder(context)
            .setTitle(context.getString(welcomeTitleId))
            .setMessage(context.getString(welcomeMessageId))
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton(context.getString(R.string.ok), null)
            .show();
   }
}
