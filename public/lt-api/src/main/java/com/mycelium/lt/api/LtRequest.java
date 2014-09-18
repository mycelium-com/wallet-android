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

package com.mycelium.lt.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

public class LtRequest {
   private StringBuilder _sb;
   private boolean _firstParameter;
   private String _postString;

   public LtRequest(String function) {
      _sb = new StringBuilder();
      _firstParameter = true;
      _sb.append(function);
      _postString = "";
   }

   public URL getUrl(String baseUrl) throws MalformedURLException {
      return new URL(baseUrl+toString());
   }

   public String getPostString() {
      return _postString;
   }

   public byte[] getPostBytes() {
      try {
         return _postString.getBytes("UTF-8");
      } catch (UnsupportedEncodingException e) {
         // Never happens
         throw new RuntimeException(e);
      }
   }

   public LtRequest setPostObject(ObjectMapper objectMapper, Object params)
         {
      try {
         _postString = objectMapper.writeValueAsString(params);
      } catch (JsonProcessingException e) {
         throw new RuntimeException(e);
      }
      return this;
   }

   public LtRequest addQueryParameter(String name, String value) {
      if (_firstParameter) {
         _firstParameter = false;
         _sb.append('?');
      } else {
         _sb.append('&');
      }
      try {
         _sb.append(URLEncoder.encode(name, "UTF-8")).append('=').append(URLEncoder.encode(value, "UTF-8"));
      } catch (UnsupportedEncodingException e) {
         // Never happens.... Jesus... we are using UTF-8!
      }
      return this;
   }

   @Override
   public String toString() {
      return _sb.toString();
   }
}
