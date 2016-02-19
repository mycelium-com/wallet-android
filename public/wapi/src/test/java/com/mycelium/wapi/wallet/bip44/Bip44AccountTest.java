package com.mycelium.wapi.wallet.bip44;

import com.mrd.bitlib.crypto.Bip39;
import com.mrd.bitlib.crypto.RandomSource;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.NetworkParameters;
import com.mrd.bitlib.util.Sha256Hash;
import com.mycelium.wapi.api.Wapi;
import com.mycelium.WapiLogger;
import com.mycelium.wapi.api.WapiResponse;
import com.mycelium.wapi.api.lib.TransactionExApi;
import com.mycelium.wapi.api.request.*;
import com.mycelium.wapi.api.response.*;
import com.mycelium.wapi.model.ExchangeRate;
import com.mycelium.wapi.model.TransactionOutputEx;
import com.mycelium.wapi.model.TransactionStatus;
import com.mycelium.wapi.wallet.*;
import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

public class Bip44AccountTest {

   private static final String MASTER_SEED_WORDS = "degree rain vendor coffee push math onion inside pyramid blush stick treat";
   private static final String MASTER_SEED_512_A0_R0_ADDRESS = "1F1QAzNLutBEuB4QZLXghqu6PdxEFdb2PV";
   private static final String MASTER_SEED_512_A0_C0_ADDRESS = "1PGrHHNjVXBr8JJhg9zRQVFvmUSu9XsMeV";

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
         GetTransactionsResponse response = new GetTransactionsResponse(new ArrayList<TransactionExApi>());
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

      @Override
      public WapiResponse<QueryExchangeRatesResponse> queryExchangeRates(QueryExchangeRatesRequest request) {
         QueryExchangeRatesResponse response = new QueryExchangeRatesResponse(request.currency, new ExchangeRate[]{});
         return new WapiResponse<QueryExchangeRatesResponse>(response);
      }

      @Override
      public WapiResponse<PingResponse> ping() {
         return new WapiResponse<PingResponse>(new PingResponse("junit test"));
      }

      @Override
      public WapiResponse<ErrorCollectorResponse> collectError(ErrorCollectorRequest request) {
         ErrorCollectorResponse response = new ErrorCollectorResponse();
         return new WapiResponse<ErrorCollectorResponse>(response);
      }

      @Override
      public WapiResponse<VersionInfoResponse> getVersionInfo(VersionInfoRequest request) {
         VersionInfoResponse response = null;
         try {
            response = new VersionInfoResponse("1.0.0-JUNIT",new URI("https://example.com"),"Fake response from junit");
            return new WapiResponse<VersionInfoResponse>(response);
         } catch (URISyntaxException e) {
            throw new RuntimeException(e);
         }
      }

      @Override
      public WapiResponse<VersionInfoExResponse> getVersionInfoEx(VersionInfoExRequest request) {
         return null;
      }

      @Override
      public WapiResponse<MinerFeeEstimationResponse> getMinerFeeEstimations() {
         throw new UnsupportedOperationException();
      }
   }

   /**
    * Test that the first two addresses we generate agree with a specific seed agree with Wallet32
    */
   @Test
   public void addressGenerationTest() throws KeyCipher.InvalidKeyCipher {
      SecureKeyValueStore store = new SecureKeyValueStore(new InMemoryWalletManagerBacking(), new MyRandomSource());
      KeyCipher cipher = AesKeyCipher.defaultKeyCipher();

      WalletManagerBacking backing = new InMemoryWalletManagerBacking();

      // Determine the next BIP44 account index
      int accountIndex = 0;

      Bip39.MasterSeed masterSeed = Bip39.generateSeedFromWordList(MASTER_SEED_WORDS.split(" "), "");

      WalletManager walletManager = new WalletManager(store, backing, NetworkParameters.productionNetwork, new FakeWapi(), null);

      walletManager.configureBip32MasterSeed(masterSeed, cipher);

      UUID account1Id = walletManager.createAdditionalBip44Account(cipher);

      Bip44Account account1 = (Bip44Account) walletManager.getAccount(account1Id);

      assertEquals(Address.fromString(MASTER_SEED_512_A0_R0_ADDRESS), account1.getReceivingAddress().get());
      assertEquals(Address.fromString(MASTER_SEED_512_A0_C0_ADDRESS), account1.getChangeAddress());
   }


}
