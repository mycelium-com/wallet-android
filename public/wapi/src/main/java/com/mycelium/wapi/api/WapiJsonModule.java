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

package com.mycelium.wapi.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.megiontechnologies.Bitcoins;
import com.mrd.bitlib.crypto.PublicKey;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.OutPoint;
import com.mrd.bitlib.util.HexUtils;
import com.mrd.bitlib.util.Sha256Hash;

/**
 * Helper class for serializing and deserializing complex types where we cannot
 * add Jackson annotations
 */
public class WapiJsonModule extends SimpleModule {
   private static final long serialVersionUID = 1L;
   private static Map<Class<?>, JsonDeserializer<?>> DESERIALIZERS;
   private static List<JsonSerializer<?>> SERIALIZERS;

   static {
      DESERIALIZERS = new HashMap<Class<?>, JsonDeserializer<?>>();
      DESERIALIZERS.put(Bitcoins.class, new BitcoinDeserializer());
      DESERIALIZERS.put(Address.class, new AddressDeserializer());
      DESERIALIZERS.put(PublicKey.class, new PublicKeyDeserializer());
      DESERIALIZERS.put(Sha256Hash.class, new Sha256HashDeserializer());
      DESERIALIZERS.put(OutPoint.class, new OutPointDeserializer());

      SERIALIZERS = new ArrayList<JsonSerializer<?>>();
      SERIALIZERS.add(new BitcoinSerializer());
      SERIALIZERS.add(new AddressSerializer());
      SERIALIZERS.add(new PublicKeySerializer());
      SERIALIZERS.add(new Sha256HashSerializer());
      SERIALIZERS.add(new OutPointSerializer());
   }

   private static class BitcoinDeserializer extends JsonDeserializer<Bitcoins> {
      @Override
      public Bitcoins deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException,
            JsonProcessingException {
         ObjectCodec oc = jp.getCodec();
         JsonNode node = oc.readTree(jp);
         Bitcoins bitcoins = Bitcoins.valueOf(node.asLong());
         if (bitcoins == null) {
            throw new JsonParseException("Failed to convert string '" + node.asText() + "' into an bitcoin",
                  JsonLocation.NA);
         }
         return bitcoins;
      }
   }

   private static class AddressDeserializer extends JsonDeserializer<Address> {

      @Override
      public Address deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException,
            JsonProcessingException {
         ObjectCodec oc = jp.getCodec();
         JsonNode node = oc.readTree(jp);
         Address address = Address.fromString(node.asText());
         if (address == null) {
            throw new JsonParseException("Failed to convert string '" + node.asText() + "' into an address",
                  JsonLocation.NA);
         }
         return address;
      }

   }

   private static class BitcoinSerializer extends JsonSerializer<Bitcoins> {

      @Override
      public void serialize(Bitcoins value, JsonGenerator jgen, SerializerProvider provider) throws IOException,
            JsonProcessingException {
         jgen.writeString(Long.toString(value.getLongValue()));
      }

      @Override
      public Class<Bitcoins> handledType() {
         return Bitcoins.class;
      }

   }

   private static class AddressSerializer extends JsonSerializer<Address> {

      @Override
      public void serialize(Address value, JsonGenerator jgen, SerializerProvider provider) throws IOException,
            JsonProcessingException {
         jgen.writeString(value.toString());
      }

      @Override
      public Class<Address> handledType() {
         return Address.class;
      }

   }

   private static class PublicKeyDeserializer extends JsonDeserializer<PublicKey> {

      @Override
      public PublicKey deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException,
            JsonProcessingException {
         ObjectCodec oc = jp.getCodec();
         JsonNode node = oc.readTree(jp);
         byte[] pubKeyBytes;
         try {
            pubKeyBytes = HexUtils.toBytes(node.asText());
         } catch (RuntimeException e) {
            throw new JsonParseException("Failed to convert string '" + node.asText() + "' into an public key bytes",
                  JsonLocation.NA);
         }
         return new PublicKey(pubKeyBytes);
      }

   }

   private static class PublicKeySerializer extends JsonSerializer<PublicKey> {

      @Override
      public void serialize(PublicKey value, JsonGenerator jgen, SerializerProvider provider) throws IOException,
            JsonProcessingException {
         jgen.writeString(HexUtils.toHex(value.getPublicKeyBytes()));
      }

      @Override
      public Class<PublicKey> handledType() {
         return PublicKey.class;
      }

   }

   private static class Sha256HashDeserializer extends JsonDeserializer<Sha256Hash> {

      @Override
      public Sha256Hash deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException,
            JsonProcessingException {
         ObjectCodec oc = jp.getCodec();
         JsonNode node = oc.readTree(jp);
         Sha256Hash hash = Sha256Hash.fromString(node.asText());
         if (hash == null) {
            throw new JsonParseException("Failed to convert string '" + node.asText() + "' into a Sha256Hash instance",
                  JsonLocation.NA);
         }
         return hash;
      }

   }

   private static class Sha256HashSerializer extends JsonSerializer<Sha256Hash> {

      @Override
      public void serialize(Sha256Hash value, JsonGenerator jgen, SerializerProvider provider) throws IOException,
            JsonProcessingException {
         jgen.writeString(value.toString());
      }

      @Override
      public Class<Sha256Hash> handledType() {
         return Sha256Hash.class;
      }

   }

   private static class OutPointDeserializer extends JsonDeserializer<OutPoint> {

      @Override
      public OutPoint deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException,
            JsonProcessingException {
         ObjectCodec oc = jp.getCodec();
         JsonNode node = oc.readTree(jp);
         OutPoint outPoint = OutPoint.fromString(node.asText());
         if (outPoint == null) {
            throw new JsonParseException("Failed to convert string '" + node.asText() + "' into an OutPoint instance",
                  JsonLocation.NA);
         }
         return outPoint;
      }

   }

   private static class OutPointSerializer extends JsonSerializer<OutPoint> {

      @Override
      public void serialize(OutPoint value, JsonGenerator jgen, SerializerProvider provider) throws IOException,
            JsonProcessingException {
         jgen.writeString(value.toString());
      }

      @Override
      public Class<OutPoint> handledType() {
         return OutPoint.class;
      }

   }

   public WapiJsonModule() {
      super("Wapi Json module", Version.unknownVersion(), DESERIALIZERS, SERIALIZERS);
   }
}
