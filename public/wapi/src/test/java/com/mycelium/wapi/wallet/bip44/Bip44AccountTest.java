package com.mycelium.wapi.wallet.bip44;

import com.mrd.bitlib.crypto.RandomSource;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.NetworkParameters;
import com.mrd.bitlib.util.HexUtils;
import com.mrd.bitlib.util.Sha256Hash;
import com.mycelium.wapi.api.Wapi;
import com.mycelium.wapi.api.WapiLogger;
import com.mycelium.wapi.api.WapiResponse;
import com.mycelium.wapi.api.request.*;
import com.mycelium.wapi.api.response.*;
import com.mycelium.wapi.model.TransactionEx;
import com.mycelium.wapi.model.TransactionOutputEx;
import com.mycelium.wapi.model.TransactionStatus;
import com.mycelium.wapi.wallet.*;
import org.junit.Ignore;
import org.junit.Test;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

public class Bip44AccountTest {

   private static final String MASTER_SEED_512 = "2e8905819b8723fe2c1d161860e5ee1830318dbf49a83bd451cfb8440c28bd6fa457fe1296106559a3c80937a1c1069be3a3a5bd381ee6260e8d9739fce1f607";
   private static final String MASTER_SEED_512_A0_R0_ADDRESS = "18MNH3xiSXsYNYxCvwQ6JouW2RkWwStSwy";
   private static final String MASTER_SEED_512_A0_C0_ADDRESS = "1AaypjAW4QtkFNiQ51caqKz21d1Tc7Z5wY";

   /**
    * A fake random source that gets our tests going
    */
   private static class MyRandomSource implements RandomSource {
      SecureRandom _rnd;

      public MyRandomSource() {
         _rnd = new SecureRandom(new byte[]{42});
      }

      @Override
      public void nextBytes(byte[] bytes) {
         _rnd.nextBytes(bytes);
      }

   }

   /**
    * A fake Wapi implementation that gets our tests going
    */
   private static class FakeWapi implements Wapi {

      @Override
      public WapiLogger getLogger() {
         return new WapiLogger() {
            @Override
            public void logError(String message, Exception e) {
               System.err.println(message);
               System.err.println(e.toString());
            }

            @Override
            public void logError(String message) {
               System.err.println(message);
            }

            @Override
            public void logInfo(String message) {
               System.out.println(message);
            }
         };
      }

      @Override
      public WapiResponse<QueryUnspentOutputsResponse> queryUnspentOutputs(QueryUnspentOutputsRequest request) {
         QueryUnspentOutputsResponse response = new QueryUnspentOutputsResponse(0, new ArrayList<TransactionOutputEx>());
         return new WapiResponse<QueryUnspentOutputsResponse>(response);
      }

      @Override
      public WapiResponse<QueryTransactionInventoryResponse> queryTransactionInventory(QueryTransactionInventoryRequest request) {
         QueryTransactionInventoryResponse response = new QueryTransactionInventoryResponse(0, new ArrayList<Sha256Hash>());
         return new WapiResponse<QueryTransactionInventoryResponse>(response);
      }

      @Override
      public WapiResponse<GetTransactionsResponse> getTransactions(GetTransactionsRequest request) {
         GetTransactionsResponse response = new GetTransactionsResponse(new ArrayList<TransactionEx>());
         return new WapiResponse<GetTransactionsResponse>(response);
      }

      @Override
      public WapiResponse<BroadcastTransactionResponse> broadcastTransaction(BroadcastTransactionRequest request) {
         BroadcastTransactionResponse response = new BroadcastTransactionResponse(true, Sha256Hash.ZERO_HASH);
         return new WapiResponse<BroadcastTransactionResponse>(response);
      }

      @Override
      public WapiResponse<CheckTransactionsResponse> checkTransactions(CheckTransactionsRequest request) {
         CheckTransactionsResponse response = new CheckTransactionsResponse(new ArrayList<TransactionStatus>());
         return new WapiResponse<CheckTransactionsResponse>(response);
      }
   }

   /**
    * Test that the first two addresses we generate agree with a specific seed agree with Wallet32
    */
   @Test
   //todo fix  walletManager.configureBip32MasterSeed(masterSeed, cipher);

   @Ignore
   public void addressGenerationTest() throws KeyCipher.InvalidKeyCipher {
      SecureKeyValueStore store = new SecureKeyValueStore(new InMemoryWalletManagerBacking(), new MyRandomSource());
      KeyCipher cipher = AesKeyCipher.defaultKeyCipher();

      WalletManagerBacking backing = new InMemoryWalletManagerBacking();

      // Determine the next BIP44 account index
      int accountIndex = 0;

      byte[] masterSeed = HexUtils.toBytes(MASTER_SEED_512);

      WalletManager walletManager = new WalletManager(store, backing, NetworkParameters.productionNetwork, new FakeWapi());

//      walletManager.configureBip32MasterSeed(masterSeed, cipher);

      UUID account1Id = walletManager.createAdditionalBip44Account(cipher);

      Bip44Account account1 = (Bip44Account) walletManager.getAccount(account1Id);

      assertEquals(account1.getReceivingAddress(), Address.fromString(MASTER_SEED_512_A0_R0_ADDRESS));
      assertEquals(account1.getChangeAddress(), Address.fromString(MASTER_SEED_512_A0_C0_ADDRESS));
   }


}
