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

package com.mycelium.wallet.wapi;

import android.content.Context;

import com.mycelium.wapi.api.exception.DbCorruptedException;

// Wrapper for SqliteWalletManagerBacking, to catch the DbCorrupted RuntimeException and inform the user about it
public class SqliteWalletManagerBackingWrapper extends SqliteWalletManagerBacking {
   public SqliteWalletManagerBackingWrapper(Context context) {
      super(context);
   }

   @Override
   public byte[] getValue(byte[] id) {
      try {
         return super.getValue(id);
      } catch (final DbCorruptedException dbe) {
         // inform the user that something wrong is going on with his hardware
         // todo: fix/show info to the user, that something wrong happened with his DB. for now, just report the error
         // Utils.showSimpleMessageDialog(context.getApplicationContext(), "The database storing your private and public keys is corrupted. This might be the result of an hardware error. \n\nIt is advisable to delete the application data and restore from your backup. \nIf this happens again, think about getting a new device.");

         // rethrow the exception, so that the app exits and we get an error mail
         throw new RuntimeException(dbe);
      }
   }
}
