/*
 * Copyright 2013 Megion Research and Development GmbH
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

package com.mycelium.wapi.model;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mrd.bitlib.model.OutPoint;

public class TransactionOutputEx implements Serializable {
   private static final long serialVersionUID = 1L;

   @JsonProperty
   public final OutPoint outPoint;
   @JsonProperty
   public final int height; // -1 means unconfirmed
   @JsonProperty
   public final long value;
   @JsonProperty
   public final byte[] script;
   @JsonProperty
   public final boolean isCoinBase;

   public TransactionOutputEx(@JsonProperty("outPoint") OutPoint outPoint, @JsonProperty("height") int height,
         @JsonProperty("value") long value, @JsonProperty("script") byte[] script,
         @JsonProperty("isCoinBase") boolean isCoinBase) {
      this.outPoint = outPoint;
      this.height = height;
      this.value = value;
      this.script = script;
      this.isCoinBase = isCoinBase;
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("outPoint:").append(outPoint).append(" height:").append(height).append(" value: ").append(value)
            .append(" isCoinbase: ").append(isCoinBase).append(" scriptLength: ").append(script.length);
      return sb.toString();
   }

   @Override
   public int hashCode() {
      return outPoint.hashCode();
   }

   @Override
   public boolean equals(Object obj) {
      if (obj == this) {
         return true;
      }
      if (!(obj instanceof TransactionOutputEx)) {
         return false;
      }
      TransactionOutputEx other = (TransactionOutputEx) obj;
      return outPoint.equals(other.outPoint);
   }

}
