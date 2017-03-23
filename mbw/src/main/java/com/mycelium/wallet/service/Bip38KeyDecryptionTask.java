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

package com.mycelium.wallet.service;

import android.content.Context;

import com.mrd.bitlib.crypto.Bip38;
import com.mrd.bitlib.lambdaworks.crypto.SCryptProgress;
import com.mrd.bitlib.model.NetworkParameters;
import com.mycelium.wallet.R;
import com.mycelium.wallet.UserFacingException;

public class Bip38KeyDecryptionTask extends ServiceTask<String> {
   private static final long serialVersionUID = 1L;

   private String _bip38PrivateKeyString;
   private String _passphrase;
   private NetworkParameters _network;
   private String _statusMessage;
   private SCryptProgress _progress;

   public Bip38KeyDecryptionTask(String bip38PrivateKeyString, String passphrase, Context context,
                                 NetworkParameters network) {
      _bip38PrivateKeyString = bip38PrivateKeyString;
      _passphrase = passphrase;
      _network = network;
      _statusMessage = context.getResources().getString(R.string.import_decrypt_stretching);
   }

   @Override
   protected String doTask(Context context) throws UserFacingException {
      _progress = Bip38.getScryptProgressTracker();
      // Do BIP38 decryption
      String result;
      try {
         result = Bip38.decrypt(_bip38PrivateKeyString, _passphrase, _progress, _network);
      } catch (InterruptedException e) {
         return null;
      } catch (OutOfMemoryError e) {
         throw new UserFacingException(e);
      }
      // The result may be null
      return result;
   }

   @Override
   protected void terminate() {
      // Tell scrypt to stop
      _progress.terminate();
   }

   @Override
   protected ServiceTaskStatus getStatus() {
      return new ServiceTaskStatus(_statusMessage, _progress == null ? 0 : _progress.getProgress());
   }

}
