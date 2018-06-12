package com.mycelium.wallet.activity.main.adapter;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.mrd.bitlib.model.Address;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.activity.util.AdaptiveDateFormat;
import com.mycelium.wallet.activity.util.TransactionConfirmationsDisplay;
import com.mycelium.wallet.persistence.MetadataStorage;
import com.mycelium.wapi.model.TransactionSummary;
import com.mycelium.wapi.wallet.currency.CurrencyValue;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static android.content.Context.MODE_PRIVATE;
import static com.mycelium.wallet.activity.send.SendMainActivity.TRANSACTION_FIAT_VALUE;
import static com.mycelium.wallet.external.changelly.bch.ExchangeFragment.BCH_EXCHANGE;
import static com.mycelium.wallet.external.changelly.bch.ExchangeFragment.BCH_EXCHANGE_TRANSACTIONS;

public class TransactionArrayAdapter extends ArrayAdapter<TransactionSummary> {
   private final MetadataStorage _storage;
   protected Context _context;
   private final boolean _alwaysShowAddress;
   private DateFormat _dateFormat;
   private MbwManager _mbwManager;
   private Fragment _containerFragment;
   private Map<Address, String> _addressBook;
   private SharedPreferences transactionFiatValuePref;
   private Set<String> exchangeTransactions;

   public TransactionArrayAdapter(Context context, List<TransactionSummary> transactions, Map<Address, String> addressBook) {
      this(context, transactions, null, addressBook, true);
   }

   public TransactionArrayAdapter(Context context,
                                  List<TransactionSummary> transactions,
                                  Fragment containerFragment,
                                  Map<Address, String> addressBook,
                                  boolean alwaysShowAddress) {
      super(context, R.layout.transaction_row, transactions);
      _context = context;
      _alwaysShowAddress = alwaysShowAddress;
      _dateFormat = new AdaptiveDateFormat(context);
      _mbwManager = MbwManager.getInstance(context);
      _containerFragment = containerFragment;
      _storage = _mbwManager.getMetadataStorage();
      _addressBook = addressBook;

      transactionFiatValuePref = context.getSharedPreferences(TRANSACTION_FIAT_VALUE, MODE_PRIVATE);

      SharedPreferences sharedPreferences = context.getSharedPreferences(BCH_EXCHANGE, MODE_PRIVATE);
      exchangeTransactions = sharedPreferences.getStringSet(BCH_EXCHANGE_TRANSACTIONS, new HashSet<String>());
   }

   @NonNull
   @Override
   public View getView(final int position, View convertView, ViewGroup parent) {
      // Only inflate a new view if we are not reusing an old one
      View rowView = convertView;
      if (rowView == null) {
         LayoutInflater inflater = (LayoutInflater) _context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
         rowView = Preconditions.checkNotNull(inflater.inflate(R.layout.transaction_row, parent, false));
      }

      // Make sure we are still added
      if (_containerFragment != null && !_containerFragment.isAdded()) {
         // We have observed that the fragment can be disconnected at this
         // point
         return rowView;
      }

      final TransactionSummary record = getItem(position);

      // Determine Color
      int color;
      if (record.isIncoming) {
         color = _context.getResources().getColor(R.color.green);
      } else {
         color = _context.getResources().getColor(R.color.red);
      }

      // Set Date
      Date date = new Date(record.time * 1000L);
      TextView tvDate = (TextView) rowView.findViewById(R.id.tvDate);
      tvDate.setText(_dateFormat.format(date));

      // Set value
      TextView tvAmount = (TextView) rowView.findViewById(R.id.tvAmount);
      if(_mbwManager.getColuManager().isColuAsset(record.value.getCurrency())) {
         tvAmount.setText(Utils.getColuFormattedValueWithUnit(record.value));
      } else if(record.value.getCurrency().equals("BCH")) {
         tvAmount.setText(_mbwManager.getBchValueString(record.value.getLongValue()));
      } else {
         tvAmount.setText(Utils.getFormattedValueWithUnit(record.value, _mbwManager.getBitcoinDenomination()));
      }
      tvAmount.setTextColor(color);

      // Set alternative value
      TextView tvFiat = (TextView) rowView.findViewById(R.id.tvFiatAmount);
      String alternativeCurrency = _mbwManager.getCurrencySwitcher().getCurrentCurrency();

      // if the current selected currency is the same as the transactions
      if (alternativeCurrency.equals(record.value.getCurrency())) {
         if (record.value.isBtc() || record.value.isBch() || record.value.getCurrency().equalsIgnoreCase("RMC")) {
            // use the current selected fiat currency
            alternativeCurrency = _mbwManager.getCurrencySwitcher().getCurrentFiatCurrency();
         } else {
            // always show BTC
            alternativeCurrency = CurrencyValue.BTC;
         }
      }

      if(alternativeCurrency.equals(CurrencyValue.BTC)
          && (record.value.getCurrency().equalsIgnoreCase("RMC")
          || record.value.getCurrency().equalsIgnoreCase("BCH"))) {
         alternativeCurrency = _mbwManager.getCurrencySwitcher().getCurrentFiatCurrency();
      }

      if (!alternativeCurrency.equals("")) {
         CurrencyValue alternativeCurrencyValue = CurrencyValue.fromValue(
               record.value,
               alternativeCurrency,
               _mbwManager.getExchangeRateManager());

         if (alternativeCurrencyValue.getValue() == null) {
            tvFiat.setVisibility(View.GONE);
         } else {
            tvFiat.setVisibility(View.VISIBLE);
            tvFiat.setText(Utils.getFormattedValueWithUnit(alternativeCurrencyValue, _mbwManager.getBitcoinDenomination()));
            tvFiat.setTextColor(color);
         }
      } else {
         tvFiat.setVisibility(View.GONE);
      }

      TextView tvFiatTimed = (TextView) rowView.findViewById(R.id.tvFiatAmountTimed);
      String value = transactionFiatValuePref.getString(record.txid.toHex(), null);
      tvFiatTimed.setVisibility(value != null ? View.VISIBLE : View.GONE);
      if(value != null) {
         tvFiatTimed.setText(value);
      }

      // Show destination address and address label, if this address is in our address book
      TextView tvAddressLabel = (TextView) rowView.findViewById(R.id.tvAddressLabel);
      TextView tvDestAddress = (TextView) rowView.findViewById(R.id.tvDestAddress);


      if (record.destinationAddress.isPresent()) {
         if (_addressBook.containsKey(record.destinationAddress.get())) {
            tvDestAddress.setText(record.destinationAddress.get().getShortAddress());
            tvAddressLabel.setText(String.format(_context.getString(R.string.transaction_to_address_prefix), _addressBook.get(record.destinationAddress.get())));
            tvDestAddress.setVisibility(View.VISIBLE);
            tvAddressLabel.setVisibility(View.VISIBLE);
         } else if (_alwaysShowAddress) {
            tvDestAddress.setText(record.destinationAddress.get().getShortAddress());
            tvDestAddress.setVisibility(View.VISIBLE);
         } else {
            tvDestAddress.setVisibility(View.GONE);
            tvAddressLabel.setVisibility(View.GONE);
         }
      } else {
         tvDestAddress.setVisibility(View.GONE);
         tvAddressLabel.setVisibility(View.GONE);
      }

      // Show confirmations indicator
      int confirmations = record.confirmations;
      TransactionConfirmationsDisplay tcdConfirmations = (TransactionConfirmationsDisplay) rowView.findViewById(R.id.tcdConfirmations);
      if (record.isQueuedOutgoing) {
         // Outgoing, not broadcasted
         tcdConfirmations.setNeedsBroadcast();
      } else {
         tcdConfirmations.setConfirmations(confirmations);
      }

      // Show label or confirmations
      TextView tvLabel = (TextView) rowView.findViewById(R.id.tvTransactionLabel);
      String label = _storage.getLabelByTransaction(record.txid);
      if(exchangeTransactions.contains(record.txid.toString())) {
         //label = "Exchange BCH to BTC " + label;
      }
      if (label.length() == 0) {
         // if we have no txLabel show the confirmation state instead - to keep they layout ballanced
         String confirmationsText;
         if (record.isQueuedOutgoing) {
            confirmationsText = _context.getResources().getString(R.string.transaction_not_broadcasted_info);
         } else {
            if (confirmations > 6) {
               confirmationsText = _context.getResources().getString(R.string.confirmed);
            } else {
               confirmationsText = _context.getResources().getString(R.string.confirmations, confirmations);
            }
         }
         tvLabel.setText(confirmationsText);
      } else {
         tvLabel.setText(label);
      }

      // Show risky unconfirmed warning if necessary
      TextView tvWarnings = (TextView) rowView.findViewById(R.id.tvUnconfirmedWarning);
      if (confirmations <= 0) {
         ArrayList<String> warnings = new ArrayList<String>();
         if (record.confirmationRiskProfile.isPresent()) {
            if (record.confirmationRiskProfile.isPresent() && record.confirmationRiskProfile.get().hasRbfRisk) {
               warnings.add(_context.getResources().getString(R.string.warning_reason_rbf));
            }
            if (record.confirmationRiskProfile.get().unconfirmedChainLength > 0) {
               warnings.add(_context.getResources().getString(R.string.warning_reason_unconfirmed_parent));
            }
            if (record.confirmationRiskProfile.get().isDoubleSpend) {
               warnings.add(_context.getResources().getString(R.string.warning_reason_doublespend));
            }
         }

         if (warnings.size() > 0) {
            tvWarnings.setText(_context.getResources().getString(R.string.warning_risky_unconfirmed, Joiner.on(", ").join(warnings)));
            tvWarnings.setVisibility(View.VISIBLE);
         } else {
            tvWarnings.setVisibility(View.GONE);
         }
      } else {
         tvWarnings.setVisibility(View.GONE);
      }

      rowView.setTag(record);
      return rowView;
   }
}
