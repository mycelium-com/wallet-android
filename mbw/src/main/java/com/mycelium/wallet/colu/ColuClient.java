package com.mycelium.wallet.colu;

import com.google.api.client.http.*;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.http.json.JsonHttpContent;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.Base64;
import com.google.common.base.Stopwatch;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.NetworkParameters;
import com.mrd.bitlib.model.Transaction;
import com.mrd.bitlib.util.HexUtils;
import com.mycelium.WapiLogger;
import com.mycelium.wallet.AdvancedHttpClient;
import com.mycelium.wallet.colu.json.AddressInfo;
import com.mycelium.wallet.colu.json.AddressTransactionsInfo;
import com.mycelium.wallet.colu.json.Asset;
import com.mycelium.wallet.colu.json.ColuBroadcastTxHex;
import com.mycelium.wallet.colu.json.ColuBroadcastTxId;
import com.mycelium.wallet.colu.json.ColuTransactionRequest;
import com.mycelium.wallet.colu.json.ColuTxDest;
import com.mycelium.wallet.colu.json.ColuTxFlags;
import com.mycelium.wallet.colu.json.Utxo;
import com.mycelium.wapi.wallet.currency.ExactCurrencyValue;

import org.json.JSONException;
import org.json.JSONObject;
import org.spongycastle.jce.provider.BouncyCastleProvider;

import android.util.Log;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.security.*;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Client for the Colu HTTP API.
 */
public class ColuClient {

   private static final String TAG = "ColuClient";

   public static final boolean coluAutoSelectUtxo = true;

   public NetworkParameters network;

   private AdvancedHttpClient coloredCoinsClient;
   private AdvancedHttpClient blockExplorerClient;

   private static String[] MAINNET_COLOREDCOINS_API_URLS = {"https://coloredcoinsd.gear.mycelium.com/v3/", "https://api.coloredcoins.org/v3/"};
   private static String[] TESTNET_COLOREDCOINS_API_URLS = {"http://testnet.api.coloredcoins.org/v3/"};
   private static String[] MAINNET_COLU_BLOCK_EXPLORER_URLS = {"https://coloredcoins.gear.mycelium.com/api/", "https://explorer.coloredcoins.org/api/"};
   private static String[] TESTNET_COLU_BLOCK_EXPLORER_URLS = {"http://testnet.explorer.coloredcoins.org/api/"};

   public ColuClient(NetworkParameters network) {
      if (network.isTestnet()) {
         this.coloredCoinsClient = new AdvancedHttpClient(TESTNET_COLOREDCOINS_API_URLS);
         this.blockExplorerClient = new AdvancedHttpClient(TESTNET_COLU_BLOCK_EXPLORER_URLS);
      }
      else {
         this.coloredCoinsClient = new AdvancedHttpClient(MAINNET_COLOREDCOINS_API_URLS);
         this.blockExplorerClient = new AdvancedHttpClient(MAINNET_COLU_BLOCK_EXPLORER_URLS);
      }

      this.network = network;

      // Level.CONFIG logs everything but Authorization header
      // Level.ALL also logs Authorization header
      // Type this to really enable: adb shell setprop log.tag.HttpTransport DEBUG
      Logger.getLogger(HttpTransport.class.getName()).setLevel(Level.CONFIG);
      initialize();
   }

   private void initialize() {
      Security.addProvider(new BouncyCastleProvider());
   }

   public AddressInfo.Json getBalance(Address address) throws IOException {
      String endpoint = "addressinfo/" + address.toString();
      return blockExplorerClient.sendGetRequest(AddressInfo.Json.class, endpoint);
   }

   public AddressTransactionsInfo.Json getAddressTransactions(Address address) throws IOException {
      String endpoint = "getaddressinfowithtransactions?address=" + address.toString();
      return blockExplorerClient.sendGetRequest(AddressTransactionsInfo.Json.class, endpoint);
   }

   //TODO: move most of the logic to ColuManager
   public ColuBroadcastTxHex.Json prepareTransaction(Address destAddress, List<Address> src,
                                                     ExactCurrencyValue nativeAmount, ColuAccount coluAccount,
                                                     long txFee)
           throws IOException {
      Log.d(TAG, "prepareTransaction");
      if (destAddress == null) {
         Log.e(TAG, "destAddress is null");
         return null;
      }
      if (src == null || src.size() == 0) {
         Log.e(TAG, "src is null or empty");
         return null;
      }
      if (nativeAmount == null) {
         Log.e(TAG, "nativeAmount is null");
         return null;
      }
      Log.d(TAG, "destAddress=" + destAddress.toString() + " src nb addr=" + src.size() + " src0=" + src.get(0).toString() + " nativeAmount=" + nativeAmount.toString());
      Log.d(TAG, " txFee=" + txFee);
      ColuTransactionRequest.Json request = new ColuTransactionRequest.Json();
      List<ColuTxDest.Json> to = new LinkedList<ColuTxDest.Json>();
      ColuTxDest.Json dest = new ColuTxDest.Json();
      dest.address = destAddress.toString();
      BigDecimal amountAssetSatoshi = (nativeAmount.getValue().multiply(new BigDecimal(10).pow(coluAccount.getColuAsset().scale)));
      dest.amount = amountAssetSatoshi.longValue();
      dest.assetId = coluAccount.getColuAsset().id;
      to.add(dest);

      request.to = to;
      request.fee = txFee;

      ColuTxFlags.Json flags = new ColuTxFlags.Json();
      flags.splitChange = true;
      request.flags = flags;

      // v1: let colu chose source tx
      if (ColuClient.coluAutoSelectUtxo) {
         LinkedList<String> from = new LinkedList<String>();
         for (Address addr : src) {
            from.add(addr.toString());
         }
         request.from = from;
         request.financeOutputTxid = "";
      } else {
         // v2: chose utxo ourselves
         LinkedList<String> sendutxo = new LinkedList<String>();
         double selectedAmount = 0;
         double selectedSatoshiAmount = 0;
         for (Address addr : src) {
            Log.d(TAG, "Selected address " + addr.toString());
            // get list of address utxo and filter out those who have asset
            List<Utxo.Json> addressUnspent = coluAccount.getAddressUnspent(addr.toString());
            Log.d(TAG, "addressUnspent.size=" + addressUnspent.size());
            for (Utxo.Json utxo : addressUnspent) {
               Log.d(TAG, "Processing " + utxo.txid + ":" + utxo.index);

               // case 1: this is a BTC/satoshi utxo, we select it for fee finance
               // Colu server will only take as much as it needs from the utxo we send it
               if (utxo.assets == null || utxo.assets.size() == 0) {
                  Log.d(TAG, "utxo without asset, use it for fee ");
                  Log.d(TAG, "txid: " + utxo.txid + ":" + utxo.index + " value: " + utxo.value);
                  sendutxo.add(utxo.txid + ":" + utxo.index);
                  selectedSatoshiAmount = selectedSatoshiAmount + utxo.value;
               }
               // case 2: asset utxo. If it is of the type we care, and we need more, select it.
               for (Asset.Json asset : utxo.assets) {
                  Log.d(TAG, "Evaluating asset " + asset.assetId);
                  if (asset.assetId.compareTo(coluAccount.getColuAsset().id) == 0) {
                     if (selectedAmount < dest.amount) {
                        sendutxo.add(utxo.txid + ":" + utxo.index);
                        selectedAmount = selectedAmount + asset.amount;
                        Log.d(TAG, "Selected output " + utxo.txid + ":" + utxo.index +
                                " and added amount " + asset.amount);
                        Log.d(TAG, "Current amount is " + selectedAmount);
                     }
                  } else if (asset.assetId.isEmpty() || asset.assetId.compareTo("") == 0) {
                     Log.d(TAG, "utxo with empty asset, use it for fee ");
                     Log.d(TAG, "txid: " + utxo.txid + ":" + utxo.index + " value: " + utxo.value);
                     sendutxo.add(utxo.txid + ":" + utxo.index);
                     selectedSatoshiAmount = selectedSatoshiAmount + utxo.value;
                  }
               }
            }  // end for
         }
         request.sendutxo = sendutxo;
         // do we need to set this one as well ?
         request.financeOutputTxid = "";
      }
      return coloredCoinsClient.sendPostRequest(ColuBroadcastTxHex.Json.class, "sendasset", null, request);
   }

   public static String bytesToHex(byte[] in) {
      final StringBuilder builder = new StringBuilder();
      for (byte b : in) {
         builder.append(String.format("%02x", b));
      }
      return builder.toString();
   }

   public ColuBroadcastTxId.Json broadcastTransaction(Transaction coluSignedTransaction) throws IOException {
      ColuBroadcastTxHex.Json tx = new ColuBroadcastTxHex.Json();
      byte[] signedTr = coluSignedTransaction.toBytes();
      tx.txHex = bytesToHex(signedTr);
      Log.d(TAG, "broadcastTransaction: hexbytes=" + tx.txHex);
      return coloredCoinsClient.sendPostRequest(ColuBroadcastTxId.Json.class, "broadcast", null, tx);
   }
}