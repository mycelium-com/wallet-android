package com.mycelium.wapi.wallet.currency.test;

import junit.framework.AssertionFailedError;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.math.BigDecimal;

public class AssertHelper {
   public static void assertEqualValue(BigDecimal should, BigDecimal is) {
      if (should.compareTo(is) != 0) {
         throw new AssertionFailedError("Value should be " + should + " but is " + is);
      }
   }

   /**
    * Assert that {@code should} and {@code is} are equal when rounded to {@code places} decimals.
    *
    * @param should The expected value
    * @param is     The actual value
    * @param places Decimal places after the .
    * @throws ArithmeticException if {@code places} is negative
    */
   public static void assertRoundedEqualValue(BigDecimal should, BigDecimal is, int places) {
      BigDecimal delta = should.subtract(is).abs();
      BigDecimal epsilon = BigDecimal.valueOf(1, places);
      if (delta.compareTo(epsilon) > 0) {
         throw new AssertionFailedError("Value should be " + should + " but is " + is + "; delta = " + delta + " > " + epsilon);
      }
   }

   private static final BigDecimal BD_1_234 = BigDecimal.valueOf(1234L, 3);
   private static final BigDecimal BD_1_23 = BigDecimal.valueOf(123L, 2);
   private static final BigDecimal BD_1_235 = BigDecimal.valueOf(1235L, 3);
   private static final BigDecimal BD_1_000 = BigDecimal.valueOf(1000L, 3);
   private static final BigDecimal BD_1_734 = BigDecimal.valueOf(1734L, 3);
   private static final BigDecimal BD_2_334 = BigDecimal.valueOf(2334L, 3);
   private static final BigDecimal BD_1_0000000000001 = BigDecimal.valueOf(10000000000001L, 13);

   @Rule
   public final ExpectedException expectedException = ExpectedException.none();

   @Test
   public void testAssertEqualValueThrowsNot() throws Exception {
      // equals are equal
      assertRoundedEqualValue(BD_1_000, BD_1_000, 0);
      assertRoundedEqualValue(BD_1_000, BD_1_000, 20);
      assertRoundedEqualValue(BD_1_734, BD_1_734, 0);
      assertRoundedEqualValue(BD_1_734, BD_1_734, 20);
      // at "places == 0", rounding matters:
      assertRoundedEqualValue(BD_1_000, BD_1_234, 0);
      assertRoundedEqualValue(BD_1_000, BD_1_23, 0);
      assertRoundedEqualValue(BD_1_000, BD_1_235, 0);
      assertRoundedEqualValue(BD_1_000, BD_1_235, 0);
      // but ...
      assertRoundedEqualValue(BD_1_734, BD_2_334, 0);
      // scale on the other hand does not matter: 1.00 == 1, even at 20 places
      assertRoundedEqualValue(BD_1_000, BigDecimal.ONE, 20);
      // how about almost equal numbers?
      assertRoundedEqualValue(BD_1_000, BD_1_0000000000001, 12);
   }

   @Test
   public void testAssertEqualValueThrows1() {
      expectedException.expect(AssertionFailedError.class);
      assertRoundedEqualValue(BigDecimal.ONE, BigDecimal.ZERO, 1);
   }

   @Test
   public void testAssertEqualValueThrows2() {
      expectedException.expect(AssertionFailedError.class);
      assertRoundedEqualValue(BD_1_000, BD_1_734, 1);
   }

   @Test
   public void testAssertEqualValueThrows3() {
      expectedException.expect(AssertionFailedError.class);
      assertRoundedEqualValue(BD_1_734, BD_2_334, 1);
   }

   @Test
   public void testAssertEqualValueThrows4() {
      expectedException.expect(AssertionFailedError.class);
      assertRoundedEqualValue(BD_1_23, BD_1_235, 3);
   }

   @Test
   public void testAssertEqualValueThrows5() {
      expectedException.expect(AssertionFailedError.class);
      assertRoundedEqualValue(BD_1_000, BD_1_0000000000001, 14);
   }


}
