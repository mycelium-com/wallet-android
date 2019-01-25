/*
 * Copyright 2013, 2014 Megion Research & Development GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mycelium.wapi.wallet.bip44;

import com.google.common.base.Preconditions;
import com.mrd.bitlib.crypto.BipDerivationType;
import com.mrd.bitlib.crypto.HdKeyNode;
import com.mrd.bitlib.crypto.InMemoryPrivateKey;
import com.mrd.bitlib.crypto.PublicKey;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.NetworkParameters;
import com.mrd.bitlib.model.hdpath.HdKeyPath;
import com.mrd.bitlib.util.BitUtils;
import com.mrd.bitlib.util.ByteReader;
import com.mrd.bitlib.util.ByteWriter;
import com.mycelium.wapi.wallet.KeyCipher;
import com.mycelium.wapi.wallet.SecureKeyValueStore;
import com.mycelium.wapi.wallet.SecureSubKeyValueStore;
import kotlin.NotImplementedError;

import java.util.UUID;

/**
 * Management functions for keys associated with a BIP44 account.
 * <p/>
 * Allows you to get a private key, public key, or address for any index in a BIP44 account.
 * <p/>
 * Private keys are calculated from the appropriate private chain root on demand.
 * <p/>
 * Public keys are calculated from the public chain root on demand once, and then stored in plain text for fast
 * retrieval next time they are requested.
 * <p/>
 * Addresses are calculated from the appropriate public key on demand once, and then stored in plain text for fast
 * retrieval next time they are requested.
 */
public class HDAccountKeyManager {
   protected static final int BIP44_PRODNET_COIN_TYPE = 0x80000000;
   protected static final int BIP44_TESTNET_COIN_TYPE = 0x80000001;
   protected int _accountIndex;
   protected final SecureKeyValueStore _secureKeyValueStore;


   protected HdKeyNode _publicAccountRoot;
   protected HdKeyNode _publicExternalChainRoot;
   protected HdKeyNode _publicChangeChainRoot;
   protected NetworkParameters _network;
   private BipDerivationType derivationType;

   public static HDAccountKeyManager createNew(HdKeyNode bip32Root, NetworkParameters network, int accountIndex,
                                               SecureKeyValueStore secureKeyValueStore, KeyCipher cipher,
                                               BipDerivationType derivationType) throws KeyCipher.InvalidKeyCipher {
      HdKeyNode bip44Root = bip32Root.createChildNode(derivationType.getHardenedPurpose());
      HdKeyNode coinTypeRoot = bip44Root.createChildNode(network.isProdnet() ? BIP44_PRODNET_COIN_TYPE : BIP44_TESTNET_COIN_TYPE);

      // Create the account root.
      HdKeyNode accountRoot = coinTypeRoot.createChildNode(accountIndex | 0x80000000);

      return createFromAccountRoot(accountRoot, network, accountIndex, secureKeyValueStore, cipher, derivationType);
   }

   public static HDAccountKeyManager createFromAccountRoot(HdKeyNode accountRoot, NetworkParameters network,
                                                           int accountIndex, SecureKeyValueStore secureKeyValueStore,
                                                           KeyCipher cipher, BipDerivationType derivationType) throws KeyCipher.InvalidKeyCipher {

      // Store the account root (xPub and xPriv) key
      secureKeyValueStore.encryptAndStoreValue(getAccountNodeId(network, accountIndex, derivationType),
              accountRoot.toCustomByteFormat(), cipher);
      secureKeyValueStore.storePlaintextValue(getAccountNodeId(network, accountIndex, derivationType),
              accountRoot.getPublicNode().toCustomByteFormat());

      // Create the external chain root. Store the private node encrypted and the public node in plain text
      HdKeyNode externalChainRoot = accountRoot.createChildNode(0);
      secureKeyValueStore.encryptAndStoreValue(getChainNodeId(network, accountIndex, false, derivationType),
              externalChainRoot.toCustomByteFormat(), cipher);
      secureKeyValueStore.storePlaintextValue(getChainNodeId(network, accountIndex, false, derivationType),
              externalChainRoot.getPublicNode().toCustomByteFormat());

      // Create the change chain root. Store the private node encrypted and the public node in plain text
      HdKeyNode changeChainRoot = accountRoot.createChildNode(1);
      secureKeyValueStore.encryptAndStoreValue(getChainNodeId(network, accountIndex, true, derivationType),
              changeChainRoot.toCustomByteFormat(), cipher);
      secureKeyValueStore.storePlaintextValue(getChainNodeId(network, accountIndex, true, derivationType),
              changeChainRoot.getPublicNode().toCustomByteFormat());
      return new HDAccountKeyManager(accountIndex, network, secureKeyValueStore, derivationType);
   }

   protected HDAccountKeyManager(SecureKeyValueStore secureKeyValueStore, BipDerivationType derivationType) {
      _secureKeyValueStore = secureKeyValueStore;
      this.derivationType = derivationType;
   }

   public HDAccountKeyManager(int accountIndex, NetworkParameters network, SecureKeyValueStore secureKeyValueStore, BipDerivationType derivationType) {
      _accountIndex = accountIndex;
      _secureKeyValueStore = secureKeyValueStore;
      _network = network;
      this.derivationType = derivationType;

      // Make sure we have the private nodes in our encrypted store
      Preconditions.checkState(secureKeyValueStore.hasCiphertextValue(getAccountNodeId(network, accountIndex, derivationType)));
      Preconditions.checkState(secureKeyValueStore.hasCiphertextValue(getChainNodeId(network, accountIndex, false, derivationType)));
      Preconditions.checkState(secureKeyValueStore.hasCiphertextValue(getChainNodeId(network, accountIndex, true, derivationType)));

      // Load the external and internal public nodes
      try {
         _publicAccountRoot = HdKeyNode.fromCustomByteformat(
                 secureKeyValueStore.getPlaintextValue(getAccountNodeId(network, accountIndex, derivationType)));
         Preconditions.checkState(!_publicAccountRoot.isPrivateHdKeyNode());
         _publicExternalChainRoot = HdKeyNode.fromCustomByteformat(
                 secureKeyValueStore.getPlaintextValue(getChainNodeId(network, accountIndex, false, derivationType)));
         Preconditions.checkState(!_publicExternalChainRoot.isPrivateHdKeyNode());
         _publicChangeChainRoot = HdKeyNode.fromCustomByteformat(
                 secureKeyValueStore.getPlaintextValue(getChainNodeId(network, accountIndex, true, derivationType)));
         Preconditions.checkState(!_publicChangeChainRoot.isPrivateHdKeyNode());
      } catch (ByteReader.InsufficientBytesException e) {
         throw new RuntimeException(e);
      }
   }

   public UUID getAccountId() {
      return _publicAccountRoot.getUuid();
   }

   public boolean isValidEncryptionKey(KeyCipher userCipher) {
      return _secureKeyValueStore.isValidEncryptionKey(userCipher);
   }

   public InMemoryPrivateKey getPrivateKey(boolean isChangeChain, int index, KeyCipher cipher) throws KeyCipher.InvalidKeyCipher {
      // Load the encrypted chain node from the secure storage
      byte[] chainNodeId = getChainNodeId(_network, _accountIndex, isChangeChain, derivationType);
      byte[] chainNodeBytes = _secureKeyValueStore.getDecryptedValue(chainNodeId, cipher);
      HdKeyNode chainNode;
      try {
         chainNode = HdKeyNode.fromCustomByteformat(chainNodeBytes);
      } catch (ByteReader.InsufficientBytesException e) {
         throw new RuntimeException(e);
      }
      // Create the private key with the appropriate index
      return chainNode.createChildPrivateKey(index);
   }

   public PublicKey getPublicKey(boolean isChangeChain, int index) {
      // See if we have it in the store
      byte[] id = getLeafNodeId(_network, _accountIndex, isChangeChain, index, true, derivationType);
      byte[] publicLeafNodeBytes = _secureKeyValueStore.getPlaintextValue(id);
      if (publicLeafNodeBytes != null) {
         // We have it already, no need to calculate it
         try {
            HdKeyNode publicLeafNode = HdKeyNode.fromCustomByteformat(publicLeafNodeBytes);
            return publicLeafNode.getPublicKey();
         } catch (ByteReader.InsufficientBytesException e) {
            throw new RuntimeException(e);
         }
      }

      // Calculate it from the chain node
      HdKeyNode chainNode = isChangeChain ? _publicChangeChainRoot : _publicExternalChainRoot;
      HdKeyNode publicLeafNode = chainNode.createChildNode(index);

      // Store it for next time
      _secureKeyValueStore.storePlaintextValue(id, publicLeafNode.toCustomByteFormat());
      return publicLeafNode.getPublicKey();
   }

   public Address getAddress(boolean isChangeChain, int index) {
      // See if we have it in the store
      byte[] id = getLeafNodeId(_network, _accountIndex, isChangeChain, index, false, derivationType);
      byte[] addressNodeBytes = _secureKeyValueStore.getPlaintextValue(id);
      HdKeyPath purpose;
      switch (derivationType) {
         case BIP44:
            purpose = HdKeyPath.BIP44;
            break;
         case BIP49:
            purpose = HdKeyPath.BIP49;
            break;
         case BIP84:
            purpose = HdKeyPath.BIP84;
            break;
         default:
            throw new NotImplementedError();
      }

      final HdKeyPath path = purpose
            .getCoinTypeBitcoin(_network.isTestnet())
            .getAccount(_accountIndex)
            .getChain(!isChangeChain)
            .getAddress(index);

      if (addressNodeBytes != null) {
         // We have it already, no need to calculate it
         return bytesToAddress(addressNodeBytes, path);
      }

      // We don't have it, need to calculate it from the public key
      PublicKey publicKey = getPublicKey(isChangeChain, index);
      Address address = publicKey.toAddress(_network, derivationType.getAddressType());
      address.setBip32Path(path);

      // Store it for next time
      _secureKeyValueStore.storePlaintextValue(id, addressToBytes(address));
      return address;
   }

   public BipDerivationType getDerivationType() {
      return derivationType;
   }

   protected static byte[] getAccountNodeId(NetworkParameters network, int accountIndex, BipDerivationType derivationType) {
      // Create a compact unique account ID
      byte[] id = new byte[1 + 1 + 4];
      id[0] = derivationType.getPurpose();
      id[1] = (byte) (network.isProdnet() ? 0 : 1); // network
      BitUtils.uint32ToByteArrayLE(accountIndex, id, 2); // account index
      return id;
   }

   protected static byte[] getChainNodeId(NetworkParameters network, int accountIndex, boolean isChangeChain,
                                          BipDerivationType derivationType) {
      // Create a compact unique chain node ID
      byte[] id = new byte[1 + 1 + 4 + 1];
      id[0] = derivationType.getPurpose();
      id[1] = (byte) (network.isProdnet() ? 0 : 1); // network
      BitUtils.uint32ToByteArrayLE(accountIndex, id, 2); // account index
      id[6] = (byte) (isChangeChain ? 1 : 0); // external chain or change chain
      return id;
   }

   private static byte[] getLeafNodeId(NetworkParameters network, int accountIndex, boolean isChangeChain, int index,
                                       boolean isHdNode, BipDerivationType derivationType) {
      // Create a compact unique address or HD node ID
      byte[] id = new byte[1 + 1 + 4 + 1 + 4 + 1];
      id[0] = derivationType.getPurpose();
      id[1] = (byte) (network.isProdnet() ? 0 : 1); // network
      BitUtils.uint32ToByteArrayLE(accountIndex, id, 2); // account index
      id[6] = (byte) (isChangeChain ? 1 : 0); // external chain or change chain
      BitUtils.uint32ToByteArrayLE(index, id, 7); // address index
      id[11] = (byte) (isHdNode ? 1 : 0); // is HD node or address
      return id;
   }


   private static byte[] addressToBytes(Address address) {
      ByteWriter writer = new ByteWriter(1024);
      // Add address as bytes
      writer.putBytes(address.getAllAddressBytes());
      // Add address as string
      String addressString = address.toString();
      writer.put((byte) addressString.length());
      writer.putBytes(addressString.getBytes());
      return writer.toBytes();
   }

   private static Address bytesToAddress(byte[] bytes, HdKeyPath path) {
      try {
         ByteReader reader = new ByteReader(bytes);
         // Address bytes
         reader.getBytes(21);
         // Read length encoded string
         String addressString = new String(reader.getBytes((int) reader.get()));
         Address address = Address.fromString(addressString);
         address.setBip32Path(path);
         return address;
      } catch (ByteReader.InsufficientBytesException e) {
         throw new RuntimeException(e);
      }
   }

   public HdKeyNode getPublicAccountRoot() {
      return _publicAccountRoot;
   }

   public HdKeyNode getPrivateAccountRoot(KeyCipher cipher, BipDerivationType derivationType) throws KeyCipher.InvalidKeyCipher {
      try {
         return HdKeyNode.fromCustomByteformat(_secureKeyValueStore.getDecryptedValue(getAccountNodeId(_network,
                 _accountIndex, derivationType), cipher));
      } catch (ByteReader.InsufficientBytesException e) {
         throw new RuntimeException(e);
      }
   }

   public void deleteSubKeyStore(){
      if (_secureKeyValueStore instanceof SecureSubKeyValueStore){
         ((SecureSubKeyValueStore) _secureKeyValueStore).deleteAllData();
      } else {
         throw new RuntimeException("this is not a SubKeyValueStore");
      }
   }
}
