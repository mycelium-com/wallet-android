package com.mycelium.wallet.external;

import android.app.Activity;
import android.content.Intent;
import android.widget.Toast;

import com.google.common.base.Optional;
import com.mrd.bitlib.model.Address;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.external.changelly.ChangellyActivity;

public class ChangellyServiceDescription extends BuySellServiceDescriptor {
    public ChangellyServiceDescription() {
        super(R.string.changelly, R.string.changelly_description, R.string.changelly_setting_show_button_summary, R.drawable.changelly_square_logo_dark);
    }

    @Override
    public void launchService(Activity activity, MbwManager mbwManager, Optional<Address> activeReceivingAddress) {
//        final ChangellyService changellyService = ChangellyService.getInstance();
        Optional<Address> receivingAddress = mbwManager.getSelectedAccount().getReceivingAddress();
        if (receivingAddress.isPresent()) {
            Address address = receivingAddress.get();
            Intent intent = new Intent(activity, ChangellyActivity.class);
            intent.putExtra("walletAddress", address.toString());
            activity.startActivity(intent);
        } else {
            Toast.makeText(activity, "Changelly cannot start - no available address.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean isEnabled(MbwManager mbwManager) {
        return mbwManager.getMetadataStorage().getChangellyIsEnabled();
    }

    @Override
    public void setEnabled(MbwManager mbwManager, boolean enabledState) {
        mbwManager.getMetadataStorage().setChangellyIsEnabled(enabledState);
    }
}
