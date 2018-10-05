package com.mycelium.wallet.external;

import android.app.Activity;
import android.content.Intent;
import android.widget.Toast;

import com.google.common.base.Optional;
import com.mrd.bitlib.model.Address;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.simplex.SimplexMainActivity;


public class SimplexServiceDescription extends BuySellServiceDescriptor {
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
         Address address = receivingAddress.get();
         Intent intent = new Intent(context, SimplexMainActivity.class);
         intent.putExtra("walletAddress",address.toString());
         context.startActivity(intent);

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
