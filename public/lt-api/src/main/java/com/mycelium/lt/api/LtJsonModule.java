package com.mycelium.lt.api;

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
import com.mrd.bitlib.crypto.PublicKey;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.util.HexUtils;

public class LtJsonModule extends SimpleModule {
   private static final long serialVersionUID = 1L;
   private static Map<Class<?>, JsonDeserializer<?>> DESERIALIZERS;
   private static List<JsonSerializer<?>> SERIALIZERS;

   static {
      DESERIALIZERS = new HashMap<Class<?>, JsonDeserializer<?>>();
      DESERIALIZERS.put(Address.class, new AddressDeserializer());
      DESERIALIZERS.put(PublicKey.class, new PublicKeyDeserializer());

      SERIALIZERS = new ArrayList<JsonSerializer<?>>();
      SERIALIZERS.add(new AddressSerializer());
      SERIALIZERS.add(new PublicKeySerializer());
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

   public LtJsonModule() {
      super("LT Json module", Version.unknownVersion(), DESERIALIZERS, SERIALIZERS);
   }
}
