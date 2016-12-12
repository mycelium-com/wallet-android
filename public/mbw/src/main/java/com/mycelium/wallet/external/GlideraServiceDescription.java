package com.mycelium.wallet.external;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;

import com.google.common.base.Optional;
import com.mrd.bitlib.model.Address;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.external.glidera.activities.GlideraMainActivity;
import com.mycelium.wallet.external.glidera.api.GlideraService;
import com.mycelium.wallet.external.glidera.api.response.GlideraError;
import com.mycelium.wallet.external.glidera.api.response.StatusResponse;

import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;

public class GlideraServiceDescription extends BuySellServiceDescriptor {

   public GlideraServiceDescription() {
      super(R.string.gd_buy_sell, R.string.gd_buy_sell_description, R.string.glidera_setting_show_button_summary, R.drawable.glidera);
   }

   @Override
   public void launchService(final Activity context, MbwManager mbwManager, Optional<Address> activeReceivingAddress) {
      final GlideraService glideraService = GlideraService.getInstance();

      final ProgressDialog progress = ProgressDialog
              .show(context, context.getString(R.string.gd_buy_sell), context.getString(R.string.gd_loading), true);
      glideraService.status()
              .observeOn(AndroidSchedulers.mainThread())
              .subscribe(new Observer<StatusResponse>() {
                 @Override
                 public void onCompleted() {
                    progress.dismiss();
                 }

                 @Override
                 public void onError(Throwable e) {
                    GlideraError error = GlideraService.convertRetrofitException(e);
                    if (error != null && error.getCode() != null) {
                       if (error.getCode() == GlideraError.ERROR_UNKNOWN_USER) {
                          // Invalid credentials, send to bitid registration
                          // this wallet had never used this service before
                          Utils.showSimpleMessageDialog(
                                  context,
                                  context.getString(R.string.glidera_tos),
                                  new Runnable() {
                                     @Override
                                     public void run() {
                                        // redirect the user to glidera website to complete the sign-up process
                                        String uri = glideraService.getBitidRegistrationUrl();
                                        Utils.openWebsite(context, uri);
                                     }
                                  }
                          );
                       }
                    } else {
                       Utils.showSimpleMessageDialog(
                               context,
                               String.format(context.getString(R.string.gd_error_unable_to_connect), e.getLocalizedMessage())
                       );
                    }
                    progress.dismiss();
                 }

                 @Override
                 public void onNext(StatusResponse statusResponse) {
                    if (statusResponse.isUserCanTransact()) {
                       Intent intent = new Intent(context, GlideraMainActivity.class);
                       context.startActivity(intent);
                       context.finish();
                    } else {
                       //Send to setup
                       String uri = glideraService.getSetupUrl();
                       Utils.openWebsite(context, uri);
                    }
                 }
              });
   }

   @Override
   public boolean isEnabled(MbwManager mbwManager) {
      return mbwManager.getMetadataStorage().getGlideraIsEnabled();
   }

   @Override
   public void setEnabled(MbwManager mbwManager, boolean enabledState) {
      mbwManager.getMetadataStorage().setGlideraIsEnabled(enabledState);
   }
}
