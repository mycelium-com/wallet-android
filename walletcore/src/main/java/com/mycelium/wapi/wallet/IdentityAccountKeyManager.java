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

import com.google.common.base.Preconditions;
import com.mrd.bitlib.crypto.HdKeyNode;
import com.mrd.bitlib.crypto.InMemoryPrivateKey;
import com.mrd.bitlib.model.hdpath.HdKeyPath;
import com.mrd.bitlib.util.BitUtils;
import com.mrd.bitlib.util.ByteReader;
import com.mrd.bitlib.util.HashUtils;
import com.mrd.bitlib.util.Sha256Hash;
import com.mrd.bitlib.crypto.BipDerivationType;
import com.mycelium.wapi.wallet.bip44.HDAccountKeyManager;
import java.nio.charset.Charset;

public class IdentityAccountKeyManager extends HDAccountKeyManager {

   private static final HdKeyPath BITID_KEY_PATH = HdKeyPath.valueOf("m/13'");

   public static IdentityAccountKeyManager createNew(HdKeyNode bip32Root, SecureKeyValueStore secureKeyValueStore, KeyCipher cipher) throws KeyCipher.InvalidKeyCipher {
      HdKeyNode accountRoot = bip32Root.createChildNode(BITID_KEY_PATH);
      // Store the account root (xPub and xPriv) key
      secureKeyValueStore.encryptAndStoreValue(getAccountNodeId(), accountRoot.toCustomByteFormat(), cipher);
      secureKeyValueStore.storePlaintextValue(getAccountNodeId(), accountRoot.getPublicNode().toCustomByteFormat());
      return new IdentityAccountKeyManager(secureKeyValueStore);
   }

   public IdentityAccountKeyManager(SecureKeyValueStore secureKeyValueStore) {
      super(secureKeyValueStore, BipDerivationType.BIP44);

      // Make sure we have the private node in our encrypted store
      Preconditions.checkState(secureKeyValueStore.hasCiphertextValue(getAccountNodeId()));
      try {
         _publicAccountRoot = HdKeyNode.fromCustomByteformat(secureKeyValueStore.getPlaintextValue(getAccountNodeId()));
         Preconditions.checkState(!_publicAccountRoot.isPrivateHdKeyNode());
      } catch (ByteReader.InsufficientBytesException e) {
         throw new RuntimeException(e);
      }
   }

   protected static byte[] getAccountNodeId() {
      byte[] id = new byte[1];
      id[0] = 13;
      return id;
   }

   protected static byte[] getKeyNodeId(byte[] hash) {
      return BitUtils.concatenate(getAccountNodeId(), hash);
   }

   public InMemoryPrivateKey getPrivateKeyForWebsite(String website, KeyCipher cipher) throws KeyCipher.InvalidKeyCipher {
      byte[] hash = getBitidWebsiteHash(website);

      // get the private and public date from cache, if available
      byte[] privKeyBytes = _secureKeyValueStore.getDecryptedValue(getKeyNodeId(hash), cipher);
      byte[] pubKeyBytes = _secureKeyValueStore.getPlaintextValue(getKeyNodeId(hash));

      if (privKeyBytes != null && pubKeyBytes != null) {
         // we have the key cached
         return new InMemoryPrivateKey(privKeyBytes, pubKeyBytes);
      }
      // we need to calculate the key
      byte[] accountNodeId = getAccountNodeId();
      byte[] accountNodeBytes = _secureKeyValueStore.getDecryptedValue(accountNodeId, cipher);
      HdKeyNode accountNode;
      try {
         accountNode = HdKeyNode.fromCustomByteformat(accountNodeBytes);
      } catch (ByteReader.InsufficientBytesException e) {
         throw new RuntimeException(e);
      }
      // Create the private key with the website hash as path
      HdKeyNode websiteNode = getWebsiteNode(accountNode, hash);
      InMemoryPrivateKey key = websiteNode.getPrivateKey();

      //cache for next time
      _secureKeyValueStore.encryptAndStoreValue(getKeyNodeId(hash), key.getPrivateKeyBytes(), cipher);
      _secureKeyValueStore.storePlaintextValue(getKeyNodeId(hash), key.getPublicKey().getPublicKeyBytes());
      return key;
   }

   private HdKeyNode getWebsiteNode(HdKeyNode accountNode, byte[] hash) {
      //split hash into 4 groups as described here: http://doc.satoshilabs.com/slips/slip-0013.html
      int a = (int) BitUtils.uint32ToLong(hash, 0);
      int b = (int) BitUtils.uint32ToLong(hash, 4);
      int c = (int) BitUtils.uint32ToLong(hash, 8);
      int d = (int) BitUtils.uint32ToLong(hash, 12);
      //get node using hardened derivation
      return accountNode.createHardenedChildNode(a).createHardenedChildNode(b).createHardenedChildNode(c).createHardenedChildNode(d);
   }

   @SuppressWarnings("NewApi")
   private static byte[] getBitidWebsiteHash(String website) {
      // the index is used for multiple keys for one website under one identity account - so far, we dont support this, so hardcoded account 0
      byte[] index = new byte[4]; //is initialized with zeroes by default
      byte[] websiteWithIndex = BitUtils.concatenate(index, website.getBytes(Charset.forName("UTF8")));
      Sha256Hash sha = HashUtils.sha256(websiteWithIndex);
      //we take the first 16 bytes, cause we need 4 blocks at 4 bytes each
      return sha.firstNBytes(16);
   }
}
