package com.mycelium.wapi.wallet.coins

import com.google.common.base.Preconditions.checkArgument
import java.io.Serializable
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

open class Value(
        /**
         * The type of this value
         */
        @JvmField
        val type: AssetInfo,
        /**
         * The number of units of this monetary value.
         */
        @JvmField
        val value: BigInteger) : Serializable {

    private val friendlyDigits: Int
        get() = type.friendlyDigits

    val valueAsBigDecimal: BigDecimal
        get() = BigDecimal(value).movePointLeft(type.unitExponent)

    val valueAsLong: Long
        get() = value.toLong()

    val currencySymbol: String
        get() = type.symbol

    /**
     * Returns true if and only if this instance represents a monetary value greater than zero,
     * otherwise false.
     */
    fun isPositive() = signum() == 1

    /**
     * Returns true if and only if this instance represents a monetary value less than zero,
     * otherwise false.
     */
    fun isNegative() = signum() == -1

    /**
     * Returns true if and only if this instance represents zero monetary value,
     * otherwise false.
     */
    open fun isZero() = signum() == 0

    private fun smallestUnitExponent(): Int = type.unitExponent

    operator fun plus(value: Value): Value {
        checkArgument(type == value.type, "Cannot add a different type")
        return Value(this.type, this.value + value.value)
    }

    operator fun plus(value: BigInteger): Value = Value(this.type, this.value + value)

    operator fun plus(value: Long): Value = Value(this.type, this.value + value.toBigInteger())

    operator fun minus(value: Value): Value {
        checkArgument(type == value.type, "Cannot subtract a different type")
        return Value(this.type, this.value - value.value)
    }

    operator fun minus(str: String): Value =
            Value(this.type, this.value - type.value(str).value)

    operator fun minus(value: BigInteger): Value = Value(this.type, this.value - value)

    operator fun times(factor: Long): Value = Value(this.type, this.value.times(factor.toBigInteger()))

    operator fun times(factor: BigInteger): Value = Value(this.type, this.value.times(factor))

    operator fun div(divisor: BigInteger): Value = Value(this.type, this.value / divisor)

    operator fun rem(divisor: BigInteger): Array<Value> =
            arrayOf(Value(this.type, this.value / divisor), Value(this.type, this.value % divisor))

    operator fun div(divisor: Value): BigInteger {
        checkArgument(type == divisor.type, "Cannot divide with a different type")
        return this.value / divisor.value
    }

    operator fun compareTo(other: Value): Int = value.compareTo(other.value)

    operator fun compareTo(other: BigInteger): Int = value.compareTo(other)

    fun shiftLeft(n: Int): Value = Value(this.type, this.value shl n)

    fun shiftRight(n: Int): Value = Value(this.type, this.value shr n)

    fun signum(): Int = value.signum()

    fun moreThanZero() = this.value > BigInteger.ZERO

    fun moreOrEqualThanZero() = this.value >= BigInteger.ZERO

    fun equalZero() = this.value == BigInteger.ZERO

    fun lessThanZero() = this.value < BigInteger.ZERO

    fun lessOrEqualThanZero() = this.value <= BigInteger.ZERO

    fun notEqualsTo(other : Value) = this != other

    fun equalsTo(other : Value) = this == other

    fun moreThan(other : Value) = this > other

    fun lessThan(other : Value) = this < other

    operator fun unaryMinus(): Value = Value(type, -value)

    /**
     * Returns the value as a 0.12 type string. More digits after the decimal place will be used
     * if necessary, but two will always be present.
     */
    fun toFriendlyString(): String =
            BigDecimal(value, smallestUnitExponent()).setScale(friendlyDigits, RoundingMode.HALF_UP).toString()

    /**
     *
     *
     * Returns the value as a plain string denominated in BTC.
     * The result is unformatted with no trailing zeroes.
     * For instance, a value of 150000 satoshis gives an output string of "0.0015" BTC
     *
     */
    fun toPlainString(): String =
            BigDecimal(value, smallestUnitExponent()).stripTrailingZeros().toString()

    override fun toString(): String = toPlainString() + " " + type.symbol

    /**
     * Returns the value expressed as string
     */
    fun toUnitsString(): String = value.toString()

    override fun equals(other: Any?): Boolean {
        if (other === this)
            return true
        if (other == null || other.javaClass != javaClass)
            return false
        val otherValue = other as Value
        return this.value == otherValue.value && this.type == otherValue.type
    }

    override fun hashCode(): Int = this.value.toInt()

    fun isOfType(otherType: AssetInfo): Boolean = type == otherType

    fun isOfType(otherValue: Value): Boolean = type == otherValue.type

    /**
     * Check if the value is within the [min, max] range
     */
    fun within(min: Value, max: Value): Boolean = compareTo(min) >= 0 && compareTo(max) <= 0

    fun canCompare(other: Value): Boolean = canCompare(this, other)

    fun abs(): Value = Value(type, value.abs())

    companion object {
        @JvmStatic
        fun valueOf(type: AssetInfo, units: Long): Value = Value(type, units.toBigInteger())
        @JvmStatic
        fun valueOf(type: AssetInfo, units: BigInteger): Value = Value(type, units)

        fun valueOf(type: AssetInfo, unitsStr: String): Value =
                valueOf(type, BigInteger(unitsStr))

        @JvmStatic
        fun zeroValue(type: AssetInfo): Value = Value(type, 0.toBigInteger())

        /**
         * Convert an amount expressed in the way humans are used to into units.
         */
        fun valueOf(type: AssetInfo, coins: Int, cents: Int): Value {
            checkArgument(cents < 100)
            checkArgument(cents >= 0)
            checkArgument(coins >= 0)
            return type.oneCoin()* coins.toBigInteger() + (type.oneCoin() / 100.toBigInteger() * cents.toBigInteger())
        }

        /**
         * Parses an amount expressed in the way humans are used to.
         *
         * This takes string in a format understood by [BigDecimal.BigDecimal],
         * for example "0", "1", "0.10", "1.23E3", "1234.5E-5".
         *
         * @throws IllegalArgumentException if you try to specify fractional units, or a value out of
         * range.
         */
        @JvmStatic
        fun parse(type: AssetInfo, str: String): Value = parse(type, BigDecimal(str))

        /**
         * Parses a [BigDecimal] amount expressed in the way humans are used to.
         *
         * @throws IllegalArgumentException if you try to specify fractional units, or a value out of
         * range.
         */
        @JvmStatic
        fun parse(type: AssetInfo, decimal: BigDecimal): Value =
                valueOf(type, decimal.movePointRight(type.unitExponent)
                        .setScale(0, RoundingMode.HALF_DOWN)
                        .toBigIntegerExact())

        @JvmStatic
        fun isNullOrZero(value: Value?): Boolean = value == null || value.isZero()

        fun max(value1: Value, value2: Value): Value = if (value1 >= value2) value1 else value2

        fun min(value1: Value, value2: Value): Value = if (value1 <= value2) value1 else value2

        fun canCompare(amount1: Value?, amount2: Value?): Boolean =
                amount1 != null && amount2 != null && amount1.isOfType(amount2)
    }
}
