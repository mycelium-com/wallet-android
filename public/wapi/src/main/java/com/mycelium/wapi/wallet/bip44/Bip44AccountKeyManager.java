package com.mycelium.wapi.wallet.bip44;

import com.google.common.base.Preconditions;
import com.mrd.bitlib.crypto.HdKeyNode;
import com.mrd.bitlib.crypto.InMemoryPrivateKey;
import com.mrd.bitlib.crypto.PublicKey;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.NetworkParameters;
import com.mrd.bitlib.util.BitUtils;
import com.mrd.bitlib.util.ByteReader;
import com.mrd.bitlib.util.ByteWriter;
import com.mycelium.wapi.wallet.KeyCipher;
import com.mycelium.wapi.wallet.SecureKeyValueStore;

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
public class Bip44AccountKeyManager {

   private static final int BIP44_PURPOSE = 0x8000002c;
   private static final int BIP44_PRODNET_COIN_TYPE = 0x80000000;
   private static final int BIP44_TESTNET_COIN_TYPE = 0x80000001;
   private int _accountIndex;
   private final SecureKeyValueStore _secureKeyValueStore;
   private HdKeyNode _publicAccountRoot;
   private HdKeyNode _publicExternalChainRoot;
   private HdKeyNode _publicChangeChainRoot;
   private NetworkParameters _network;

   public static Bip44AccountKeyManager createNew(HdKeyNode bip32Root, NetworkParameters network, int accountIndex, SecureKeyValueStore secureKeyValueStore, KeyCipher cipher) throws KeyCipher.InvalidKeyCipher {
      HdKeyNode bip44Root = bip32Root.createChildNode(BIP44_PURPOSE);
      HdKeyNode coinTypeRoot = bip44Root.createChildNode(network.isProdnet() ? BIP44_PRODNET_COIN_TYPE : BIP44_TESTNET_COIN_TYPE);

      // Create the account root. Store the private node encrypted and the public node in plain text
      HdKeyNode accountRoot = coinTypeRoot.createChildNode(accountIndex | 0x80000000);
      secureKeyValueStore.encryptAndStoreValue(getAccountNodeId(network, accountIndex), accountRoot.toCustomByteFormat(), cipher);
      secureKeyValueStore.storePlaintextValue(getAccountNodeId(network, accountIndex), accountRoot.getPublicNode().toCustomByteFormat());

      // Create the external chain root. Store the private node encrypted and the public node in plain text
      HdKeyNode externalChainRoot = accountRoot.createChildNode(0);
      secureKeyValueStore.encryptAndStoreValue(getChainNodeId(network, accountIndex, false), externalChainRoot.toCustomByteFormat(), cipher);
      secureKeyValueStore.storePlaintextValue(getChainNodeId(network, accountIndex, false), externalChainRoot.getPublicNode().toCustomByteFormat());

      // Create the change chain root. Store the private node encrypted and the public node in plain text
      HdKeyNode changeChainRoot = accountRoot.createChildNode(1);
      secureKeyValueStore.encryptAndStoreValue(getChainNodeId(network, accountIndex, true), changeChainRoot.toCustomByteFormat(), cipher);
      secureKeyValueStore.storePlaintextValue(getChainNodeId(network, accountIndex, true), changeChainRoot.getPublicNode().toCustomByteFormat());
      return new Bip44AccountKeyManager(accountIndex, network, secureKeyValueStore);
   }


   public Bip44AccountKeyManager(int accountIndex, NetworkParameters network, SecureKeyValueStore secureKeyValueStore) {
      _accountIndex = accountIndex;
      _secureKeyValueStore = secureKeyValueStore;
      _network = network;

      // Make sure we have the private nodes in our encrypted store
      Preconditions.checkState(secureKeyValueStore.hasCiphertextValue(getAccountNodeId(network, accountIndex)));
      Preconditions.checkState(secureKeyValueStore.hasCiphertextValue(getChainNodeId(network, accountIndex, false)));
      Preconditions.checkState(secureKeyValueStore.hasCiphertextValue(getChainNodeId(network, accountIndex, true)));

      // Load the external and internal public nodes
      try {
         _publicAccountRoot = HdKeyNode.fromCustomByteformat(secureKeyValueStore.getPlaintextValue(getAccountNodeId(network, accountIndex)));
         Preconditions.checkState(!_publicAccountRoot.isPrivateHdKeyNode());
         _publicExternalChainRoot = HdKeyNode.fromCustomByteformat(secureKeyValueStore.getPlaintextValue(getChainNodeId(network, accountIndex, false)));
         Preconditions.checkState(!_publicExternalChainRoot.isPrivateHdKeyNode());
         _publicChangeChainRoot = HdKeyNode.fromCustomByteformat(secureKeyValueStore.getPlaintextValue(getChainNodeId(network, accountIndex, true)));
         Preconditions.checkState(!_publicChangeChainRoot.isPrivateHdKeyNode());
      } catch (ByteReader.InsufficientBytesException e) {
         throw new RuntimeException(e);
      }
   }

   public UUID getAccountId() {
      // Create a UUID from the byte indexes 8-15 and 16-23 of the account public key
      byte[] publicKeyBytes = _publicAccountRoot.getPublicKey().getPublicKeyBytes();
      return new UUID(BitUtils.uint64ToLong(publicKeyBytes, 8), BitUtils.uint64ToLong(
            publicKeyBytes, 16));
   }

   public boolean isValidEncryptionKey(KeyCipher userCipher) {
      return _secureKeyValueStore.isValidEncryptionKey(userCipher);
   }

   public InMemoryPrivateKey getPrivateKey(boolean isChangeChain, int index, KeyCipher cipher) throws KeyCipher.InvalidKeyCipher {
      // Load the encrypted chain node from the secure storage
      byte[] chainNodeId = getChainNodeId(_network, _accountIndex, isChangeChain);
      byte[] chainNodeBytes = _secureKeyValueStore.getEncryptedValue(chainNodeId, cipher);
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
      byte[] id = getLeafNodeId(_network, _accountIndex, isChangeChain, index, true);
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
      byte[] id = getLeafNodeId(_network, _accountIndex, isChangeChain, index, false);
      byte[] addressNodeBytes = _secureKeyValueStore.getPlaintextValue(id);
      if (addressNodeBytes != null) {
         // We have it already, no need to calculate it
         return bytesToAddress(addressNodeBytes);
      }

      // We don't have it, need to calculate it from the public key
      PublicKey publicKey = getPublicKey(isChangeChain, index);
      Address address = publicKey.toAddress(_network);

      // Store it for next time
      _secureKeyValueStore.storePlaintextValue(id, addressToBytes(address));
      return address;
   }

   private static byte[] getAccountNodeId(NetworkParameters network, int accountIndex) {
      // Create a compact unique account ID
      byte[] id = new byte[1 + 1 + 4];
      id[0] = 44; // BIP44
      id[1] = (byte) (network.isProdnet() ? 0 : 1); // network
      BitUtils.uint32ToByteArrayLE(accountIndex, id, 2); // account index
      return id;
   }

   private static byte[] getChainNodeId(NetworkParameters network, int accountIndex, boolean isChangeChain) {
      // Create a compact unique chain node ID
      byte[] id = new byte[1 + 1 + 4 + 1];
      id[0] = 44; // BIP44
      id[1] = (byte) (network.isProdnet() ? 0 : 1); // network
      BitUtils.uint32ToByteArrayLE(accountIndex, id, 2); // account index
      id[6] = (byte) (isChangeChain ? 1 : 0); // external chain or change chain
      return id;
   }

   private static byte[] getLeafNodeId(NetworkParameters network, int accountIndex, boolean isChangeChain, int index, boolean isHdNode) {
      // Create a compact unique address or HD node ID
      byte[] id = new byte[1 + 1 + 4 + 1 + 4 + 1];
      id[0] = 44; // BIP44
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

   private static Address bytesToAddress(byte[] bytes) {
      try {
         ByteReader reader = new ByteReader(bytes);
         // Address bytes
         byte[] addressBytes = reader.getBytes(21);
         // Read length encoded string
         String addressString = null;
         addressString = new String(reader.getBytes((int) reader.get()));
         return new Address(addressBytes, addressString);
      } catch (ByteReader.InsufficientBytesException e) {
         throw new RuntimeException(e);
      }
   }


}
