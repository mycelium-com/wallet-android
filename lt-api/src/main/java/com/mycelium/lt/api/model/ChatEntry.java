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

package com.mycelium.lt.api.model;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ChatEntry implements Serializable {
   private static final long serialVersionUID = 1L;

   public static final int TYPE_OWNER_CHAT = 1;
   public static final int TYPE_PEER_CHAT = 2;
   public static final int TYPE_EVENT = 3;

   public static final int EVENT_SUBTYPE_NONE = 0;
   public static final int EVENT_SUBTYPE_TRADE_STARTED = 1;
   public static final int EVENT_SUBTYPE_TRADE_OWNER_REFRESH = 2;
   public static final int EVENT_SUBTYPE_TRADE_PEER_REFRESH = 3;
   public static final int EVENT_SUBTYPE_OWNER_ACCEPTED = 4;
   public static final int EVENT_SUBTYPE_PEER_ACCEPTED = 5;
   public static final int EVENT_SUBTYPE_OWNER_ABORTED = 6;
   public static final int EVENT_SUBTYPE_PEER_ABORTED = 7;
   public static final int EVENT_SUBTYPE_BTC_RELEASED = 8;
   public static final int EVENT_SUBTYPE_BTC_CONFIRMED = 9;
   public static final int EVENT_SUBTYPE_TRADE_OWNER_CHANGE_PRICE = 10;
   public static final int EVENT_SUBTYPE_TIMEOUT = 11;
   public static final int EVENT_SUBTYPE_OWNER_STOPPED = 12;
   public static final int EVENT_SUBTYPE_PEER_STOPPED = 13;
   public static final int EVENT_SUBTYPE_TRADE_LOCATION = 14;
   public static final int EVENT_SUBTYPE_CASH_ONLY_WARNING = 15;

   @JsonProperty
   public long time;
   @JsonProperty
   public int type;
   @JsonProperty
   public int subtype;
   @JsonProperty
   public String message;

   public ChatEntry(@JsonProperty("time") long time, @JsonProperty("type") int type,
         @JsonProperty("subtype") int subtype, @JsonProperty("message") String message) {
      this.time = time;
      this.type = type;
      this.subtype = subtype;
      this.message = message;
   }

}
