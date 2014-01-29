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

package com.mrd.mbwapi.api;

import java.io.Serializable;
import java.net.URI;

import com.mrd.bitlib.util.ByteReader;
import com.mrd.bitlib.util.ByteWriter;

public class WalletVersionResponse extends ApiObject implements Serializable {

   private static final long serialVersionUID = -8708847365845218804L;
   public final String versionNumber;
   public final URI directDownload;
   public final String message;

   public WalletVersionResponse(String versionNumber, URI directDownload, String message) {
      this.versionNumber = versionNumber;
      this.directDownload = directDownload;
      this.message = message;
   }

   public WalletVersionResponse(ByteReader payloadReader) {
      try {
         versionNumber = payloadReader.getString();
         directDownload = URI.create(payloadReader.getString());
         message = payloadReader.getString();
      } catch (ByteReader.InsufficientBytesException e) {
         throw new RuntimeException(e);
      }
   }

   @Override
   protected ByteWriter toByteWriter(ByteWriter writer) {
      writer.putString(versionNumber);
      writer.putString(directDownload.toString());
      writer.putString(message);
      return writer;
   }

   @Override
   protected byte getType() {
      return ApiObject.WALLET_VERSION_RESPONSE_TYPE;
   }

   @Override
   public String toString() {
      return "WalletVersionResponse{" +
            "versionNumber='" + versionNumber + '\'' +
            ", directDownload=" + directDownload +
            ", message='" + message + '\'' +
            '}';
   }
}
