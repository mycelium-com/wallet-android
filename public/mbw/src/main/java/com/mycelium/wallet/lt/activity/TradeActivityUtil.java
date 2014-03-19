package com.mycelium.wallet.lt.activity;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.common.base.Preconditions;
import com.mrd.bitlib.StandardTransactionBuilder;
import com.mrd.bitlib.StandardTransactionBuilder.InsufficientFundsException;
import com.mrd.bitlib.StandardTransactionBuilder.OutputTooSmallException;
import com.mrd.bitlib.StandardTransactionBuilder.UnsignedTransaction;
import com.mrd.bitlib.TransactionUtils;
import com.mrd.bitlib.crypto.PrivateKeyRing;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.NetworkParameters;
import com.mrd.bitlib.model.Transaction;
import com.mrd.bitlib.model.UnspentTransactionOutput;
import com.mycelium.lt.api.model.PriceFormula;
import com.mycelium.lt.api.model.TradeSession;
import com.mycelium.wallet.Constants;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.Wallet;
import com.mycelium.wallet.Wallet.SpendableOutputs;

public class TradeActivityUtil {

   public static boolean canAffordTrade(TradeSession tradeSession, MbwManager mbwManager) {
      // Create default wallet
      Wallet wallet = mbwManager.getRecordManager().getWallet(mbwManager.getWalletMode());

      // Get spendable outputs
      SpendableOutputs spendable = wallet.getLocalSpendableOutputs(mbwManager.getBlockChainAddressTracker());

      // Extract private key ring
      PrivateKeyRing keyRing = wallet.getPrivateKeyRing();

      return createUnsignedTransaction(tradeSession, mbwManager, spendable, keyRing, mbwManager.getNetwork()) != null;
   }

   public static Transaction createSignedTransaction(TradeSession tradeSession, MbwManager mbwManager) {
      // Create default wallet
      Wallet wallet = mbwManager.getRecordManager().getWallet(mbwManager.getWalletMode());

      // Get spendable outputs
      SpendableOutputs spendable = wallet.getLocalSpendableOutputs(mbwManager.getBlockChainAddressTracker());

      // Extract private key ring
      PrivateKeyRing keyRing = wallet.getPrivateKeyRing();

      // Create unsigned transaction
      UnsignedTransaction unsigned = createUnsignedTransaction(tradeSession, mbwManager, spendable, keyRing,
            mbwManager.getNetwork());

      if (unsigned == null) {
         return null;
      }

      // Make signatures
      List<byte[]> signatures = StandardTransactionBuilder.generateSignatures(unsigned.getSignatureInfo(), keyRing,
            mbwManager.getRecordManager().getRandomSource());

      // Sign transaction
      Transaction tx = StandardTransactionBuilder.finalizeTransaction(unsigned, signatures);
      return tx;
   }

   private static UnsignedTransaction createUnsignedTransaction(TradeSession tradeSession, MbwManager mbwManager,
         SpendableOutputs spendable, PrivateKeyRing keyRing, NetworkParameters network) {
      Preconditions.checkArgument(tradeSession.satoshisForBuyer > TransactionUtils.MINIMUM_OUTPUT_VALUE);
      Preconditions.checkArgument(tradeSession.satoshisFromSeller >= tradeSession.satoshisForBuyer);
      long localTraderFee = tradeSession.satoshisFromSeller - tradeSession.satoshisForBuyer;

      Address receiverAddress = tradeSession.buyerAddress;
      Address localTraderFeeAddress = tradeSession.feeAddress;

      // Construct list of spendable outputs
      List<UnspentTransactionOutput> outputs = new LinkedList<UnspentTransactionOutput>();
      outputs.addAll(spendable.unspent);
      outputs.addAll(spendable.change);

      // Create unsigned empty transaction
      StandardTransactionBuilder stb = new StandardTransactionBuilder(network);

      // Add the outputs
      try {
         stb.addOutput(receiverAddress, tradeSession.satoshisForBuyer);

         if (localTraderFee >= TransactionUtils.MINIMUM_OUTPUT_VALUE) {
            stb.addOutput(localTraderFeeAddress, localTraderFee);
         }
      } catch (OutputTooSmallException e) {
         // This should not happen as we have checked it above in a precondition
         Log.e("TradeActivityUtil", "Unexpected OutputTooSmallException exception");
         return null;
      }

      // Create the unsigned transaction
      try {
         return stb.createUnsignedTransaction(outputs, null, keyRing, network);
      } catch (InsufficientFundsException e) {
         return null;
      }

   }

   public static void populatePriceDetails(Context context, View root, boolean isBuyer, boolean isSelf,
         String currency, PriceFormula priceFormula, long satoshisAtMarketPrice, long satoshisTraded, int fiatTraded,
         MbwManager mbwManager) {
      Locale locale = new Locale("en", "US");

      // Price
      double oneBtcPrice = (double) fiatTraded * Constants.ONE_BTC_IN_SATOSHIS / (double) satoshisTraded;
      String price = Utils.getFiatValueAsString(Constants.ONE_BTC_IN_SATOSHIS, oneBtcPrice);
      ((TextView) root.findViewById(R.id.tvPriceValue)).setText(context.getResources().getString(R.string.lt_btc_price,
            price, currency));

      String fiatAmountString = String.format(locale, "%s %s", fiatTraded, currency);
      String btcAmountString = mbwManager.getBtcValueString(satoshisTraded);

      // You Pay / You Get Values
      if (isBuyer) {
         ((TextView) root.findViewById(R.id.tvPayValue)).setText(fiatAmountString);
         ((TextView) root.findViewById(R.id.tvGetValue)).setText(btcAmountString);
      } else {
         ((TextView) root.findViewById(R.id.tvPayValue)).setText(btcAmountString);
         ((TextView) root.findViewById(R.id.tvGetValue)).setText(fiatAmountString);
      }
      // You Pay / You Get Labels
      if (isSelf) {
         if (isBuyer) {
            ((TextView) root.findViewById(R.id.tvPriceLabel)).setText(R.string.lt_you_buy_at_label);
         } else {
            ((TextView) root.findViewById(R.id.tvPriceLabel)).setText(R.string.lt_you_sell_at_label);
         }
         ((TextView) root.findViewById(R.id.tvPayLabel)).setText(R.string.lt_you_pay_label);
         ((TextView) root.findViewById(R.id.tvGetLabel)).setText(R.string.lt_you_get_label);
      } else {
         if (isBuyer) {
            ((TextView) root.findViewById(R.id.tvPriceLabel)).setText(R.string.lt_buyer_buys_at_label);
            ((TextView) root.findViewById(R.id.tvPayLabel)).setText(R.string.lt_buyer_pays_label);
            ((TextView) root.findViewById(R.id.tvGetLabel)).setText(R.string.lt_buyer_gets_label);
         } else {
            ((TextView) root.findViewById(R.id.tvPriceLabel)).setText(R.string.lt_seller_sells_at_label);
            ((TextView) root.findViewById(R.id.tvPayLabel)).setText(R.string.lt_seller_pays_label);
            ((TextView) root.findViewById(R.id.tvGetLabel)).setText(R.string.lt_seller_gets_label);
         }
      }

   }

   @SuppressWarnings("unused")
   private static double calculatePremium(long satoshisTraded, long satoshisAtMarketPrice) {
      double traded = (double) satoshisTraded;
      double market = (double) satoshisAtMarketPrice;
      double premium = 1D - traded / market;
      double premiumInPercent = premium * 100;
      return premiumInPercent;
   }

   @SuppressWarnings("unused")
   private static double roundDoubleHalfUp(double value, int decimals) {
      return BigDecimal.valueOf(value).setScale(decimals, BigDecimal.ROUND_HALF_UP).doubleValue();
   }

}
