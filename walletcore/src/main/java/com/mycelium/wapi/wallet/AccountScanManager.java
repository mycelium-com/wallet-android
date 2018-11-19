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

package com.mycelium.wapi.wallet;

import com.google.common.base.Optional;
import com.mrd.bitlib.crypto.BipDerivationType;
import com.mrd.bitlib.crypto.HdKeyNode;
import com.mrd.bitlib.model.hdpath.HdKeyPath;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface AccountScanManager {
   void startBackgroundAccountScan(AccountCallback scanningCallback);

   void stopBackgroundAccountScan();

   void forgetAccounts();

   List<HdKeyNode> getNextUnusedAccounts();
   Optional<HdKeyNode> getAccountPubKeyNode(HdKeyPath keyPath, BipDerivationType derivationType);
   Map<BipDerivationType, ? extends HdKeyPath> getAccountPathsToScan(HdKeyPath lastPath, boolean wasUsed);

   void setPassphrase(String passphrase);


   enum Status{
      unableToScan, initializing, readyToScan
   }

   enum AccountStatus{
      unknown, scanning, done
   }

   class HdKeyNodeWrapper {
      final public Collection<HdKeyPath> keysPaths;
      final public List<HdKeyNode> accountsRoots;
      final public UUID accountId;

      public HdKeyNodeWrapper(Collection<HdKeyPath> keysPaths, List<HdKeyNode> accountsRoots, UUID accountId) {
         this.keysPaths = keysPaths;
         this.accountsRoots = accountsRoots;
         this.accountId = accountId;
      }
   }

   // Classes for the EventBus
   class OnAccountFound {
      public final HdKeyNodeWrapper account;

      public OnAccountFound(HdKeyNodeWrapper account) {
         this.account = account;
      }
   }

   class OnStatusChanged {
      public final Status state;
      public final AccountStatus accountState;

      public OnStatusChanged(Status state, AccountStatus accountState) {
         this.state = state;
         this.accountState = accountState;
      }
   }

   class OnScanError {
      public final String errorMessage;
      public final ErrorType errorType;

      public enum ErrorType {
         UNKNOWN, NOT_INITIALIZED;
      }

      public OnScanError(String errorMessage) {
         this(errorMessage, ErrorType.UNKNOWN);
      }

      public OnScanError(String errorMessage, ErrorType errorType) {
         this.errorMessage = errorMessage;
         this.errorType = errorType;
      }
   }

   class OnPassphraseRequest {
   }

   interface AccountCallback {
      // gets called from a background thread
      UUID checkForTransactions(HdKeyNodeWrapper account);
   }
}
