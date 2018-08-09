package com.mycelium.wapi.wallet.btc.bip44;

import com.google.common.base.Preconditions;
import com.mrd.bitlib.crypto.HdKeyNode;
import com.mrd.bitlib.model.NetworkParameters;
import com.mrd.bitlib.util.ByteReader;
import com.mycelium.wapi.wallet.SecureKeyValueStore;


public class Bip44PubOnlyAccountKeyManager extends Bip44AccountKeyManager {

   public static Bip44PubOnlyAccountKeyManager createFromPublicAccountRoot(HdKeyNode accountRoot, NetworkParameters network, int accountIndex, SecureKeyValueStore secureKeyValueStore) {

      // store the public accountRoot as plaintext
      secureKeyValueStore.storePlaintextValue(getAccountNodeId(network, accountIndex), accountRoot.getPublicNode().toCustomByteFormat());

      // Create the external chain root. Store the public node in plain text
      HdKeyNode externalChainRoot = accountRoot.createChildNode(0);
      secureKeyValueStore.storePlaintextValue(getChainNodeId(network, accountIndex, false), externalChainRoot.getPublicNode().toCustomByteFormat());

      // Create the change chain root. Store the public node in plain text
      HdKeyNode changeChainRoot = accountRoot.createChildNode(1);
      secureKeyValueStore.storePlaintextValue(getChainNodeId(network, accountIndex, true), changeChainRoot.getPublicNode().toCustomByteFormat());
      return new Bip44PubOnlyAccountKeyManager(accountIndex, network, secureKeyValueStore);
   }

   public Bip44PubOnlyAccountKeyManager(int accountIndex, NetworkParameters network, SecureKeyValueStore secureKeyValueStore) {
      super(secureKeyValueStore);
      _accountIndex = accountIndex;
      _network = network;

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
}
