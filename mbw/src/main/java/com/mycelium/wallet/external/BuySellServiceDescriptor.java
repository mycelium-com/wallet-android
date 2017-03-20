package com.mycelium.wallet.external;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;

import com.google.common.base.Optional;
import com.mrd.bitlib.model.Address;
import com.mycelium.wallet.MbwManager;

abstract public class BuySellServiceDescriptor {
   @StringRes
   final public int title;

   @StringRes
   final public int description;

   @StringRes
   final public int settingDescription;

   final public int icon;


   public BuySellServiceDescriptor(@StringRes int title, @StringRes int description, @StringRes int settingDescription, @DrawableRes int icon) {
      this.title = title;
      this.description = description;
      this.settingDescription = settingDescription;
      this.icon = icon;
   }

   public Drawable getIcon(Resources resources){
      return resources.getDrawable(icon);
   };

   public boolean showEnableInSettings() { return true; }

   abstract public void launchService(Activity activity, MbwManager mbwManager, Optional<Address> activeReceivingAddress);
   abstract public boolean isEnabled(MbwManager mbwManager);
   abstract public void setEnabled(MbwManager mbwManager, boolean enabledState);
}
