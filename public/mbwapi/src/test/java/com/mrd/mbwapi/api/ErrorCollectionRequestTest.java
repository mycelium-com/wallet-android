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

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Ignore;
import org.junit.Test;

import com.mrd.bitlib.model.NetworkParameters;
import com.mrd.bitlib.util.ByteWriter;
import com.mrd.mbwapi.impl.MyceliumWalletApiImpl;

public class ErrorCollectionRequestTest {
   @Test
   public void testToByteWriter() throws Exception {
      String longString = String.valueOf(new char[2000]);
      ErrorCollectionRequest req = new ErrorCollectionRequest(new RuntimeException(longString), "junit", ErrorMetaData.DUMMY);
      ByteWriter bigWriter = new ByteWriter(1024);
      assertEquals(0, bigWriter.length());
      req.toByteWriter(bigWriter);
      assertTrue(bigWriter.length() > 1024);
   }

   @Test
   @Ignore
   public void testSendRealMail2() throws ApiException, IOException {

//      InputStream inputStream = new URL("https://node3.mycelium.com/mwstestnet/api/1/request/status").openStream();
//      System.out.println(new Byte);

      MyceliumWalletApiImpl.HttpsEndpoint httpsTestnetEndpoint = new MyceliumWalletApiImpl.HttpsEndpoint(
            "https://node3.mycelium.com/mwstestnet", "E5:70:76:B2:67:3A:89:44:7A:48:14:81:DF:BD:A0:58:C8:82:72:4F");

//      MyceliumWalletApiImpl.HttpsEndpoint httpsTestnetEndpoint = new MyceliumWalletApiImpl.HttpsEndpoint(
//            "https://mws1.mycelium.com/mws", "B3:42:65:33:40:F5:B9:1B:DA:A2:C8:7A:F5:4C:7C:5D:A9:63:C4:C3");

      MyceliumWalletApiImpl api = new MyceliumWalletApiImpl(new MyceliumWalletApiImpl.HttpEndpoint[]{httpsTestnetEndpoint},
            NetworkParameters.productionNetwork);
      api.getRate(CurrencyCode.USD);


      api.collectError(new RuntimeException("fresh test from junit to node1"),"-1",ErrorMetaData.DUMMY);

   }

   @Test
   @Ignore
   public void testSendRealMail3() throws ApiException, IOException {

//      InputStream inputStream = new URL("https://node3.mycelium.com/mwstestnet/api/1/request/status").openStream();
//      System.out.println(new Byte);

      MyceliumWalletApiImpl.HttpEndpoint httpsTestnetEndpoint = new MyceliumWalletApiImpl.HttpEndpoint(
            "http://192.168.178.53:8086");

      MyceliumWalletApiImpl api = new MyceliumWalletApiImpl(new MyceliumWalletApiImpl.HttpEndpoint[]{httpsTestnetEndpoint},
            NetworkParameters.testNetwork);
      api.getRate(CurrencyCode.USD);


      api.collectError(new RuntimeException("test from junit"),"-1",ErrorMetaData.DUMMY);

   }
}
