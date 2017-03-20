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

/*

   Based on https://github.com/barend/java-iban
   Directly embedded and slightly modified/stripped down to not rely on Java8

*/

package nl.garvelink.iban;

import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An immutable value type representing an International Bank Account Number. Instances of this class have correct
 * check digits and a valid length for their country code. No country-specific validation is performed, other than
 * matching the length of the IBAN to its country code. Unknown country codes are not supported.
 * @author Barend Garvelink (barend@garvelink.nl) https://github.com/barend
 */
public final class IBAN {

   /**
    * A comparator that puts IBAN's into lexicographic ordering, per {@link String#compareTo(String)}.
    */
   public static final Comparator<IBAN> LEXICAL_ORDER = new Comparator<IBAN>() {
      @Override
      public int compare(IBAN iban, IBAN iban2) {
         return iban.value.compareTo(iban2.value);
      }
   };

   /**
    * The technically shortest possible IBAN.
    */
   public static final int SHORTEST_POSSIBLE_IBAN = 5;

   /**
    * Used to remove spaces
    */
   private static final Pattern SPACE_PATTERN = Pattern.compile(" ");

   /**
    * IBAN value, normalized form (no whitespace).
    */
   private final String value;

   /**
    * Whether or not this IBAN is of a SEPA participating country.
    */
   private final boolean sepa;

   /**
    * Pretty-printed value, lazily initialized.
    */
   private transient String valuePretty;

   /**
    * Initializing constructor.
    * @param value the IBAN value, without any white space.
    * @throws IllegalArgumentException if the input is null, malformed or otherwise fails validation.
    */
   private IBAN(String value) {
      if (value == null) {
         throw new IllegalArgumentException("Input is null");
      }
      if (value.length() < SHORTEST_POSSIBLE_IBAN) {
         throw new IllegalArgumentException("Length is too short to be an IBAN");
      }
      if (value.charAt(2) < '0' || value.charAt(2) > '9' || value.charAt(3) < '0' || value.charAt(3) > '9') {
         throw new IllegalArgumentException("Characters at index 2 and 3 not both numeric.");
      }
      final String countryCode = value.substring(0, 2);
      final int expectedLength = CountryCodes.getLengthForCountryCode(countryCode);
      if (expectedLength < 0) {
         throw new UnknownCountryCodeException(value);
      }
      if (expectedLength != value.length()) {
         throw new WrongLengthException(value, expectedLength);
      }
      final int calculatedChecksum = Modulo97.checksum(value);
      if (calculatedChecksum != 1) {
         throw new WrongChecksumException(value);
      }
      this.value = value;
      this.sepa = CountryCodes.isSEPACountry(countryCode);
   }

   /**
    * Parses the given string into an IBAN object and confirms the check digits.
    * @param input the input, which can be either plain ("CC11ABCD123...") or formatted with (ASCII 0x20) space characters ("CC11 ABCD 123. ..").
    * @return the parsed and validated IBAN object, never null.
    * @throws IllegalArgumentException if the input is null, malformed or otherwise fails validation.
    * @see #valueOf(String)
    */
   public static IBAN parse(String input) {
      if (input == null || input.length() == 0) {
         throw new IllegalArgumentException("Input is null or empty string.");
      }
      if (!isLetterOrDigit(input.charAt(0)) || !isLetterOrDigit(input.charAt(input.length() - 1))) {
         throw new IllegalArgumentException("Input begins or ends in an invalid character.");
      }
      return new IBAN(toPlain(input));
   }

   /**
    * Parses the given string into an IBAN object and confirms the check digits, but returns null for null.
    * @param input the input, which can be either plain ("CC11ABCD123...") or formatted ("CC11 ABCD 123. ..").
    * @return the parsed and validated IBAN object, or null.
    * @throws IllegalArgumentException if the input is malformed or otherwise fails validation.
    * @see #parse(String)
    */
   public static IBAN valueOf(String input) {
      if (input == null) {
         return null;
      }
      return parse(input);
   }

   /**
    * @deprecated invoke {@link CountryCodes#getLengthForCountryCode(String)} instead.
    * @param countryCode the country code for which to return the length.
    * @return the length of the IBAN for the given country code, or -1 if unknown.
    */
   @Deprecated
   public static int getLengthForCountryCode(String countryCode) {
      return CountryCodes.getLengthForCountryCode(countryCode);
   }

   /**
    * Returns the Country Code embedded in the IBAN.
    * @return the two-letter country code.
    */
   public String getCountryCode() {
      return value.substring(0, 2);
   }

   /**
    * Returns the check digits of the IBAN.
    * @return the two check digits.
    */
   public String getCheckDigits() {
      return value.substring(2, 4);
   }

   /**
    * Returns whether the IBAN's country participates in SEPA.
    * @return true if SEPA, false if non-SEPA.
    */
   public boolean isSEPA() {
      return this.sepa;
   }

   /**
    * Returns the IBAN without formatting.
    * @return the unformatted IBAN number.
    */
   public String toPlainString() {
      return value;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof IBAN)) return false;
      return value.equals(((IBAN) o).value);
   }

   @Override
   public int hashCode() {
      return value.hashCode();
   }

   /**
    * Returns the IBAN in standard formatting, with a space every four characters.
    * @return the formatted IBAN number.
    * @see #toPlainString()
    */
   @Override
   public String toString() {
      // This code is using a non-threadsafe (but still nullsafe) assignment. The addSpaces() operation is
      // idempotent, so no harm done if it happens to run more than once. I expect concurrent use to be rare.
      String vp = valuePretty;
      if (vp == null) {
         vp = valuePretty = addSpaces(value);
      }
      return vp;
   }

   /**
    * Returns whether the given character is in the {@code A-Za-z0-9} range.
    * This differs from {@link Character#isLetterOrDigit(char)} because it doesn't understand non-Western characters.
    */
   private static boolean isLetterOrDigit(char c) {
      return (c >= '0' && c <= '9')
            || (c >= 'A' && c <= 'Z')
            || (c >= 'a' && c <= 'z');
   }

   /**
    * Removes any spaces contained in the String thereby converting the input into a plain IBAN
    *
    * @param input
    *         possibly pretty printed IBAN
    * @return plain IBAN
    */
   public static String toPlain(String input) {
      Matcher matcher = SPACE_PATTERN.matcher(input);
      if (matcher.find()) {
         return matcher.replaceAll("");
      } else {
         return input;
      }
   }

   /**
    * Ensures that the input is pretty printed by first removing any spaces the String might contain and then adding spaces in the right places.
    * <p>This can be useful when prompting a user to correct wrong input</p>
    *
    * @param input
    *         plain or pretty printed IBAN
    * @return pretty printed IBAN
    */
   public static String toPretty(String input) {
      return addSpaces(toPlain(input));
   }

   /**
    * Converts a plain to a pretty printed IBAN
    *
    * @param value
    *         plain iban
    * @return pretty printed IBAN
    */
   private static String addSpaces(String value) {
      final int length = value.length();
      final int lastPossibleBlock = length - 4;
      final StringBuilder sb = new StringBuilder(length + (length - 1) / 4);
      int i;
      for (i = 0; i < lastPossibleBlock; i += 4) {
         sb.append(value, i, i + 4);
         sb.append(' ');
      }
      sb.append(value, i, length);
      return sb.toString();
   }

   public static class UnknownCountryCodeException extends IllegalArgumentException {
      private final String failedInput;

      UnknownCountryCodeException(String failedInput) {
         super("Unknown country code in " + failedInput);
         this.failedInput = failedInput;
      }

      public String getFailedInput() {
         return failedInput;
      }
   }

   public static class WrongChecksumException extends IllegalArgumentException {
      private final String failedInput;

      WrongChecksumException(String failedInput) {
         super("Input \"" + failedInput + "\" failed checksum validation.");
         this.failedInput = failedInput;
      }

      public String getFailedInput() {
         return failedInput;
      }
   }

   public static class WrongLengthException extends IllegalArgumentException {
      private final String failedInput;
      private final int actualLength;
      private final int expectedLength;

      WrongLengthException(String failedInput, int expectedLength) {
         super("Input \"" + failedInput + "\" failed length validation: found " + failedInput.length() + ", but expect "
               + expectedLength + " for country code.");
         this.failedInput = failedInput;
         this.actualLength = failedInput.length();
         this.expectedLength = expectedLength;
      }

      public String getFailedInput() {
         return failedInput;
      }

      public int getExpectedLength() {
         return expectedLength;
      }

      public int getActualLength() {
         return actualLength;
      }
   }
}