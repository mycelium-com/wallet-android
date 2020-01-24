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

package com.mycelium.net;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

import java.util.ArrayList;

public class ServerEndpointType {
   private ArrayList<Class> allowedEndpoints;

   public enum Types{
      ALL, HTTPS_AND_TOR, ONLY_HTTPS, ONLY_TOR
   }

   //public static ServerEndpointType ALL = new ServerEndpointType(new Class[]{HttpEndpoint.class, HttpsEndpoint.class, TorHttpsEndpoint.class });
   //public static ServerEndpointType HTTPS_AND_TOR = new ServerEndpointType(new Class[]{HttpsEndpoint.class, TorHttpsEndpoint.class});
   public static ServerEndpointType ONLY_HTTPS = new ServerEndpointType(new Class[]{HttpsEndpoint.class});
   public static ServerEndpointType ONLY_TOR = new ServerEndpointType(new Class[]{TorHttpsEndpoint.class});

   public static ServerEndpointType fromType(Types type){
      /*if (type == Types.ALL) {
         return ALL;
      }else if (type == Types.HTTPS_AND_TOR){
         return HTTPS_AND_TOR;
      }else*/
      if (type == Types.ONLY_TOR){
         return ONLY_TOR;
      }else {
         return ONLY_HTTPS;
      }
   }


   public ServerEndpointType(Class allowedEndpoints[]) {
      this.allowedEndpoints = Lists.newArrayList(allowedEndpoints);
   }

   public boolean mightUseTor(){
      return allowedEndpoints.contains(TorHttpsEndpoint.class);
   }

   public boolean isValid(Class endpointType){
      return allowedEndpoints.contains(endpointType);
   }

   @Override
   public String toString() {
      return Joiner.on(", ").join(allowedEndpoints);
   }
}
