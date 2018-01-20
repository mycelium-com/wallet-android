package com.mycelium.wallet.api.retrofit;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import retrofit.converter.ConversionException;
import retrofit.converter.Converter;
import retrofit.mime.TypedInput;
import retrofit.mime.TypedOutput;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;

/**
 * A {@link Converter} which uses Jackson for reading and writing entities.
 * <p/>
 * <p/>
 * based on: https://github.com/square/retrofit/blob/master/retrofit-converters/jackson/src/main/java/retrofit/converter/JacksonConverter.java
 */
public class JacksonConverter implements Converter {
   private final ObjectMapper objectMapper;

   public JacksonConverter() {
      this(new ObjectMapper());
   }

   public JacksonConverter(ObjectMapper objectMapper) {
      if (objectMapper == null) {
         throw new NullPointerException("objectMapper == null");
      }
      this.objectMapper = objectMapper;
   }

   @Override
   public Object fromBody(TypedInput body, Type type) throws ConversionException {
      InputStream in = null;
      try {
         JavaType javaType = objectMapper.getTypeFactory().constructType(type);
         in = body.in();
         return objectMapper.readValue(in, javaType);
      } catch (JsonParseException e) {
         throw new ConversionException(e);
      } catch (JsonMappingException e) {
         throw new ConversionException(e);
      } catch (IOException e) {
         throw new ConversionException(e);
      } finally {
         try {
            if (in != null) {
               in.close();
            }
         } catch (IOException ignored) {
         }
      }
   }

   @Override
   public TypedOutput toBody(Object object) {
      try {
         JavaType javaType = objectMapper.getTypeFactory().constructType(object.getClass());
         String json = objectMapper.writerWithType(javaType).writeValueAsString(object);
         return new JsonTypedOutput(json.getBytes("UTF-8"), "UTF-8");
      } catch (JsonProcessingException e) {
         throw new AssertionError(e);
      } catch (UnsupportedEncodingException e) {
         throw new AssertionError(e);
      }
   }

   private static class JsonTypedOutput implements TypedOutput {
      private final byte[] jsonBytes;
      private final String mimeType;

      JsonTypedOutput(byte[] jsonBytes, String encode) {
         this.jsonBytes = jsonBytes;
         this.mimeType = "application/json; charset=" + encode;
      }

      @Override
      public String fileName() {
         return null;
      }

      @Override
      public String mimeType() {
         return mimeType;
      }

      @Override
      public long length() {
         return jsonBytes.length;
      }

      @Override
      public void writeTo(OutputStream out) throws IOException {
         out.write(jsonBytes);
      }
   }

}