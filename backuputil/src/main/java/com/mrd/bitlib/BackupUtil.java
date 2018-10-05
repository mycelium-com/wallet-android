/*
 * Copyright 2013. 2014 Megion Research & Development GmbH
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

package com.mrd.bitlib;

import java.io.IOException;

import com.mrd.bitlib.crypto.InMemoryPrivateKey;
import com.mrd.bitlib.crypto.MrdExport;
import com.mrd.bitlib.model.AddressType;

public class BackupUtil {
   private final String encryptedPrivateKey;
   private final String password;

   public BackupUtil(String... args) {
      encryptedPrivateKey = args[0];
      password = args[1];
   }

   public static void main(String[] args) throws IOException, MrdExport.DecodingException, InterruptedException {
      if (args.length != 2) {
         printHelp();
         return;
      }
      BackupUtil backupUtil = new BackupUtil(args);
      System.out.println(backupUtil.getKey());
   }

   public String getKey() {
      final String realpassword;
      if (password.length() == 16) {
         boolean checksumValid = MrdExport.isChecksumValid(password);
         if (!checksumValid) {
            return "Error: the last character of the password was not matching the checksum";
         } else {
            realpassword = password.substring(0, 15);
         }
      } else {
         if (password.length() != 15) {
            return "Error: the supplied password did not match the expected length";
         }
         realpassword = password;
      }
      try {
         MrdExport.V1.Header header = MrdExport.V1.extractHeader(encryptedPrivateKey);
         MrdExport.V1.KdfParameters kdfParameters = MrdExport.V1.KdfParameters.fromPassphraseAndHeader(realpassword, header);
         MrdExport.V1.EncryptionParameters parameters = MrdExport.V1.EncryptionParameters.generate(kdfParameters);
         String privateKey = MrdExport.V1.decryptPrivateKey(parameters, encryptedPrivateKey, header.network);
         InMemoryPrivateKey key = new InMemoryPrivateKey(privateKey, header.network);
         return "Private key (Wallet Import Format): " + key.getBase58EncodedPrivateKey(header.network) +
               "\n                   Bitcoin Address: " + key.getPublicKey().toAddress(header.network, AddressType.P2PKH) +
               "\n                   Bitcoin Address: " + key.getPublicKey().toAddress(header.network, AddressType.P2SH_P2WPKH) +
               "\n                   Bitcoin Address: " + key.getPublicKey().toAddress(header.network, AddressType.P2WPKH);
      } catch (InterruptedException e) {
         throw new RuntimeException(e);
      } catch (MrdExport.V1.WrongNetworkException e) {
         throw new RuntimeException(e);
      } catch (MrdExport.V1.InvalidChecksumException e) {
         return "Error: the supplied password did not match the checksum of the encrypted key";
      } catch (MrdExport.DecodingException e) {
         throw new RuntimeException(e);
      }
   }

   private static void printHelp() {
      System.out.println("\n" +
            "Usage of this restore utility:\n" +
            "java -jar backuputil.jar encryptedKey PASSWORD\n" +
            "EXAMPLE:\n" +
            "java -jar backuputil.jar xEncGXICZE1_eVYfGWDioNu_8hA6RZzep4XqwPGRtcKb01MDg3s1XFntJYI9Dw QDTDXOYFBXBKKMKR");
   }
}
