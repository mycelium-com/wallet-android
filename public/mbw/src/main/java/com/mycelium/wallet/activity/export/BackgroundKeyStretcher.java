/*
 * Copyright 2013 Megion Research and Development GmbH
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

package com.mycelium.wallet.activity.export;

import android.os.Handler;

import com.mrd.bitlib.crypto.MrdExport;
import com.mrd.bitlib.crypto.MrdExport.V1.EncryptionParameters;
import com.mrd.bitlib.crypto.MrdExport.V1.KdfParameters;

/**
 * Stretch a key in the background and do proper thread cleanup if terminated
 */
public class BackgroundKeyStretcher {

   /**
    * Implement this to get the result of the key stretching. Not called if
    * terminated
    */
   public static abstract class PostRunner implements Runnable {
      private boolean _error;
      private MrdExport.V1.EncryptionParameters _parameters;

      public void setResult(boolean error, MrdExport.V1.EncryptionParameters parameters) {
         _error = error;
         _parameters = parameters;
      }

      public void run() {
         onPostExecute(_error, _parameters);
      }

      /**
       * Implement this to get the result of the key stretching. Not called if
       * terminated
       */
      public abstract void onPostExecute(boolean error, MrdExport.V1.EncryptionParameters parameters);
   }

   public KdfParameters getProgressTracker() {
      return _kdfParameters;
   }

   private Handler _handler;
   private KdfParameters _kdfParameters;
   private PostRunner _postRunner;
   private Thread _thread;

   public void start(KdfParameters kdfParameters, PostRunner postRunner) {
      _handler = new Handler();
      _kdfParameters = kdfParameters;
      _postRunner = postRunner;
      _thread = new Thread(new Runner());
      _thread.start();
   }

   public void terminate() {
      if (_thread == null) {
         return;
      }
      KdfParameters tracker = _kdfParameters;
      if (tracker != null) {
         tracker.terminate();
      }
      try {
         _thread.join();
         _thread = null;
      } catch (InterruptedException e) {
         // Ignore
      }
   }

   private class Runner implements Runnable {

      @Override
      public void run() {
         try {
            EncryptionParameters parameters = MrdExport.V1.EncryptionParameters.generate(_kdfParameters);
            _postRunner.setResult(false, parameters);
         } catch (OutOfMemoryError e) {
            _postRunner.setResult(true, null);
         } catch (InterruptedException e) {
            // This happens when it is terminated
            return;
         }
         _kdfParameters = null;
         _handler.post(_postRunner);
      }
   }
}
