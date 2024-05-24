package com.mycelium.wallet.activity.main.adapter;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.activity.send.SendCoinsActivity;
import com.mycelium.wallet.activity.util.AdaptiveDateFormat;
import com.mycelium.wallet.activity.util.TransactionConfirmationsDisplay;
import com.mycelium.wallet.activity.util.ValueExtensionsKt;
import com.mycelium.wallet.persistence.MetadataStorage;
import com.mycelium.wapi.wallet.Address;
import com.mycelium.wapi.wallet.AddressUtils;
import com.mycelium.wapi.wallet.TransactionSummary;
import com.mycelium.wapi.wallet.TransactionSummaryKt;
import com.mycelium.wapi.wallet.coins.AssetInfo;
import com.mycelium.wapi.wallet.coins.Value;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static android.content.Context.MODE_PRIVATE;
import static com.mycelium.wallet.activity.send.SendCoinsActivity.BATCH_HASH_PREFIX;

public class TransactionArrayAdapter extends ArrayAdapter<TransactionSummary> {
   public static final String BCH_EXCHANGE = "bch_exchange";
   public static final String BCH_EXCHANGE_TRANSACTIONS = "bch_exchange_transactions";
   private final MetadataStorage _storage;
   protected Context _context;
   private DateFormat _dateFormat;
   private MbwManager _mbwManager;
   private Fragment _containerFragment;
   private SharedPreferences transactionFiatValuePref;
   private Map<Address, String> _addressBook;
   private boolean _alwaysShowAddress;
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
      _dateFormat = new AdaptiveDateFormat(context);
      _mbwManager = MbwManager.getInstance(context);
      _containerFragment = containerFragment;
      _storage = _mbwManager.getMetadataStorage();
      _addressBook = addressBook;
      _alwaysShowAddress = alwaysShowAddress;
      transactionFiatValuePref = context.getSharedPreferences(SendCoinsActivity.TRANSACTION_FIAT_VALUE, MODE_PRIVATE);

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
      if (record.isIncoming()) {
         color = _context.getResources().getColor(R.color.green);
      } else {
         color = _context.getResources().getColor(R.color.red);
      }

      // Set Date
      Date date = new Date(record.getTimestamp() * 1000L);
      TextView tvDate = rowView.findViewById(R.id.tvDate);
      tvDate.setText(_dateFormat.format(date));

      // Set value
      TextView tvAmount = rowView.findViewById(R.id.tvAmount);
      tvAmount.setText(ValueExtensionsKt.toStringWithUnit(record.getTransferred().abs(),
              _mbwManager.getDenomination(_mbwManager.getSelectedAccount().getCoinType())));
      tvAmount.setTextColor(color);

      // Set alternative value
      TextView tvFiat = rowView.findViewById(R.id.tvFiatAmount);
      AssetInfo alternativeCurrency = _mbwManager.getCurrencySwitcher()
              .getCurrentFiatCurrency(_mbwManager.getSelectedAccount().getCoinType());

      if (alternativeCurrency != null) {
         Value recordValue = record.getTransferred().abs();
         Value alternativeValue = _mbwManager.getExchangeRateManager().get(recordValue, alternativeCurrency);

         if (alternativeValue == null) {
            tvFiat.setVisibility(View.GONE);
         } else {
            tvFiat.setVisibility(View.VISIBLE);
            tvFiat.setText(ValueExtensionsKt.toStringWithUnit(alternativeValue,
                    _mbwManager.getDenomination(_mbwManager.getSelectedAccount().getCoinType())));
            tvFiat.setTextColor(color);
         }
      } else {
         tvFiat.setVisibility(View.GONE);
      }

      TextView tvFiatTimed = rowView.findViewById(R.id.tvFiatAmountTimed);
      String value = transactionFiatValuePref.getString(record.getIdHex(), null);
      if (value == null && !record.isIncoming()) {
         value = transactionFiatValuePref.getString(BATCH_HASH_PREFIX + record.getIdHex(), null);
      }
      boolean showFiatTimed = value != null && !TransactionSummaryKt.isMinerFeeTx(record, _mbwManager.getSelectedAccount());
      tvFiatTimed.setVisibility(showFiatTimed ? View.VISIBLE : View.GONE);
      if (value != null) {
         tvFiatTimed.setText(value);
      }

      // Show destination address and address label, if this address is in our address book
      TextView tvAddressLabel = rowView.findViewById(R.id.tvAddressLabel);
      TextView tvDestAddress = rowView.findViewById(R.id.tvDestAddress);

      if (record.getDestinationAddresses().size() > 0) {
         // As we have a current limitation to send only to one recepient, we consider that
         // record.destinationAddresses should always have size of 1
         // and thus take the first element from it.
         Address destAddress = record.getDestinationAddresses().get(0);
         String destAddressStr = destAddress.toString();
         if (_addressBook.containsKey(destAddress)) {
            tvDestAddress.setText(AddressUtils.toShortString(destAddressStr));
            tvAddressLabel.setText(String.format(_context.getString(R.string.transaction_to_address_prefix),
                    _addressBook.get(destAddress)));
            tvAddressLabel.setVisibility(View.VISIBLE);
            tvDestAddress.setVisibility(View.VISIBLE);

            tvAddressLabel.setVisibility(View.VISIBLE);
         } else if (_alwaysShowAddress) {
            tvDestAddress.setText(AddressUtils.toShortString(destAddressStr));
            tvDestAddress.setVisibility(View.VISIBLE);
            tvAddressLabel.setVisibility(View.VISIBLE);
         } else {
            tvDestAddress.setVisibility(View.GONE);
            tvAddressLabel.setVisibility(View.GONE);
         }
      } else {
         tvDestAddress.setVisibility(View.GONE);
         tvAddressLabel.setVisibility(View.GONE);
      }

      // Show confirmations indicator
      int confirmations = record.getConfirmations();
      TransactionConfirmationsDisplay tcdConfirmations = rowView.findViewById(R.id.tcdConfirmations);
      if (record.isQueuedOutgoing()) {
         // Outgoing, not broadcasted
         tcdConfirmations.setNeedsBroadcast();
      } else {
         tcdConfirmations.setConfirmations(confirmations);
      }

      // Show label or confirmations
      TextView tvLabel = rowView.findViewById(R.id.tvTransactionLabel);
      String label = _storage.getLabelByTransaction(record.getIdHex());
      // if we have no txLabel show the confirmation state instead - to keep they layout ballanced
      String confirmationsText;
      if (record.isQueuedOutgoing()) {
         confirmationsText = _context.getResources().getString(R.string.transaction_not_broadcasted_info);
      } else {
         if (confirmations > 6) {
            confirmationsText = _context.getResources().getString(R.string.confirmed);
         } else {
            confirmationsText = _context.getResources().getString(R.string.confirmations, confirmations);
         }
      }
      String minerFeeLabel = "";
      if (TransactionSummaryKt.isMinerFeeTx(record, _mbwManager.getSelectedAccount())) {
         minerFeeLabel = " / " + _context.getString(R.string.history_row_miner_fee_label);
      }
      tvLabel.setText(confirmationsText + minerFeeLabel + (label.isEmpty() ? "" : " / " + label));

      // Show risky unconfirmed warning if necessary
      TextView tvWarnings = rowView.findViewById(R.id.tvUnconfirmedWarning);
      if (confirmations <= 0) {
         ArrayList<String> warnings = new ArrayList<String>();
         if (record.getConfirmationRiskProfile().isPresent()) {
            if (record.getConfirmationRiskProfile().get().hasRbfRisk) {
               warnings.add(_context.getResources().getString(R.string.warning_reason_rbf));
            }
            if (record.getConfirmationRiskProfile().get().unconfirmedChainLength > 0) {
               warnings.add(_context.getResources().getString(R.string.warning_reason_unconfirmed_parent));
            }
            if (record.getConfirmationRiskProfile().get().isDoubleSpend) {
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
