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

package com.mycelium.wallet.activity.util;

import android.content.Context;
import com.google.common.base.Optional;
import com.mrd.bitlib.crypto.Bip39;
import com.mrd.bitlib.crypto.HdKeyNode;
import com.mrd.bitlib.model.*;
import com.mrd.bitlib.model.hdpath.HdKeyPath;
import com.mycelium.wapi.wallet.WalletManager;
import com.squareup.otto.Bus;

import java.util.UUID;

public class MasterseedScanManager extends AbstractAccountScanManager {
   private Bip39.MasterSeed masterSeed;
   private final String[] words;
   private HdKeyNode accountsRoot = null;


   public MasterseedScanManager(Context context, NetworkParameters network, Bip39.MasterSeed masterSeed, Bus eventBus){
      super(context, network, eventBus);
      this.masterSeed = masterSeed;
      this.words = null;
   }

   public MasterseedScanManager(Context context, NetworkParameters network, String[] words, Bus eventBus){
      super(context, network, eventBus);
      this.words = words;
   }


   @Override
   protected boolean onBeforeScan() {
      // if we only have a wordlist, query the user for a passphrase
      if (masterSeed == null){
         Optional<String> passphrase = waitForPassphrase();
         if (passphrase.isPresent()){
            this.masterSeed = Bip39.generateSeedFromWordList(words, passphrase.get());
            return true;
         }else{
            return false;
         }
      }

      return true;
   }

   @Override
   public Optional<HdKeyNode> getAccountPubKeyNode(int accountIndex){
      // Generate the root private key
      HdKeyNode root = HdKeyNode.fromSeed(masterSeed.getBip32Seed());
      if (accountsRoot == null) {
         accountsRoot = root.createChildNode(HdKeyPath.BIP44.getBip44CoinType(getNetwork()));
      }
      HdKeyNode childNode = accountsRoot.createHardenedChildNode(accountIndex);
      return Optional.of(childNode);
   }

   @Override
   public UUID createOnTheFlyAccount(HdKeyNode accountRoot, WalletManager walletManager, int accountIndex) {
      UUID account;
      if (walletManager.hasAccount(accountRoot.getUuid())){
         // Account already exists
         account = accountRoot.getUuid();
      }else {
         account = walletManager.createUnrelatedBip44Account(accountRoot);
      }
      return account;
   }

}
