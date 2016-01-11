/*
   Copyright 2015 Barend Garvelink

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package nl.garvelink.iban;

import java.math.BigInteger;

/**
 * Calculates the modulo 97 checksum used in IBAN numbers (and some other entities).
 */
public abstract class Modulo97 {

   /**
    * The BigInteger '97', used in the MOD97 division.
    */
   private static final BigInteger NINETY_SEVEN = new BigInteger("97");

   /**
    * Calculates the raw MOD97 checksum for a given input.
    * <p>
    * The input is allowed to contain space characters. Any character outside the range {@code [A-Za-z0-9 ]} will cause
    * an IllegalArgumentException to be thrown. This method allocates a temporary buffer of twice the input length, so
    * it will fail for unreasonably large inputs.</p>
    * <p>
    * It is expected, but not enforced, that the characters at index 2 and 3 are numeric. If the existing check digits
    * are {@code 00} then this method will return the value that, after subtracting it from 98, gives you the check
    * digits for a MOD-97 verifiable string. If the existing check digits are any other value, this method will return
    * {@code 1} if the input checksums correctly.</p>
    * <p>
    * You may want to use {@link #calculateCheckDigits(CharSequence)} or {@link #verifyCheckDigits(CharSequence)}
    * instead of this method.</p>
    *
    * @param input the input, which should be at least five characters excluding spaces.
    * @return the check digits calculated for the given IBAN.
    * @throws IllegalArgumentException if the input is in some way invalid.
    * @see #calculateCheckDigits(CharSequence)
    * @see #verifyCheckDigits(CharSequence)
    */
   public static int checksum(CharSequence input) {
      if (input == null || !atLeastFiveNonSpaceCharacters(input)) {
         throw new IllegalArgumentException("The input must be non-null and contain at least five non-space characters.");
      }
      final char[] buffer = new char[input.length() * 2];
      int offset = transform(input, 4, input.length(), buffer, 0);
      offset = transform(input, 0, 4, buffer, offset);
      final BigInteger sum = new BigInteger(new String(buffer, 0, offset));
      final BigInteger remainder = sum.remainder(NINETY_SEVEN);
      return remainder.intValue();
   }

   /**
    * Calculates the check digits to be used in a MOD97 checked string.
    * @param input the input; the characters at indices 2 and 3 <strong>must</strong> be {@code '0'}. The input must
    *              also satisfy the criteria defined in {@link #checksum(CharSequence)}.
    * @return the check digits to be used at indices 2 and 3 to make the input MOD97 verifiable.
    * @throws IllegalArgumentException if the input is in some way invalid.
    */
   public static int calculateCheckDigits(CharSequence input) {
      if (input == null || input.length() < 5 || input.charAt(2) != '0' || input.charAt(3) != '0') {
         throw new IllegalArgumentException("The input must be non-null, have a minimum length of five characters, and the characters at indices 2 and 3 must be '0'.");
      }
      return 98 - checksum(input);
   }

   /**
    * Determines whether the given input has a valid MOD97 checksum.
    * @param input the input to verify, it must meet the criteria defined in {@link #checksum(CharSequence)}.
    * @return {@code true} if the input passes checksum verification, {@code false} otherwise.
    * @throws IllegalArgumentException if the input is in some way invalid.
    */
   public static boolean verifyCheckDigits(CharSequence input) {
      return checksum(input) == 1;
   }

   /**
    * Copies {@code src[srcPos...srcLen)} into {@code dest[destPos)} while applying character to numeric transformation and skipping over space (ASCII 0x20) characters.
    * @param src the data to begin copying, must contain only characters {@code [A-Za-z0-9 ]}.
    * @param srcPos the index in {@code src} to begin transforming (inclusive).
    * @param srcLen the number of characters starting from {@code srcPos} to transform.
    * @param dest the buffer to write transformed characters into.
    * @param destPos the index in {@code dest} to begin writing.
    * @return the value of {@code destPos} incremented by the number of characters that were added, i.e. the next unused index in {@code dest}.
    * @throws IllegalArgumentException if {@code src} contains an unsupported character.
    * @throws ArrayIndexOutOfBoundsException if {@code dest} does not have enough capacity to store the transformed result (keep in mind that a single character from {@code src} can expand to two characters in {@code dest}).
    */
   private static int transform(final CharSequence src, final int srcPos, final int srcLen, final char[] dest, final int destPos) {
      int offset = destPos;
      for (int i = srcPos; i < srcLen; i++) {
         char c = src.charAt(i);
         if (c >= '0' && c <= '9') {
            dest[offset++] = c;
         } else if (c >= 'A' && c <= 'Z') {
            int tmp = 10 + (c - 'A');
            dest[offset++] = (char)('0' + tmp / 10);
            dest[offset++] = (char)('0' + tmp % 10);
         } else if (c >= 'a' && c <= 'z') {
            int tmp = 10 + (c - 'a');
            dest[offset++] = (char)('0' + tmp / 10);
            dest[offset++] = (char)('0' + tmp % 10);
         } else if (c != ' ') {
            throw new IllegalArgumentException("Invalid character '" + c + "'.");
         }
      }
      return offset;
   }

   private static boolean atLeastFiveNonSpaceCharacters(CharSequence input) {
      int lookingFor = 5;
      for (int i = 0, max = input.length(); lookingFor > 0 && i < max; i++) {
         if (input.charAt(i) != ' ') {
            lookingFor--;
         }
      }
      return lookingFor == 0;
   }

   /** Prevent instantiation of static utility class. */
   private Modulo97() { }
}