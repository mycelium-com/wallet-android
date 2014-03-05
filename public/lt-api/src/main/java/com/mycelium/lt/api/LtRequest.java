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
   private LtApiClient.HttpEndpoint _endpoint;
   private String _postString;

   public LtRequest(LtApiClient.HttpEndpoint endpoint, String function) {
      _endpoint = endpoint;
      _sb = new StringBuilder();
      _firstParameter = true;
      _sb.append(endpoint.baseUrlString).append(function);
      _postString = "";
   }

   public URL getUrl() throws MalformedURLException {
      return new URL(toString());
   }

   public boolean isHttps() {
      return _endpoint instanceof LtApiClient.HttpsEndpoint;
   }

   public String getCertificateThumbprint() {
      if (isHttps()) {
         return ((LtApiClient.HttpsEndpoint) _endpoint).certificateThumbprint;
      }
      return null;
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
