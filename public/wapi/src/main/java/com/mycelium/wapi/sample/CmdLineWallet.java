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

package com.mycelium.wapi.sample;

import com.mrd.bitlib.StandardTransactionBuilder.InsufficientFundsException;
import com.mrd.bitlib.StandardTransactionBuilder.OutputTooSmallException;
import com.mrd.bitlib.StandardTransactionBuilder.UnsignedTransaction;
import com.mrd.bitlib.TransactionUtils;
import com.mrd.bitlib.crypto.Bip39;
import com.mrd.bitlib.crypto.InMemoryPrivateKey;
import com.mrd.bitlib.crypto.RandomSource;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.NetworkParameters;
import com.mrd.bitlib.model.Transaction;
import com.mrd.bitlib.util.HashUtils;
import com.mrd.bitlib.util.Sha256Hash;
import com.mycelium.net.HttpEndpoint;
import com.mycelium.net.HttpsEndpoint;
import com.mycelium.net.ServerEndpoints;
import com.mycelium.wapi.api.Wapi;
import com.mycelium.wapi.api.WapiClient;
import com.mycelium.wapi.api.WapiException;
import com.mycelium.WapiLogger;
import com.mycelium.wapi.api.request.GetTransactionsRequest;
import com.mycelium.wapi.api.request.QueryTransactionInventoryRequest;
import com.mycelium.wapi.api.request.QueryUnspentOutputsRequest;
import com.mycelium.wapi.api.response.GetTransactionsResponse;
import com.mycelium.wapi.api.response.QueryTransactionInventoryResponse;
import com.mycelium.wapi.api.response.QueryUnspentOutputsResponse;
import com.mycelium.wapi.model.TransactionEx;
import com.mycelium.wapi.model.TransactionOutputEx;
import com.mycelium.wapi.model.TransactionSummary;
import com.mycelium.wapi.wallet.*;
import com.mycelium.wapi.wallet.KeyCipher.InvalidKeyCipher;
import com.mycelium.wapi.wallet.WalletAccount.Receiver;
import com.mycelium.wapi.wallet.WalletManager.Event;
import com.mycelium.wapi.wallet.WalletManager.Observer;
import com.mycelium.wapi.wallet.WalletManager.State;
import com.mycelium.wapi.wallet.currency.ExactBitcoinValue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.SecureRandom;
import java.util.*;

public class CmdLineWallet {

   private static MyRandomSource _randomSource = new MyRandomSource();

   /**
    * @param args
    * @throws WapiException
    * @throws InvalidKeyCipher
    */
   public static void main(String[] args) throws WapiException, InvalidKeyCipher {
      simple();
      //massiveSender();
   }

   private static void massiveSender() throws WapiException, InvalidKeyCipher {
      NetworkParameters network = NetworkParameters.testNetwork;

      String myceliumThumbprint = "E5:70:76:B2:67:3A:89:44:7A:48:14:81:DF:BD:A0:58:C8:82:72:4F";
      HttpEndpoint endpoint = new HttpsEndpoint("https://node3.mycelium.com/wapitestnet", myceliumThumbprint);

      WapiLogger logger = new WapiLogger() {

         @Override
         public void logError(String message) {
            System.err.println(message);
         }

         @Override
         public void logError(String message, Exception e) {
            System.err.println(message);
            e.printStackTrace();
         }

         @Override
         public void logInfo(String message) {
            System.out.println(message);
         }
      };

      ServerEndpoints serverEndpoints = new ServerEndpoints(new HttpEndpoint[]{endpoint});
      WapiClient wapi = new WapiClient(serverEndpoints, logger, "sample");

      AesKeyCipher cipher = AesKeyCipher.defaultKeyCipher();

      WalletManagerBacking backing1 = new InMemoryWalletManagerBacking();
      SecureKeyValueStore secureKeyValueStore1 = new SecureKeyValueStore(backing1, _randomSource);
      final WalletManager wallet1 = new WalletManager(secureKeyValueStore1, backing1, network, wapi, null);

      try {
         Bip39.MasterSeed masterSeed = Bip39.generateSeedFromWordList(new String[]{"salmon", "army", "unique", "urge", "robot", "view", "vote", "milk", "once", "mansion", "type", "unable"}, null);
         wallet1.configureBip32MasterSeed(masterSeed, cipher);
      } catch (InvalidKeyCipher e) {
         throw new RuntimeException(e);
      }

      wallet1.createAdditionalBip44Account(cipher);
      print("Wallet 1 account 0 receiving address 0: " + wallet1.getActiveAccounts().get(0).getReceivingAddress().get().toString());

      WalletManagerBacking backing2 = new InMemoryWalletManagerBacking();
      SecureKeyValueStore secureKeyValueStore2 = new SecureKeyValueStore(backing2, _randomSource);
      final WalletManager wallet2 = new WalletManager(secureKeyValueStore2, backing2, network, wapi, null);

      try {
         Bip39.MasterSeed masterSeed = Bip39.generateSeedFromWordList(new String[]{"erosion", "intact", "atom", "water", "clap", "chef", "wool", "section", "busy", "elevator", "weekend", "diary"}, null);
         wallet2.configureBip32MasterSeed(masterSeed, cipher);
      } catch (InvalidKeyCipher e) {
         throw new RuntimeException(e);
      }

      wallet2.createAdditionalBip44Account(cipher);

      print("Wallet 2 account 0 receiving address 0: " + wallet2.getActiveAccounts().get(0).getReceivingAddress().get().toString());

      Observer observer = new Observer() {

         @Override
         public void onWalletStateChanged(WalletManager wallet, State state) {
            System.out.println("Wallet 1 state changed to: " + state.toString());
            if (state == State.READY) {
               printWallet(wallet);
               WalletManager foreign;
               if (wallet == wallet1) {
                  foreign = wallet2;
               } else {
                  foreign = wallet1;
               }
               Address address = foreign.getActiveAccounts().get(0).getReceivingAddress().get();
               WalletAccount myAccount = wallet.getActiveAccounts().get(0);
               List<Receiver> receivers = new ArrayList<Receiver>();
               receivers.add(new Receiver(address, 10000 * (1 + new Random().nextInt(100))));
               try {
                  UnsignedTransaction unsigned = myAccount.createUnsignedTransaction(receivers, TransactionUtils.DEFAULT_KB_FEE);
                  Transaction transaction = myAccount.signTransaction(unsigned, AesKeyCipher.defaultKeyCipher());
                  myAccount.broadcastTransaction(transaction);
                  wallet.startSynchronization();
               } catch (OutputTooSmallException e) {
                  throw new RuntimeException(e);
               } catch (InsufficientFundsException e) {
                  print("No more funds");
               } catch (InvalidKeyCipher e) {
                  throw new RuntimeException(e);
               }
            }
         }

         @Override
         public void onAccountEvent(WalletManager wallet, UUID accountId, Event event) {
            switch (event) {
               case SERVER_CONNECTION_ERROR:
                  print("EVENT: Unable to establish connection");
                  break;
               case BROADCASTED_TRANSACTION_ACCEPTED:
                  print("EVENT: Transaction broadcasted successfully");
                  break;
               case BROADCASTED_TRANSACTION_DENIED:
                  print("EVENT: One of your transactions were rejected by the network. You may have another wallet using the same bitcoin addresses. You should resynchronize your wallet now.");
                  break;
               case BALANCE_CHANGED:
                  print("EVENT: Balance changed");
                  break;
               case TRANSACTION_HISTORY_CHANGED:
                  print("EVENT: Transaction history changed");
                  break;
               case RECEIVING_ADDRESS_CHANGED:
                  print("EVENT: Receiving address changed");
                  break;
               default:
                  print("Unknown event" + event.toString());
            }
         }
      };

      wallet1.addObserver(observer);
      wallet2.addObserver(observer);

      wallet1.startSynchronization();
      wallet2.startSynchronization();

      while (true) {
         try {
            Thread.sleep(100);
         } catch (InterruptedException e) {
            return;
         }
      }
   }

   private static void simple() throws WapiException, InvalidKeyCipher {
      NetworkParameters network = NetworkParameters.testNetwork;

      String myceliumThumbprint = "E5:70:76:B2:67:3A:89:44:7A:48:14:81:DF:BD:A0:58:C8:82:72:4F";
      HttpEndpoint endpoint = new HttpsEndpoint("https://node3.mycelium.com/wapitestnet", myceliumThumbprint);

      WapiLogger logger = new WapiLogger() {

         @Override
         public void logError(String message) {
            System.err.println(message);
         }

         @Override
         public void logError(String message, Exception e) {
            System.err.println(message);
            e.printStackTrace();
         }

         @Override
         public void logInfo(String message) {
            System.out.println(message);
         }
      };

      ServerEndpoints serverEndpoints = new ServerEndpoints(new HttpEndpoint[]{endpoint});
      WapiClient wapi = new WapiClient(serverEndpoints, logger, "sample");

      AesKeyCipher cipher = AesKeyCipher.defaultKeyCipher();

      WalletManagerBacking backing = new InMemoryWalletManagerBacking();

      SecureKeyValueStore secureKeyValueStore = new SecureKeyValueStore(backing, _randomSource);
      // Create wallet
      WalletManager wallet = new WalletManager(secureKeyValueStore, backing, network, wapi, null);

      // Add a single key account
      createSingleKeyAccountFromStringSeed(wallet, "some random string", cipher, network, wapi, logger);

      wallet.addObserver(new Observer() {

         @Override
         public void onWalletStateChanged(WalletManager wallet, State state) {
            System.out.println("Wallet state changed to: " + state.toString());
            if (state == State.READY) {
               printWallet(wallet);
            }
         }

         @Override
         public void onAccountEvent(WalletManager wallet, UUID accountId, Event event) {
            switch (event) {
               case SERVER_CONNECTION_ERROR:
                  print("EVENT: Unable to establish connection");
                  break;
               case BROADCASTED_TRANSACTION_ACCEPTED:
                  print("EVENT: Transaction broadcasted successfully");
                  break;
               case BROADCASTED_TRANSACTION_DENIED:
                  print("EVENT: One of your transactions were rejected by the network. You may have another wallet using the same bitcoin addresses. You should resynchronize your wallet now.");
                  break;
               case BALANCE_CHANGED:
                  print("EVENT: Balance changed");
                  break;
               case TRANSACTION_HISTORY_CHANGED:
                  print("EVENT: Transaction history changed");
                  break;
               case RECEIVING_ADDRESS_CHANGED:
                  print("EVENT: Receiving address changed");
                  break;
               default:
                  print("Unknown event" + event.toString());
            }
         }
      });

      importMasterSeed(wallet, cipher);
      addBip44Account(wallet, cipher);

      printMenu();

      while (true) {
         String cmd = readLine();
         if (cmd.equals("1")) {
            printWallet(wallet);
         } else if (cmd.equals("2")) {
            wallet.startSynchronization();
         } else if (cmd.equals("3")) {
            sendTransaction(wallet, cipher);
         } else if (cmd.equals("4")) {
            printTransactionHistory(wallet);
         } else if (cmd.equals("5")) {
            importMasterSeed(wallet, cipher);
         } else if (cmd.equals("6")) {
            addBip44Account(wallet, cipher);
         } else if (cmd.equals("x")) {
            return;
         }
         printMenu();
      }
   }

   private static void importMasterSeed(WalletManager wallet, KeyCipher cipher) {
      if (wallet.hasBip32MasterSeed()) {
         print("Master seed already loaded");
         return;
      }

      // Create master seed
      Bip39.MasterSeed masterSeed = Bip39.generateSeedFromWordList("degree rain vendor coffee push math onion inside pyramid blush stick treat".split(" "), "");
      // Configure master seed
      try {
         wallet.configureBip32MasterSeed(masterSeed, cipher);
      } catch (InvalidKeyCipher e) {
         throw new RuntimeException(e);
      }
   }

   private static void createSingleKeyAccountFromStringSeed(WalletManager walletManager, String string,
                                                            KeyCipher cipher, NetworkParameters network, Wapi wapi, WapiLogger logger) throws InvalidKeyCipher {
      InMemoryPrivateKey privateKey = new InMemoryPrivateKey(HashUtils.sha256(string.getBytes()).getBytes());
      walletManager.createSingleAddressAccount(privateKey, cipher);
   }

   private static void addBip44Account(WalletManager walletManager, KeyCipher cipher) throws InvalidKeyCipher {
      if (!walletManager.hasBip32MasterSeed()) {
         print("Master seed not imported");
         return;
      }
      if (!walletManager.canCreateAdditionalBip44Account()) {
         print("Can only have one HD account which is unused");
         return;
      }
      walletManager.createAdditionalBip44Account(cipher);
   }

   private static void printTransactionHistory(WalletManager wallet) {
      for (WalletAccount account : wallet.getActiveAccounts()) {
         print("Wallet: " + account.getId());
         List<TransactionSummary> list = account.getTransactionHistory(0, 5000);
         int index = 0;
         for (TransactionSummary s : list) {
            print("#"+(++index)+" Value: " + s.value + " Confirmations: " + s.confirmations + " Time: "
                  + new Date(s.time * 1000).toString() + " ID: " + s.txid.toString());
         }
      }
   }

   private static void sendTransaction(WalletManager wallet, KeyCipher cipher) throws InvalidKeyCipher {

      // Choosing account
      List<WalletAccount> accounts = wallet.getActiveAccounts();
      WalletAccount account;
      if (accounts.size() == 0) {
         print("no active accounts");
         return;
      } else if (accounts.size() == 1) {
         account = accounts.get(0);
      } else {
         print("Choose account index: " + accounts.toString());
         int index = 0;
         try {
            index = Integer.parseInt(readLine());
            if (index < 0 || index >= accounts.size()) {
               print("invalid account");
               return;
            }
         } catch (NumberFormatException e) {
            print("invalid account");
            return;
         }
         account = accounts.get(index);
      }
      // Determine max amount
      ExactBitcoinValue exactCurrencyValue = (ExactBitcoinValue) account.calculateMaxSpendableAmount(TransactionUtils.DEFAULT_KB_FEE);
      long maxAmount = exactCurrencyValue.getAsBitcoin().getLongValue();
      if (maxAmount == 0) {
         print("Not enough funds");
         return;
      }

      // Entering address
      print("Enter receiving address: ");
      Address address = Address.fromString(readLine(), account.getNetwork());
      if (address == null) {
         print("Invalid address");
         return;
      }

      // Entering amount
      print("Enter amount (max=" + Long.toString(maxAmount) + "): ");
      long amount;
      try {
         amount = Long.parseLong(readLine());
         if (amount > maxAmount || amount < TransactionUtils.MINIMUM_OUTPUT_VALUE) {
            print("invalid amount");
            return;
         }
      } catch (NumberFormatException e) {
         print("invalid amount");
         return;
      }

      List<Receiver> receivers = new LinkedList<Receiver>();
      receivers.add(new Receiver(address, amount));
      UnsignedTransaction unsigned;
      try {
         unsigned = account.createUnsignedTransaction(receivers, TransactionUtils.DEFAULT_KB_FEE);
      } catch (OutputTooSmallException e) {
         print("invalid amount");
         return;
      } catch (InsufficientFundsException e) {
         print("insufficient funds");
         return;
      }

      print("fee: " + unsigned.calculateFee());

      Transaction tx = account.signTransaction(unsigned, cipher);
      account.broadcastTransaction(tx);
      print("Transaction " + tx.getHash().toString() + " queued");
   }

   private static void printMenu() {
      print("1. print");
      print("2. synchronize");
      print("3. send transaction");
      print("4. transaction history");
      print("5. import master seed");
      print("6. add HD account");
      print("x. quit");
   }

   private static void print(String s) {
      System.out.println(s);
   }

   private static void printWallet(WalletManager wallet) {
      System.out.println(wallet.toString());
      for (WalletAccount account : wallet.getActiveAccounts()) {
         if (account != null) {
            System.out.println(account.toString());
         }
      }
   }

   private static String readLine() {
      BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
      try {
         String s = br.readLine();
         if (s == null) {
            return "";
         }
         return s.trim();
      } catch (IOException e) {
         throw new RuntimeException(e);
      }

   }

   public static void tryQueryUnspentOutputs(WapiClient wapi) throws WapiException {

      List<Address> addresses = new LinkedList<Address>();
      addresses.add(Address.fromString("muHct3YZ9Nd5Pq7uLYYhXRAxeW4EnpcaLz"));
      QueryUnspentOutputsRequest request = new QueryUnspentOutputsRequest(Wapi.VERSION, addresses);
      QueryUnspentOutputsResponse response = wapi.queryUnspentOutputs(request).getResult();
      for (TransactionOutputEx out : response.unspent) {
         System.out.println(out);
      }
   }

   public static void tryQueryTransactionInventory(WapiClient wapi) throws WapiException {
      List<Address> addresses = new LinkedList<Address>();
      addresses.add(Address.fromString("moXV3ZY5QtQqfRgdfZyWRbR2h5GzuEE9BC"));
      QueryTransactionInventoryRequest request = new QueryTransactionInventoryRequest(Wapi.VERSION, addresses, 50);
      QueryTransactionInventoryResponse response = wapi.queryTransactionInventory(request).getResult();
      for (Sha256Hash txid : response.txIds) {
         System.out.println(txid);
      }
   }

   public static void tryGetTransactions(WapiClient wapi) throws WapiException {
      // Get a transaction
      List<Sha256Hash> txids = new LinkedList<Sha256Hash>();
      txids.add(Sha256Hash.fromString("0e349a53cfc94b4454f10cd7e408958a64e11a8ec7f4bd2a3b837cde4c990185"));
      txids.add(Sha256Hash.fromString("1aa58059cdc4861c6fe16838eea986b12960fb7d0d22fcbef1cf5e61c5ead3dc"));
      GetTransactionsResponse response = wapi.getTransactions(new GetTransactionsRequest(Wapi.VERSION, txids))
            .getResult();
      for (TransactionEx tx : response.transactions) {
         System.out.println(tx.toString());
      }
   }

   private static class MyRandomSource implements RandomSource {
      SecureRandom _rnd;

      public MyRandomSource() {
         _rnd = new SecureRandom();
      }

      @Override
      public void nextBytes(byte[] bytes) {
         _rnd.nextBytes(bytes);
      }

   }

}
