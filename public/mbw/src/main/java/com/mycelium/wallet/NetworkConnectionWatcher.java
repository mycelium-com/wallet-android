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

package com.mycelium.wallet;

import java.util.LinkedList;
import java.util.List;

import android.content.Context;

public class NetworkConnectionWatcher implements Runnable {

   private boolean _quit;
   private Context _context;
   private volatile boolean _isCurrentlyConnected;
   private List<ConnectionObserver> _observers;

   public interface ConnectionObserver {
      public void OnNetworkConnected();

      public void OnNetworkDisconnected();
   }

   public NetworkConnectionWatcher(Context context) {
      _quit = false;
      _context = context;
      _observers = new LinkedList<ConnectionObserver>();
   }

   public void addObserver(ConnectionObserver observer) {
      synchronized (_observers) {
         _observers.add(observer);
//         if (_isCurrentlyConnected) {
//            observer.OnNetworkConnected();
//         } else {
//            observer.OnNetworkDisconnected();
//         }
      }
   }

   public void removeObserver(ConnectionObserver observer) {
      synchronized (_observers) {
         _observers.remove(observer);
      }
   }

   public void stop() {
      _quit = true;
   }

   @Override
   public void run() {
      try {
         _isCurrentlyConnected = Utils.isConnected(_context);
         while (!_quit) {
            Thread.sleep(1000);
            if (_quit) {
               return;
            }
            boolean connected = Utils.isConnected(_context);
            synchronized (_observers) {

               if (connected && !_isCurrentlyConnected) {
                  // We went from not connected to connected
                  // Post a handler that updates our balance
                  for (ConnectionObserver observer : _observers) {
                     observer.OnNetworkConnected();
                  }
               }
               _isCurrentlyConnected = connected;
            }
         }
      } catch (InterruptedException ignored) {
      }

   }
}
