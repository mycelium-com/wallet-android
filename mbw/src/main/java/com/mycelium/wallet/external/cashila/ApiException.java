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

package com.mycelium.wallet.external.cashila;


import com.mycelium.wallet.external.cashila.api.response.CashilaResponse;

public class ApiException extends RuntimeException {
   public final int code;

   public static ApiException fromResponse(CashilaResponse<?> response){
      if (!response.isError()) {
         throw new RuntimeException("Not an API exception");
      }
      switch (response.error.code){
         case 1012: return new BadSignUpParams(response);
         case 1013: return new UserAlreadyExists(response);
         case 1017: return new AnotherUserPaired(response);
         case 1021: return new AccountLocked(response);
         case 1022: return new WrongPassword(response);
         case 1023: return new WrongSecondFactor(response);
         default: return new ApiException(response);
      }
   }

   public ApiException(String message) {
      super(message);
      code=-1;
   }

   protected ApiException(CashilaResponse<?> response) {
      super(response.error.toString());
      code = response.error.code;
   }

   public static class BadSignUpParams extends ApiException{
      public BadSignUpParams(CashilaResponse<?> response) {
         super(response);
      }
   }

   public static class UserAlreadyExists extends ApiException{
      public UserAlreadyExists(CashilaResponse<?> response) {
         super(response);
      }
   }

   public static class AnotherUserPaired extends ApiException{
      public AnotherUserPaired(CashilaResponse<?> response) {
         super(response);
      }
   }

   public static class AccountLocked extends ApiException{
      public AccountLocked(CashilaResponse<?> response) {
         super(response);
      }
   }

   public static class WrongPassword extends ApiException{
      public WrongPassword(CashilaResponse<?> response) {
         super(response);
      }
   }

   public static class WrongSecondFactor extends ApiException{
      public WrongSecondFactor(CashilaResponse<?> response) {
         super(response);
      }
   }
}
