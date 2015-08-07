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

package com.mrd.bitlib.model;

import java.io.Serializable;

import com.mrd.bitlib.model.hdpath.HdKeyPath;
import com.mrd.bitlib.util.HexUtils;

/**
 * Settings for the network used. Can be either the test or production network.
 */
public class NetworkParameters implements Serializable {
   private static final long serialVersionUID = 1L;

   public static final int PROTOCOL_VERSION = 70002;
   public static final NetworkParameters testNetwork;
   public static final NetworkParameters productionNetwork;
   private static byte[] TESTNET_GENESIS_BLOCK;

   private final String _blockchain_explorer_transaction;
   private final String _blockchain_explorer_address;

   private final HdKeyPath _bip44_coin_type;

   private static byte[] PRODNET_GENESIS_BLOCK;

   static {
      TESTNET_GENESIS_BLOCK = HexUtils.toBytes("0100000043497fd7f826957108f4a30fd9cec3aeba79972084e90ead01ea3309"
            + "00000000bac8b0fa927c0ac8234287e33c5f74d38d354820e24756ad709d7038"
            + "fc5f31f020e7494dffff001d03e4b67201010000000100000000000000000000"
            + "00000000000000000000000000000000000000000000ffffffff0e0420e7494d"
            + "017f062f503253482fffffffff0100f2052a010000002321021aeaf2f8638a12"
            + "9a3156fbe7e5ef635226b0bafd495ff03afe2c843d7e3a4b51ac00000000");
      PRODNET_GENESIS_BLOCK = HexUtils.toBytes("0100000000000000000000000000000000000000000000000000000000000000"
            + "000000003ba3edfd7a7b12b27ac72c3e67768f617fc81bc3888a51323a9fb8aa"
            + "4b1e5e4a29ab5f49ffff001d1dac2b7c01010000000100000000000000000000"
            + "00000000000000000000000000000000000000000000ffffffff4d04ffff001d"
            + "0104455468652054696d65732030332f4a616e2f32303039204368616e63656c"
            + "6c6f72206f6e206272696e6b206f66207365636f6e64206261696c6f75742066"
            + "6f722062616e6b73ffffffff0100f2052a01000000434104678afdb0fe554827"
            + "1967f1a67130b7105cd6a828e03909a67962e0ea1f61deb649f6bc3f4cef38c4"
            + "f35504e51ec112de5c384df7ba0b8d578a4c702b6bf11d5fac00000000");
      testNetwork = new NetworkParameters(false);
      productionNetwork = new NetworkParameters(true);
   }

   /**
    * The first byte of a base58 encoded bitcoin standard address.
    */
   private int _standardAddressHeader;

   /**
    * The first byte of a base58 encoded bitcoin multisig address.
    */
   private int _multisigAddressHeader;

   /**
    * The genesis block
    */
   private byte[] _genesisBlock;

   private int _port;
   private int _packetMagic;
   private byte[] _packetMagicBytes;

   private NetworkParameters(boolean isProdnet) {
      if (isProdnet) {
         _standardAddressHeader = 0x00;
         _multisigAddressHeader = 0x05;
         _genesisBlock = PRODNET_GENESIS_BLOCK;
         _port = 8333;
         _packetMagic = 0xf9beb4d9;
         _packetMagicBytes = new byte[] { (byte) 0xf9, (byte) 0xbe, (byte) 0xb4, (byte) 0xd9 };
         _blockchain_explorer_address = "https://blockchain.info/address/";
         _blockchain_explorer_transaction = "https://blockchain.info/tx/";
         _bip44_coin_type = HdKeyPath.BIP44_PRODNET;
      } else {
         _standardAddressHeader = 0x6F;
         _multisigAddressHeader = 0xC4;
         _genesisBlock = TESTNET_GENESIS_BLOCK;
         _port = 18333;
         // _packetMagic = 0xfabfb5da;
         _packetMagic = 0x0b110907;
         // _packetMagicBytes = new byte[] { (byte) 0xfa, (byte) 0xbf, (byte)
         // 0xb5, (byte) 0xda };
         _packetMagicBytes = new byte[] { (byte) 0x0b, (byte) 0x11, (byte) 0x09, (byte) 0x07 };
         _blockchain_explorer_address = "http://tbtc.blockr.io/address/info/";
         _blockchain_explorer_transaction = "http://tbtc.blockr.io/tx/info/";
         _bip44_coin_type = HdKeyPath.BIP44_TESTNET;
      }
   }

   /**
    * Get the first byte of a base58 encoded bitcoin address as an integer.
    * 
    * @return The first byte of a base58 encoded bitcoin address as an integer.
    */
   public int getStandardAddressHeader() {
      return _standardAddressHeader;
   }

   /**
    * Get the first byte of a base58 encoded bitcoin multisig address as an
    * integer.
    * 
    * @return The first byte of a base58 encoded bitcoin multisig address as an
    *         integer.
    */
   public int getMultisigAddressHeader() {
      return _multisigAddressHeader;
   }

   public byte[] getGenesisBlock() {
      return _genesisBlock;
   }

   public int getPort() {
      return _port;
   }

   public int getPacketMagic() {
      return _packetMagic;
   }

   public byte[] getPacketMagicBytes() {
      return _packetMagicBytes;
   }

   @Override
   public int hashCode() {
      return _standardAddressHeader;
   };

   public boolean isProdnet() {
      return this.equals(NetworkParameters.productionNetwork);
   }

   public boolean isTestnet() {
      return this.equals(NetworkParameters.testNetwork);
   }

   // used for Trezor coin_name
   public String getCoinName(){
      return isProdnet() ? "Bitcoin" : "Testnet";
   }

   @Override
   public boolean equals(Object obj) {
      if (!(obj instanceof NetworkParameters)) {
         return false;
      }
      NetworkParameters other = (NetworkParameters) obj;
      return other._standardAddressHeader == _standardAddressHeader;
   }

   @Override
   public String toString() {
      return isProdnet() ? "prodnet" : "testnet";
   }



   public HdKeyPath getBip44CoinType() {
      return _bip44_coin_type;
   }

}


