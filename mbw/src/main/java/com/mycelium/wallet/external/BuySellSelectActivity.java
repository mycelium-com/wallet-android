package com.mycelium.wallet.external;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.google.api.client.util.Lists;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.activity.modern.ModernMain;

import java.util.List;

import javax.annotation.Nullable;


public class BuySellSelectActivity extends FragmentActivity {
   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.buy_sell_service_selector);

      if (getActionBar() != null) {
         getActionBar().setDisplayHomeAsUpEnabled(true);
      }

      final MbwManager mbwManager = MbwManager.getInstance(this);


      final ListView lvServices = findViewById(R.id.lvServices);
      final List<BuySellServiceDescriptor> buySellServices = mbwManager.getEnvironmentSettings().getBuySellServices();

      BuySellServiceDescriptor onlyOneEnabled = null;
      // check if only one is enabled - if so, just launch it directly
      for (BuySellServiceDescriptor buySellService : buySellServices) {
         if (buySellService.isEnabled(mbwManager)){
            if (onlyOneEnabled == null){
               onlyOneEnabled = buySellService;
            } else {
               // more than one is enabled
               onlyOneEnabled = null;
               break;
            }
         }
      }

      if (onlyOneEnabled != null){
         onlyOneEnabled.launchService(this, mbwManager, mbwManager.getSelectedAccount().getReceivingAddress());
      }

      final List<BuySellServiceDescriptor> enabledServices = Lists.newArrayList(Iterables.filter(buySellServices, new Predicate<BuySellServiceDescriptor>() {
         @Override
         public boolean apply(@Nullable BuySellServiceDescriptor input) {
            return input.isEnabled(mbwManager);
         }
      }));

      lvServices.setAdapter(new ServicesListAdapter(this, enabledServices));
   }


   @Override
   public boolean onOptionsItemSelected(MenuItem item) {
      switch (item.getItemId()) {
         case android.R.id.home:
            Intent intent = new Intent(this, ModernMain.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
            return true;
      }
      return super.onOptionsItemSelected(item);
   }

   private class ServicesListAdapter extends ArrayAdapter<BuySellServiceDescriptor> {
      private final Context context;
      private final List<BuySellServiceDescriptor> buySellServices;
      private final LayoutInflater inflater;
      private final MbwManager mbwManager;

      public ServicesListAdapter(Context context, List<BuySellServiceDescriptor> buySellServices) {
         super(context, R.layout.buy_sell_service_row, buySellServices);
         this.context = context;
         this.buySellServices = buySellServices;
         inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
         mbwManager = MbwManager.getInstance(context);
      }

      @Override
      public View getView(int position, View convertView, ViewGroup parent) {
         View v = convertView;

         if (v == null) {
            v = Preconditions.checkNotNull(inflater.inflate(R.layout.buy_sell_service_row, parent, false));
         }
         final BuySellServiceDescriptor service = Preconditions.checkNotNull(getItem(position));

         ((TextView) v.findViewById(R.id.tvServiceName)).setText(service.title);
         ((TextView) v.findViewById(R.id.tvServiceDescription)).setText(service.description);
         ((ImageView) v.findViewById(R.id.ivIcon)).setImageDrawable(service.getIcon(context.getResources()));

         v.findViewById(R.id.llServiceRow).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               service.launchService(BuySellSelectActivity.this, mbwManager, mbwManager.getSelectedAccount().getReceivingAddress());
            }
         });

         return v;
      }
   }
}
