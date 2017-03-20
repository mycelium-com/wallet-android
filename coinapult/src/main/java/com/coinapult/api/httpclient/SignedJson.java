package com.coinapult.api.httpclient;

import com.google.api.client.json.GenericJson;

/**
* Created by Andreas on 12.11.2014.
*/
public class SignedJson extends GenericJson {
   @com.google.api.client.util.Key
   public String sign;

   @com.google.api.client.util.Key
   public String data;
}
