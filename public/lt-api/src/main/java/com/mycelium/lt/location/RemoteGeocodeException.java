/*
 * Copyright 2013, 2014 Megion Research & Development GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mycelium.lt.location;

public class RemoteGeocodeException extends Exception {
   private static final long serialVersionUID = 4646210150078841846L;
   public final String status;
   @SuppressWarnings("unused")
   private final String url;

   public RemoteGeocodeException(String message, String status, String errorMessage) {
      super(message + " status: " + status + " errorMessage: " + errorMessage);
      this.status = status;
      url = null;
   }

   public RemoteGeocodeException(String url, String status) {
      super(url + " status: " + status);
      this.status = status;
      this.url = url;
   }
}
