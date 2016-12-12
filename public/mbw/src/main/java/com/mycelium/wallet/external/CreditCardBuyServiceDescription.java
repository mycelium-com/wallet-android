package com.mycelium.wallet.external;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.widget.Toast;

import com.google.common.base.Optional;
import com.mrd.bitlib.model.Address;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;

public class CreditCardBuyServiceDescription extends BuySellServiceDescriptor {

   public CreditCardBuyServiceDescription() {
      super(R.string.credit_card_buy, R.string.credit_card_buy_description, R.string.credit_card_buy_setting_description, R.drawable.credit_card_buy);
   }

   @Override
   public void launchService(final Activity activity, MbwManager mbwManager, final Optional<Address> activeReceivingAddress) {

      // check if the current account is spend-able. if not, warn the user, but allow it if he wants to
      if (!mbwManager.getSelectedAccount().canSpend()) {
         new AlertDialog.Builder(activity)
                 .setTitle(R.string.buy_with_cc_spend_only_warning_title)
                 .setMessage(R.string.buy_with_cc_spend_only_warning)
                 .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                       launchWebservice(activity, activeReceivingAddress);
                    }
                 })
                 .setNegativeButton(R.string.no, null)
                 .show();
         Toast.makeText(activity, R.string.lt_warning_watch_only_account, Toast.LENGTH_LONG).show();
      } else {
         launchWebservice(activity, activeReceivingAddress);
      }
   }

   private void launchWebservice(final Activity activity, Optional<Address> activeReceivingAddress) {
      String uri = "https://swish.to/BTC/myceliumwallet";
      if (activeReceivingAddress.isPresent()) {
         uri += "?btcaddress=" + activeReceivingAddress.get().toString();
      }
      final String finalUri = uri;
      Utils.showOptionalMessage(activity, R.string.buy_with_cc_tos, new Runnable() {
         @Override
         public void run() {
            Utils.openWebsite(activity, finalUri);
         }
      });
   }

   @Override
   public boolean isEnabled(MbwManager mbwManager) {
      return mbwManager.getMetadataStorage().getSwishCreditCardIsEnabled();
   }

   @Override
   public void setEnabled(MbwManager mbwManager, boolean enabledState) {
      mbwManager.getMetadataStorage().setSwishCreditCardIsEnabled(enabledState);
   }
}
