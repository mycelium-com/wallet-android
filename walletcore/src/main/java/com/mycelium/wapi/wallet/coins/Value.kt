package com.mycelium.wapi.wallet.coins

import com.google.common.math.LongMath

import java.io.Serializable
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

import com.google.common.base.Preconditions.checkArgument

open class Value(
        /**
         * The type of this value
         */
        @JvmField
        val type: GenericAssetInfo,
        /**
         * The number of units of this monetary value.
         */
        @JvmField
        val value: Long) : Serializable {

    private val friendlyDigits: Int
        get() = type.friendlyDigits

    val valueAsBigDecimal: BigDecimal
        get() = BigDecimal.valueOf(value).movePointLeft(type.unitExponent)

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

    private fun smallestUnitExponent(): Int {
        return type.unitExponent
    }

    operator fun plus(value: Value): Value {
        checkArgument(type == value.type, "Cannot add a different type")
        return Value(this.type, LongMath.checkedAdd(this.value, value.value))
    }

    operator fun plus(value: Long): Value {
        return Value(this.type, LongMath.checkedAdd(this.value, value))
    }

    operator fun minus(value: Value): Value {
        checkArgument(type == value.type, "Cannot subtract a different type")
        return Value(this.type, LongMath.checkedSubtract(this.value, value.value))
    }

    operator fun minus(str: String): Value {
        return Value(this.type, LongMath.checkedSubtract(this.value, type.value(str).value))
    }

    operator fun minus(value: Long): Value {
        return Value(this.type, LongMath.checkedSubtract(this.value, value))
    }

    operator fun times(factor: Long): Value {
        return Value(this.type, LongMath.checkedMultiply(this.value, factor))
    }

    operator fun div(divisor: Long): Value {
        return Value(this.type, this.value / divisor)
    }

    operator fun rem(divisor: Long): Array<Value> {
        return arrayOf(Value(this.type, this.value / divisor), Value(this.type, this.value % divisor))
    }

    operator fun div(divisor: Value): Long {
        checkArgument(type == divisor.type, "Cannot divide with a different type")
        return this.value / divisor.value
    }

    operator fun compareTo(other: Value): Int {
        if (this.value > other.value) {
            return 1
        } else if (this.value < other.value) {
            return -1
        }
        return 0
    }

    fun shiftLeft(n: Int): Value {
        return Value(this.type, this.value shl n)
    }

    fun shiftRight(n: Int): Value {
        return Value(this.type, this.value shr n)
    }

    fun signum(): Int {
        if (this.value == 0L)
            return 0
        return if (this.value < 0) -1 else 1
    }

    operator fun unaryMinus(): Value {
        return Value(this.type, -this.value)
    }

    /**
     * Returns the value as a 0.12 type string. More digits after the decimal place will be used
     * if necessary, but two will always be present.
     */
    fun toFriendlyString(): String {
        return BigDecimal.valueOf(value, smallestUnitExponent()).setScale(friendlyDigits, RoundingMode.HALF_UP).toString()
    }

    /**
     *
     *
     * Returns the value as a plain string denominated in BTC.
     * The result is unformatted with no trailing zeroes.
     * For instance, a value of 150000 satoshis gives an output string of "0.0015" BTC
     *
     */
    fun toPlainString(): String {
        return BigDecimal.valueOf(value, smallestUnitExponent()).stripTrailingZeros().toString()
    }

    override fun toString(): String {
        return toPlainString() + " " + type.symbol
    }

    /**
     * Returns the value expressed as string
     */
    fun toUnitsString(): String {
        return BigInteger.valueOf(value).toString()
    }

    override fun equals(o: Any?): Boolean {
        if (o === this)
            return true
        if (o == null || o.javaClass != javaClass)
            return false
        val other = o as Value?
        return !(this.value != other!!.value || this.type != other.type)
    }

    override fun hashCode(): Int {
        return this.value.toInt()
    }

    fun isOfType(otherType: GenericAssetInfo): Boolean {
        return type == otherType
    }

    fun isOfType(otherValue: Value): Boolean {
        return type == otherValue.type
    }

    /**
     * Check if the value is within the [min, max] range
     */
    fun within(min: Value, max: Value): Boolean {
        return compareTo(min) >= 0 && compareTo(max) <= 0
    }

    fun canCompare(other: Value): Boolean {
        return canCompare(this, other)
    }

    fun abs(): Value {
        return Value(type, Math.abs(value))
    }

    companion object {
        @JvmStatic
        fun valueOf(type: GenericAssetInfo, units: Long): Value {
            return Value(type, units)
        }

        fun valueOf(type: GenericAssetInfo, units: BigInteger): Value {
            return Value(type, units.toLong())
        }

        fun valueOf(type: GenericAssetInfo, unitsStr: String): Value {
            return valueOf(type, BigInteger(unitsStr))
        }

        @JvmStatic
        fun zeroValue(type: GenericAssetInfo): Value {
            return Value(type, 0)
        }

        /**
         * Convert an amount expressed in the way humans are used to into units.
         */
        fun valueOf(type: GenericAssetInfo, coins: Int, cents: Int): Value {
            checkArgument(cents < 100)
            checkArgument(cents >= 0)
            checkArgument(coins >= 0)
            return type.oneCoin()* coins.toLong() + (type.oneCoin() / (100) * cents.toLong())
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
        fun parse(type: GenericAssetInfo, str: String): Value {
            return parse(type, BigDecimal(str))
        }

        /**
         * Parses a [BigDecimal] amount expressed in the way humans are used to.
         *
         * @throws IllegalArgumentException if you try to specify fractional units, or a value out of
         * range.
         */
        @JvmStatic
        fun parse(type: GenericAssetInfo, decimal: BigDecimal): Value {
            return valueOf(type, decimal.movePointRight(type.unitExponent)
                    .setScale(0, RoundingMode.HALF_DOWN)
                    .toBigIntegerExact().toLong())
        }

        @JvmStatic
        fun isNullOrZero(value: Value?): Boolean {
            return value == null || value.isZero()
        }

        fun max(value1: Value, value2: Value): Value {
            return if (value1 >= value2) value1 else value2
        }

        fun min(value1: Value, value2: Value): Value {
            return if (value1 <= value2) value1 else value2
        }

        fun canCompare(amount1: Value?, amount2: Value?): Boolean {
            return amount1 != null && amount2 != null && amount1.isOfType(amount2)
        }
    }
}
