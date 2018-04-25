/*
 * Copyright 2013, 2014 Megion Research and Development GmbH
 *
 * Licensed under the Microsoft Reference Source License (MS-RSL)
 *
 * This license governs use of the accompanying software. If you use the software, you accept this license.
 * If you do not accept the license, do not use the software.
 *
 * 1. Definitions
 * The terms "reproduce," "reproduction," and "distribution" have the same meaning here as under U.S. copyright law.
 * "You" means the licensee of the software.
 * "Your company" means the company you worked for when you downloaded the software.
 * "Reference use" means use of the software within your company as a reference, in read only form, for the sole purposes
 * of debugging your products, maintaining your products, or enhancing the interoperability of your products with the
 * software, and specifically excludes the right to distribute the software outside of your company.
 * "Licensed patents" means any Licensor patent claims which read directly on the software as distributed by the Licensor
 * under this license.
 *
 * 2. Grant of Rights
 * (A) Copyright Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 * worldwide, royalty-free copyright license to reproduce the software for reference use.
 * (B) Patent Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 * worldwide, royalty-free patent license under licensed patents for reference use.
 *
 * 3. Limitations
 * (A) No Trademark License- This license does not grant you any rights to use the Licensor’s name, logo, or trademarks.
 * (B) If you begin patent litigation against the Licensor over patents that you think may apply to the software
 * (including a cross-claim or counterclaim in a lawsuit), your license to the software ends automatically.
 * (C) The software is licensed "as-is." You bear the risk of using it. The Licensor gives no express warranties,
 * guarantees or conditions. You may have additional consumer rights under your local laws which this license cannot
 * change. To the extent permitted under your local laws, the Licensor excludes the implied warranties of merchantability,
 * fitness for a particular purpose and non-infringement.
 */

package com.mycelium.wallet.activity.util;

import android.content.Context;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import com.mycelium.wallet.R;

import java.lang.reflect.Field;

public class TransactionConfirmationsDisplay extends AppCompatImageView {
   public static final int MAX_CONFIRMATIONS = 6;

   public TransactionConfirmationsDisplay(Context context) {
      super(context);
      setConfirmations(0);
   }

   public TransactionConfirmationsDisplay(Context context, AttributeSet attrs) {
      super(context, attrs);
      setConfirmations(0);
   }

   public TransactionConfirmationsDisplay(Context context, AttributeSet attrs, int defStyle) {
      super(context, attrs, defStyle);
      setConfirmations(0);
   }

   public void setNeedsBroadcast(){
      try {
         Class res = R.drawable.class;
         Field field = res.getField("pie_send");
         int drawableId = field.getInt(null);
         setImageResource(drawableId);
      } catch (NoSuchFieldException | IllegalAccessException e) {
         throw new RuntimeException("drawable not found, pie_send");
      }
   }

   public void setConfirmations(int number){
      if (number > MAX_CONFIRMATIONS){
         number = MAX_CONFIRMATIONS;
      }else if (number < 0){
         number = 0;
      }

      try {
         Class res = R.drawable.class;
         Field field = res.getField("pie_" + number);
         int drawableId = field.getInt(null);
         setImageResource(drawableId);
      } catch (NoSuchFieldException | IllegalAccessException e) {
         throw new RuntimeException("drawable not found, " + number);
      }
   }
}

/*
generate pie graphics:

http://jsfiddle.net/a02gw5xf/2/

convert all.png -fuzz 2% -trim +repage trim1.png  #white
convert all.png -fuzz 2% -trim +repage trim.png   #black

for i in {0..6}; do; echo $i; convert trim.png -repage 0x0 -crop "0x22+0+$(($i*24))" -fuzz 4% -transparent black  pie_$i.png; done;

 */
