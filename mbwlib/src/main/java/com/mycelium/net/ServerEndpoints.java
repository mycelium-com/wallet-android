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

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.Random;

public class ServerEndpoints {

   final private ArrayList<HttpEndpoint> endpoints;
   private int currentEndpoint;
   private ServerEndpointType allowedEndpointTypes = ServerEndpointType.ONLY_HTTPS;


   public ServerEndpoints(HttpEndpoint endpoints[]) {
      this.endpoints = Lists.newArrayList(endpoints);
      currentEndpoint = new Random().nextInt(this.endpoints.size());
      // ensure correct kind of endpoint
      switchToNextEndpoint();
   }

   public ServerEndpoints(HttpEndpoint endpoints[], int initialEndpoint) {
      this.endpoints = Lists.newArrayList(endpoints);

      Preconditions.checkElementIndex(initialEndpoint, endpoints.length);
      currentEndpoint = initialEndpoint;
   }

   public HttpEndpoint getCurrentEndpoint(){
      return endpoints.get(currentEndpoint);
   }

   public int getCurrentEndpointIndex(){
      return currentEndpoint;
   }

   public synchronized void switchToNextEndpoint(){
      HttpEndpoint selectedEndpoint;
      int cnt=0;
      int tmpCurrentEndpoint = currentEndpoint;
      do{
         tmpCurrentEndpoint = (tmpCurrentEndpoint + 1) % endpoints.size();
         selectedEndpoint = endpoints.get(tmpCurrentEndpoint);
         cnt++;
         if (cnt>endpoints.size()){
            throw new RuntimeException("No valid next Endpoint found, " + allowedEndpointTypes.toString());
         }
      }while(!allowedEndpointTypes.isValid(selectedEndpoint.getClass()));
      currentEndpoint = tmpCurrentEndpoint;
   }

   public void setAllowedEndpointTypes(ServerEndpointType types){
      allowedEndpointTypes=types;
      switchToNextEndpoint();
   }

   public void setTorManager(TorManager torManager){
      for (HttpEndpoint e : endpoints){
         if (e instanceof TorHttpsEndpoint){
            ((TorHttpsEndpoint) e).setTorManager(torManager);
         }
      }
   }
}
