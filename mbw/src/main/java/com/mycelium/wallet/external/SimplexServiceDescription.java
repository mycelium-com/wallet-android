package com.mycelium.wallet.external;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AlertDialog;
import android.widget.Toast;

import com.google.common.base.Optional;
import com.mrd.bitlib.model.Address;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.simplex.SimplexMainActivity;


public class SimplexServiceDescription extends BuySellServiceDescriptor {

   public static final String SOFELLO = "https://app.s4f3.io/sdk/quickbuy.html?appId=1234-5678&lang=en&country=no&address=_address_";

   public SimplexServiceDescription() {
      super(R.string.si_buy_sell, R.string.si_buy_sell_description, R.string.si_setting_show_button_summary, R.drawable.credit_card_buy);
   }

   @Override
   public void launchService(final Activity context, MbwManager mbwManager, Optional<Address> activeReceivingAddress) {
      if (!mbwManager.getSelectedAccount().canSpend()) {
         Toast.makeText(context, R.string.lt_warning_watch_only_account, Toast.LENGTH_LONG).show();
         return;
      }
      Optional<Address> receivingAddress = mbwManager.getSelectedAccount().getReceivingAddress();
      if (receivingAddress.isPresent()) {
         final Address address = receivingAddress.get();
         String[] regions = {context.getString(R.string.europe), context.getString(R.string.asia),
                 context.getString(R.string.united_states), context.getString(R.string.australia)};
         new AlertDialog.Builder(context, R.style.BuySell_Dialog)
                 .setTitle(R.string.select_you_region)
                 .setItems(regions, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                       Intent intent;
                       if (i == 0) {
                          intent = new Intent(Intent.ACTION_VIEW,
                                  Uri.parse(SOFELLO.replace("_address_", address.toString())));
                       } else {
                          intent = new Intent(context, SimplexMainActivity.class);
                          intent.putExtra("walletAddress", address.toString());
                       }
                       context.startActivity(intent);
                    }
                 })
                 .create().show();
      } else {
         Toast.makeText(context, "Simplex cannot start - no available address.", Toast.LENGTH_LONG).show();
      }
   }

   @Override
   public boolean isEnabled(MbwManager mbwManager) {
      return mbwManager.getMetadataStorage().getSimplexIsEnabled();
   }

   @Override
   public void setEnabled(MbwManager mbwManager, boolean enabledState) {
      mbwManager.getMetadataStorage().setSimplexIsEnabled(enabledState);
   }
}
