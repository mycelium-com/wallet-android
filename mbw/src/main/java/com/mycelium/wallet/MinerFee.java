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

package com.mycelium.wallet;

import android.content.Context;

import com.megiontechnologies.Bitcoins;
import com.mycelium.wapi.api.lib.FeeEstimation;

public enum MinerFee {
   LOWPRIO("LOWPRIO", 20, R.string.miner_fee_lowprio_name, R.string.miner_fee_lowprio_desc),
   ECONOMIC("ECONOMIC", 10, R.string.miner_fee_economic_name, R.string.miner_fee_economic_desc),
   NORMAL("NORMAL", 3, R.string.miner_fee_normal_name, R.string.miner_fee_normal_desc),
   PRIORITY("PRIORITY", 1, R.string.miner_fee_priority_name, R.string.miner_fee_priority_desc);

   public final String tag;
   private final int nBlocks;
   private final int idTag;
   private final int idLongDesc;

   MinerFee(String tag, int nBlocks, int idTag, int idLongDesc) {
      this.tag = tag;
      this.nBlocks = nBlocks;
      this.idTag = idTag;
      this.idLongDesc = idLongDesc;
   }

   @Override
   public String toString() {
      return tag;
   }

   public static MinerFee fromString(String string) {
      for(MinerFee fee : values()) {
         if(fee.tag.equals(string)) {
            return fee;
         }
      }
      return NORMAL;
   }

   //simply returns the next fee in order of declaration, starts with the first after reaching the last
   //useful for cycling through them in sending for example
   public MinerFee getNext() {
      return values()[(ordinal() + 1) % values().length];
   }

   public MinerFee getPrevious() {
      return values()[(ordinal() - 1 + values().length) % values().length];
   }

   public Bitcoins getFeePerKb(FeeEstimation feeEstimation) {
      return feeEstimation.getEstimation(nBlocks);
   }

   public String getMinerFeeName(Context context) {
      return context.getString(idTag);
   }

   public String getMinerFeeDescription(Context context) {
      return context.getString(idLongDesc);
   }

   public int getNBlocks() {
      return nBlocks;
   }

}
