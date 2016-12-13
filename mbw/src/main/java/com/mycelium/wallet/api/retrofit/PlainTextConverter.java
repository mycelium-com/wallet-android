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

package com.mycelium.wallet.api.retrofit;

import retrofit.converter.ConversionException;
import retrofit.converter.Converter;
import retrofit.mime.TypedInput;
import retrofit.mime.TypedOutput;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.util.Scanner;

/**
 * A {@link Converter} which uses Jackson for reading and writing entities.
 * <p/>
 * <p/>
 * based on: https://github.com/square/retrofit/blob/master/retrofit-converters/jackson/src/main/java/retrofit/converter/JacksonConverter.java
 */
public class PlainTextConverter implements Converter {

   public PlainTextConverter() {

   }


   @Override
   public Object fromBody(TypedInput body, Type type) throws ConversionException {
      InputStream in = null;
      try {
         in = body.in();
         final Scanner s = new Scanner(in, "UTF-8").useDelimiter("\\A");
         final String ret = s.hasNext() ? s.next() : "";
         return ret;
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
         return new PlainTextOutput(object.toString());
      } catch (UnsupportedEncodingException e) {
         throw new AssertionError(e);
      }
   }

   private static class PlainTextOutput implements TypedOutput {
      private final byte[] bytes;
      private final String mimeType;

      public PlainTextOutput(String data) throws UnsupportedEncodingException {
         this(data.getBytes("UTF-8"), "UTF-8");
      }

      PlainTextOutput(byte[] jsonBytes, String encode) {
         this.bytes = jsonBytes;
         this.mimeType = "text/plain; charset=" + encode;
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
         return bytes.length;
      }

      @Override
      public void writeTo(OutputStream out) throws IOException {
         out.write(bytes);
      }
   }

}