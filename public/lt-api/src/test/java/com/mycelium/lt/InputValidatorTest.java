package com.mycelium.lt;


import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class InputValidatorTest {

   @Test
   public void testIllegalUsernames() {
      assertInvalid("abcabcabcabcabcabcabcabcabcabcabc");
      assertInvalid(" abc ");
      assertInvalid(" abc");
      assertInvalid("abc ");
      assertInvalid("a*-bc ");
      assertInvalid("Björn");
      assertInvalid("a");
      assertInvalid("_12345678901234567890");
      assertInvalid("***");
   }

   @Test
   public void testValidUsernames() {
      assertValid("abc");
      assertValid("abc abc");
      assertValid("a.-_bc0");
      assertValid("1234567890ABc");
      assertValid("ap1980");

   }

   private void assertInvalid(String input) {
      assertFalse("this should be invalid: '" + input + "'", InputValidator.isValidTraderName(input));
   }

   private void assertValid(String input) {
      assertTrue("this should be valid: '" + input + "'", InputValidator.isValidTraderName(input));
   }
}
